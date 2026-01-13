package com.appcontrolx.data.model

/**
 * Enum representing the running state of an application.
 * Used for accurate running detection on Android 12+.
 * 
 * Requirements: 4.6
 */
enum class AppRunningState {
    /**
     * App is confirmed running (foreground or background service).
     * Detected via /proc check, dumpsys services, or UsageStatsManager.
     */
    RUNNING,
    
    /**
     * App has been launched but may not be running now.
     * FLAG_STOPPED is false, meaning the app was launched at some point.
     */
    AWAKENED,
    
    /**
     * App has never been launched or was force-stopped.
     * FLAG_STOPPED is true.
     */
    STOPPED,
    
    /**
     * Cannot determine the running state.
     * Used when detection methods fail or timeout.
     */
    UNKNOWN
}
