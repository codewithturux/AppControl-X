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
import com.appcontrolx.data.model.AppRunningState
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.domain.detector.AppRunningStateDetector
import com.appcontrolx.domain.detector.RootRunningStateDetector
import com.appcontrolx.domain.detector.ShizukuRunningStateDetector
import com.appcontrolx.domain.detector.ViewOnlyRunningStateDetector
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
 * - Root: /proc check + dumpsys services (most accurate)
 * - Shizuku: dumpsys services
 * - None: UsageStatsManager + FLAG_STOPPED fallback
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
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
    
    // Running state detector based on execution mode (Requirement 4.1, 4.6)
    private var runningStateDetector: AppRunningStateDetector? = null
    
    // Package change receiver
    private var packageReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false


    
    /**
     * Set the command executor and execution mode.
     * Call this when execution mode changes.
     * Creates appropriate AppRunningStateDetector based on mode.
     * 
     * Requirements: 4.2, 4.3, 4.4, 4.5
     * 
     * @param executor The command executor to use (null for View-Only mode)
     * @param mode The current execution mode
     */
    fun setExecutor(executor: CommandExecutor?, mode: ExecutionMode) {
        this.executor = executor
        this.executionMode = mode
        
        // Create appropriate running state detector based on mode (Requirements 4.2-4.5)
        runningStateDetector = when (mode) {
            ExecutionMode.Root -> {
                executor?.let { RootRunningStateDetector(context, it) }
            }
            ExecutionMode.Shizuku -> {
                executor?.let { ShizukuRunningStateDetector(context, it) }
            }
            ExecutionMode.None -> {
                ViewOnlyRunningStateDetector(context)
            }
        }
        
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
     * Integrates AppRunningStateDetector for accurate running state detection.
     * 
     * Requirements: 4.1, 4.6
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
     * Uses AppRunningStateDetector for accurate running state detection.
     * 
     * Requirements: 4.1, 4.6
     */
    private suspend fun fetchAllApps(): List<AppInfo> {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val runningPackages = getRunningPackages()
        
        // Get package names for batch running state detection
        val packageNames = packages.mapNotNull { it.packageName }
        
        // Detect running states in batch using AppRunningStateDetector (Requirement 4.6)
        val runningStates = runningStateDetector?.detectBatchRunningStates(packageNames)
            ?: emptyMap()
        
        return packages.mapNotNull { pkg ->
            try {
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                
                // Get running state from detector (Requirement 4.6)
                val runningState = runningStates[pkg.packageName] ?: AppRunningState.UNKNOWN
                
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
                    lastUpdateTime = pkg.lastUpdateTime,
                    size = getAppSize(appInfo),
                    uid = appInfo.uid,
                    runningState = runningState
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }
    
    /**
     * Get the size of an application in bytes.
     * Uses the sourceDir to get the APK file size.
     * 
     * @param appInfo The ApplicationInfo of the app
     * @return Size in bytes, or 0 if unable to determine
     */
    private fun getAppSize(appInfo: ApplicationInfo): Long {
        return try {
            val sourceDir = appInfo.sourceDir
            if (sourceDir != null) {
                java.io.File(sourceDir).length()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }


    
    /**
     * Get the set of currently running packages.
     * Uses different methods based on execution mode.
     * 
     * @return Set of package names that are currently running
     */
    private suspend fun getRunningPackages(): Set<String> {
        return when (executionMode) {
            ExecutionMode.Root, ExecutionMode.Shizuku -> {
                getRunningPackagesViaDumpsys()
            }
            ExecutionMode.None -> {
                getRunningPackagesViaActivityManager()
            }
        }
    }
    
    /**
     * Get running packages using dumpsys activity processes.
     * Most accurate method, requires Root or Shizuku.
     * 
     * @return Set of running package names
     */
    internal suspend fun getRunningPackagesViaDumpsys(): Set<String> {
        val running = mutableSetOf<String>()
        val exec = executor ?: return getRunningPackagesViaActivityManager()
        
        try {
            val result = exec.execute("dumpsys activity processes")
            
            result.onSuccess { output ->
                output.lines().forEach { line ->
                    if (line.contains("app=")) {
                        val match = APP_PATTERN.find(line)
                        match?.groupValues?.getOrNull(1)?.let { pkg ->
                            if (isValidPackageName(pkg)) {
                                running.add(pkg)
                            }
                        }
                    }
                    
                    if (line.contains("ProcessRecord")) {
                        val match = PROCESS_RECORD_PATTERN.find(line)
                        match?.groupValues?.getOrNull(1)?.let { pkg ->
                            if (isValidPackageName(pkg)) {
                                running.add(pkg)
                            }
                        }
                    }
                    
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
            return getRunningPackagesViaActivityManager()
        }
        
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
     * @return Set of running package names
     */
    internal fun getRunningPackagesViaActivityManager(): Set<String> {
        val running = mutableSetOf<String>()
        
        try {
            activityManager.runningAppProcesses?.forEach { process ->
                process.pkgList?.forEach { pkg ->
                    running.add(pkg)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
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
     * @param packageName The package to check
     * @param uid The UID of the package
     * @return true if background is restricted, false otherwise
     */
    internal fun getBackgroundRestrictionStatus(packageName: String, uid: Int): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
        private val APP_PATTERN = Regex("""app=ProcessRecord\{[^}]+\s+([a-zA-Z][a-zA-Z0-9_.]*)/""")
        private val PROCESS_RECORD_PATTERN = Regex("""ProcessRecord\{[^}]+\s+\d+:([a-zA-Z][a-zA-Z0-9_.]*)/""")
        private val PROC_PATTERN = Regex("""/([a-zA-Z][a-zA-Z0-9_.]+)\s*\(""")
        private val PACKAGE_NAME_PATTERN = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")
    }
}
