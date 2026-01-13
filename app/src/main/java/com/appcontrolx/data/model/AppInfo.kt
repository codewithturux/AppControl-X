package com.appcontrolx.data.model

import android.graphics.drawable.Drawable

/**
 * Represents the status of an installed application.
 */
enum class AppStatus {
    RUNNING,
    STOPPED,
    FROZEN,
    RESTRICTED,
    NORMAL
}

/**
 * Data class representing information about an installed application.
 * Requirements: 5.1, 5.2, 4.6
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val isRunning: Boolean,
    val isStopped: Boolean,
    val isBackgroundRestricted: Boolean,
    val installedTime: Long,
    val lastUpdateTime: Long,
    val size: Long = 0L,
    /**
     * The running state of the app detected via multi-method detection.
     * Requirements: 4.6
     */
    val runningState: AppRunningState = AppRunningState.UNKNOWN
) {
    /**
     * Whether the app is frozen (disabled)
     */
    val isFrozen: Boolean
        get() = !isEnabled
    
    /**
     * Computed status based on app state flags.
     * Priority: Frozen > Restricted > Running > Stopped > Normal
     */
    val status: AppStatus
        get() = when {
            isFrozen -> AppStatus.FROZEN
            isBackgroundRestricted -> AppStatus.RESTRICTED
            isRunning -> AppStatus.RUNNING
            isStopped -> AppStatus.STOPPED
            else -> AppStatus.NORMAL
        }
}
