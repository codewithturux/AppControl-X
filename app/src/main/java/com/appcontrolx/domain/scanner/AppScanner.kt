package com.appcontrolx.domain.scanner

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.domain.executor.CommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans and retrieves information about installed applications.
 * 
 * Uses different detection methods based on execution mode:
 * - Root: dumpsys activity processes (most accurate)
 * - Shizuku: Shizuku API to execute dumpsys
 * - None: ActivityManager.getRunningAppProcesses() (fallback, less accurate)
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7
 */
@Singleton
class AppScanner @Inject constructor(
    private val context: Context
) {
    
    private val packageManager: PackageManager = context.packageManager
    
    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    
    private val appOpsManager: AppOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }
    
    // Cache management
    private var cachedApps: List<AppInfo>? = null
    private var cacheTimestamp: Long = 0
    private val cacheMutex = Mutex()
    private var isCacheInvalidated = false
    
    // Current executor and mode (set via setExecutor)
    private var executor: CommandExecutor? = null
    private var executionMode: ExecutionMode = ExecutionMode.None
    
    // Package change receiver
    private var packageReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    
    /**
     * Set the command executor and execution mode.
     * Call this when execution mode changes.
     * 
     * @param executor The command executor to use (null for View-Only mode)
     * @param mode The current execution mode
     */
    fun setExecutor(executor: CommandExecutor?, mode: ExecutionMode) {
        this.executor = executor
        this.executionMode = mode
        // Invalidate cache when mode changes as running status detection may differ
        invalidateCache()
    }
    
    /**
     * Register the package change broadcast receiver.
     * Call this when the app starts or resumes.
     */
    fun registerPackageReceiver() {
        if (isReceiverRegistered) return
        
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Invalidate cache when packages change
                invalidateCache()
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
        isReceiverRegistered = true
    }
    
    /**
     * Unregister the package change broadcast receiver.
     * Call this when the app is destroyed.
     */
    fun unregisterPackageReceiver() {
        if (!isReceiverRegistered) return
        
        try {
            packageReceiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            // Ignore if not registered
        }
        packageReceiver = null
        isReceiverRegistered = false
    }
    
    /**
     * Invalidate the app cache.
     * Next scan will fetch fresh data.
     */
    fun invalidateCache() {
        isCacheInvalidated = true
    }
    
    /**
     * Clear the cache completely.
     */
    suspend fun clearCache() {
        cacheMutex.withLock {
            cachedApps = null
            cacheTimestamp = 0
            isCacheInvalidated = false
        }
    }

    
    /**
     * Scan all installed applications.
     * Uses cache if available and not invalidated.
     * 
     * Requirements: 2.1, 2.7
     * 
     * @return List of all installed apps sorted by name
     */
    suspend fun scanAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            // Return cached data if valid
            if (!isCacheInvalidated && cachedApps != null) {
                return@withContext cachedApps!!
            }
            
            // Fetch fresh data
            val apps = fetchAllApps()
            cachedApps = apps
            cacheTimestamp = System.currentTimeMillis()
            isCacheInvalidated = false
            
            apps
        }
    }
    
    /**
     * Scan only user-installed applications.
     * 
     * @return List of user apps sorted by name
     */
    suspend fun scanUserApps(): List<AppInfo> {
        return scanAllApps().filter { !it.isSystemApp }
    }
    
    /**
     * Scan only system applications.
     * 
     * @return List of system apps sorted by name
     */
    suspend fun scanSystemApps(): List<AppInfo> {
        return scanAllApps().filter { it.isSystemApp }
    }
    
    /**
     * Fetch all apps from PackageManager with running status.
     * Internal method that always fetches fresh data.
     */
    private suspend fun fetchAllApps(): List<AppInfo> {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val runningPackages = getRunningPackages()
        
        return packages.mapNotNull { pkg ->
            try {
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                
                AppInfo(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    icon = try { appInfo.loadIcon(packageManager) } catch (e: Exception) { null },
                    versionName = pkg.versionName ?: "",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkg.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode.toLong()
                    },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = appInfo.enabled,
                    isRunning = runningPackages.contains(pkg.packageName),
                    isStopped = (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0,
                    isBackgroundRestricted = getBackgroundRestrictionStatus(pkg.packageName, appInfo.uid),
                    installedTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    
    /**
     * Get the set of currently running packages.
     * Uses different methods based on execution mode.
     * 
     * Requirements: 2.2, 2.3, 2.4
     * 
     * @return Set of package names that are currently running
     */
    private suspend fun getRunningPackages(): Set<String> {
        return when (executionMode) {
            ExecutionMode.Root, ExecutionMode.Shizuku -> {
                // Use dumpsys for accurate detection (Requirements 2.2, 2.3)
                getRunningPackagesViaDumpsys()
            }
            ExecutionMode.None -> {
                // Fallback to ActivityManager (Requirement 2.4)
                getRunningPackagesViaActivityManager()
            }
        }
    }
    
    /**
     * Get running packages using dumpsys activity processes.
     * Most accurate method, requires Root or Shizuku.
     * 
     * Requirements: 2.2, 2.3
     * 
     * @return Set of running package names
     */
    internal suspend fun getRunningPackagesViaDumpsys(): Set<String> {
        val running = mutableSetOf<String>()
        val exec = executor ?: return getRunningPackagesViaActivityManager()
        
        try {
            // Primary method: dumpsys activity processes
            val result = exec.execute("dumpsys activity processes")
            
            result.onSuccess { output ->
                // Parse output for package names
                // Look for lines like: "app=ProcessRecord{...packageName/uid}"
                // or "packageName/uid" patterns
                output.lines().forEach { line ->
                    // Pattern 1: app=ProcessRecord{hash packageName/uid}
                    if (line.contains("app=")) {
                        val match = APP_PATTERN.find(line)
                        match?.groupValues?.getOrNull(1)?.let { pkg ->
                            if (isValidPackageName(pkg)) {
                                running.add(pkg)
                            }
                        }
                    }
                    
                    // Pattern 2: ProcessRecord{hash uid:packageName/uid}
                    if (line.contains("ProcessRecord")) {
                        val match = PROCESS_RECORD_PATTERN.find(line)
                        match?.groupValues?.getOrNull(1)?.let { pkg ->
                            if (isValidPackageName(pkg)) {
                                running.add(pkg)
                            }
                        }
                    }
                    
                    // Pattern 3: Proc # N: adj=... /packageName (pid)
                    if (line.contains("Proc #")) {
                        val match = PROC_PATTERN.find(line)
                        match?.groupValues?.getOrNull(1)?.let { pkg ->
                            if (isValidPackageName(pkg)) {
                                running.add(pkg)
                            }
                        }
                    }
                }
            }
            
            // If primary method didn't find anything, try alternative
            if (running.isEmpty()) {
                val altResult = exec.execute("ps -A -o NAME")
                altResult.onSuccess { output ->
                    output.lines().forEach { line ->
                        val pkg = line.trim()
                        if (isValidPackageName(pkg)) {
                            running.add(pkg)
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            // Fall back to ActivityManager on error
            return getRunningPackagesViaActivityManager()
        }
        
        // If still empty, use ActivityManager as fallback
        if (running.isEmpty()) {
            return getRunningPackagesViaActivityManager()
        }
        
        return running
    }

    
    /**
     * Get running packages using ActivityManager.
     * Fallback method for View-Only mode or when dumpsys fails.
     * Less accurate on Android 10+ due to privacy restrictions.
     * 
     * Requirement: 2.4
     * 
     * @return Set of running package names
     */
    internal fun getRunningPackagesViaActivityManager(): Set<String> {
        val running = mutableSetOf<String>()
        
        // Method 1: runningAppProcesses (limited on Android 10+)
        try {
            activityManager.runningAppProcesses?.forEach { process ->
                process.pkgList?.forEach { pkg ->
                    running.add(pkg)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        // Method 2: getRunningServices (deprecated but still works)
        try {
            @Suppress("DEPRECATION")
            activityManager.getRunningServices(Int.MAX_VALUE)?.forEach { service ->
                running.add(service.service.packageName)
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return running
    }
    
    /**
     * Check if an app has background restrictions enabled.
     * Uses AppOpsManager to query RUN_IN_BACKGROUND operation.
     * 
     * Requirement: 2.5
     * 
     * @param packageName The package to check
     * @param uid The UID of the package
     * @return true if background is restricted, false otherwise
     */
    internal fun getBackgroundRestrictionStatus(packageName: String, uid: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // RUN_IN_BACKGROUND operation
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    "android:run_in_background",
                    uid,
                    packageName
                )
                mode == AppOpsManager.MODE_IGNORED
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate if a string looks like a valid package name.
     */
    private fun isValidPackageName(name: String): Boolean {
        if (name.isBlank()) return false
        if (!name.contains(".")) return false
        if (name.startsWith("/")) return false
        if (name.contains(" ")) return false
        return PACKAGE_NAME_PATTERN.matches(name)
    }
    
    companion object {
        // Regex patterns for parsing dumpsys output
        private val APP_PATTERN = Regex("""app=ProcessRecord\{[^}]+\s+([a-zA-Z][a-zA-Z0-9_.]*)/""")
        private val PROCESS_RECORD_PATTERN = Regex("""ProcessRecord\{[^}]+\s+\d+:([a-zA-Z][a-zA-Z0-9_.]*)/""")
        private val PROC_PATTERN = Regex("""/([a-zA-Z][a-zA-Z0-9_.]+)\s*\(""")
        private val PACKAGE_NAME_PATTERN = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")
    }
}
