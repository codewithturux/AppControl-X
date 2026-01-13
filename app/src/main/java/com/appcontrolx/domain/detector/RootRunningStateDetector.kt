package com.appcontrolx.domain.detector

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.appcontrolx.data.model.AppRunningState
import com.appcontrolx.domain.executor.CommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Running state detector for Root mode.
 * Uses /proc check as primary method and dumpsys services as secondary.
 * 
 * Requirements: 4.2, 4.3
 */
class RootRunningStateDetector @Inject constructor(
    private val context: Context,
    private val executor: CommandExecutor
) : AppRunningStateDetector {
    
    private val packageManager: PackageManager = context.packageManager
    
    override suspend fun detectRunningState(packageName: String): AppRunningState {
        return withContext(Dispatchers.IO) {
            // First check FLAG_STOPPED
            if (isAppStopped(packageName)) {
                return@withContext AppRunningState.STOPPED
            }
            
            // Primary method: /proc check
            val procResult = checkViaProc(packageName)
            if (procResult == AppRunningState.RUNNING) {
                return@withContext AppRunningState.RUNNING
            }
            
            // Secondary method: dumpsys services
            val dumpsysResult = checkViaDumpsysServices(packageName)
            if (dumpsysResult == AppRunningState.RUNNING) {
                return@withContext AppRunningState.RUNNING
            }
            
            // App was launched before (FLAG_STOPPED = false) but not currently running
            AppRunningState.AWAKENED
        }
    }
    
    override suspend fun detectBatchRunningStates(
        packageNames: List<String>
    ): Map<String, AppRunningState> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, AppRunningState>()
            
            // Get all running packages via /proc in one call
            val runningViaProc = getRunningPackagesViaProc()
            
            // Get all running packages via dumpsys in one call
            val runningViaDumpsys = getRunningPackagesViaDumpsys()
            
            for (packageName in packageNames) {
                val state = when {
                    isAppStopped(packageName) -> AppRunningState.STOPPED
                    runningViaProc.contains(packageName) -> AppRunningState.RUNNING
                    runningViaDumpsys.contains(packageName) -> AppRunningState.RUNNING
                    !isAppStopped(packageName) -> AppRunningState.AWAKENED
                    else -> AppRunningState.UNKNOWN
                }
                results[packageName] = state
            }
            
            results
        }
    }
    
    /**
     * Check if app has FLAG_STOPPED set.
     * Requirements: 4.1
     */
    private fun isAppStopped(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            true // Treat not found as stopped
        }
    }
    
    /**
     * Check if app is running via /proc.
     * Primary method for Root mode.
     * Requirements: 4.2
     */
    private suspend fun checkViaProc(packageName: String): AppRunningState {
        return try {
            val result = executor.execute("cat /proc/*/cmdline 2>/dev/null | tr '\\0' '\\n' | grep -E '^$packageName$'")
            result.fold(
                onSuccess = { output ->
                    if (output.trim().isNotEmpty()) {
                        AppRunningState.RUNNING
                    } else {
                        AppRunningState.UNKNOWN
                    }
                },
                onFailure = { AppRunningState.UNKNOWN }
            )
        } catch (e: Exception) {
            AppRunningState.UNKNOWN
        }
    }
    
    /**
     * Get all running packages via /proc in batch.
     */
    private suspend fun getRunningPackagesViaProc(): Set<String> {
        return try {
            val result = executor.execute("cat /proc/*/cmdline 2>/dev/null | tr '\\0' '\\n' | grep -E '^[a-zA-Z]' | sort -u")
            result.fold(
                onSuccess = { output ->
                    output.lines()
                        .map { it.trim() }
                        .filter { it.contains(".") && !it.contains(" ") }
                        .toSet()
                },
                onFailure = { emptySet() }
            )
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Check if app has running services via dumpsys.
     * Secondary method for Root mode.
     * Requirements: 4.3
     */
    private suspend fun checkViaDumpsysServices(packageName: String): AppRunningState {
        return try {
            val result = executor.execute("dumpsys activity services $packageName")
            result.fold(
                onSuccess = { output ->
                    if (output.contains("ServiceRecord") || output.contains("app=ProcessRecord")) {
                        AppRunningState.RUNNING
                    } else {
                        AppRunningState.UNKNOWN
                    }
                },
                onFailure = { AppRunningState.UNKNOWN }
            )
        } catch (e: Exception) {
            AppRunningState.UNKNOWN
        }
    }
    
    /**
     * Get all running packages via dumpsys in batch.
     */
    private suspend fun getRunningPackagesViaDumpsys(): Set<String> {
        return try {
            val result = executor.execute("dumpsys activity services")
            result.fold(
                onSuccess = { output ->
                    val running = mutableSetOf<String>()
                    // Parse ServiceRecord entries
                    SERVICE_RECORD_PATTERN.findAll(output).forEach { match ->
                        match.groupValues.getOrNull(1)?.let { pkg ->
                            if (isValidPackageName(pkg)) {
                                running.add(pkg)
                            }
                        }
                    }
                    running
                },
                onFailure = { emptySet() }
            )
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    private fun isValidPackageName(name: String): Boolean {
        if (name.isBlank()) return false
        if (!name.contains(".")) return false
        if (name.startsWith("/")) return false
        if (name.contains(" ")) return false
        return PACKAGE_NAME_PATTERN.matches(name)
    }
    
    companion object {
        private val SERVICE_RECORD_PATTERN = Regex("""ServiceRecord\{[^}]+\s+([a-zA-Z][a-zA-Z0-9_.]*)/""")
        private val PACKAGE_NAME_PATTERN = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")
    }
}
