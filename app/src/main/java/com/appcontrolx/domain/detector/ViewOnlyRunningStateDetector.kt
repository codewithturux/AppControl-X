package com.appcontrolx.domain.detector

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.appcontrolx.data.model.AppRunningState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Running state detector for View-Only mode.
 * Uses UsageStatsManager for running detection.
 * 
 * Requirements: 4.5
 */
class ViewOnlyRunningStateDetector @Inject constructor(
    private val context: Context
) : AppRunningStateDetector {
    
    private val packageManager: PackageManager = context.packageManager
    
    private val usageStatsManager: UsageStatsManager? by lazy {
        try {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun detectRunningState(packageName: String): AppRunningState {
        return withContext(Dispatchers.IO) {
            // First check FLAG_STOPPED
            if (isAppStopped(packageName)) {
                return@withContext AppRunningState.STOPPED
            }
            
            // Primary method: UsageStatsManager
            val usageResult = checkViaUsageStats(packageName)
            if (usageResult == AppRunningState.RUNNING) {
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
            
            // Get recently active packages via UsageStats
            val recentlyActivePackages = getRecentlyActivePackages()
            
            for (packageName in packageNames) {
                val state = when {
                    isAppStopped(packageName) -> AppRunningState.STOPPED
                    recentlyActivePackages.contains(packageName) -> AppRunningState.RUNNING
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
     * Check if app was recently active via UsageStatsManager.
     * Requirements: 4.5
     */
    private fun checkViaUsageStats(packageName: String): AppRunningState {
        val manager = usageStatsManager ?: return AppRunningState.UNKNOWN
        
        return try {
            val endTime = System.currentTimeMillis()
            // Check last 5 minutes for recent activity
            val startTime = endTime - RECENT_ACTIVITY_WINDOW_MS
            
            val stats = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            val appStats = stats?.find { it.packageName == packageName }
            
            if (appStats != null && appStats.lastTimeUsed > startTime) {
                AppRunningState.RUNNING
            } else {
                AppRunningState.UNKNOWN
            }
        } catch (e: Exception) {
            AppRunningState.UNKNOWN
        }
    }
    
    /**
     * Get all recently active packages via UsageStats.
     */
    private fun getRecentlyActivePackages(): Set<String> {
        val manager = usageStatsManager ?: return emptySet()
        
        return try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - RECENT_ACTIVITY_WINDOW_MS
            
            val stats = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            stats?.filter { it.lastTimeUsed > startTime }
                ?.map { it.packageName }
                ?.toSet()
                ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    companion object {
        // 5 minutes window for recent activity detection
        private const val RECENT_ACTIVITY_WINDOW_MS = 5 * 60 * 1000L
    }
}
