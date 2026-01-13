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
 * Running state detector for Shizuku mode.
 * Uses dumpsys services for running detection.
 * 
 * Requirements: 4.4
 */
class ShizukuRunningStateDetector @Inject constructor(
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
            
            // Primary method: dumpsys services
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
            
            // Get all running packages via dumpsys in one call
            val runningViaDumpsys = getRunningPackagesViaDumpsys()
            
            for (packageName in packageNames) {
                val state = when {
                    isAppStopped(packageName) -> AppRunningState.STOPPED
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
     * Check if app has running services via dumpsys.
     * Primary method for Shizuku mode.
     * Requirements: 4.4
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
