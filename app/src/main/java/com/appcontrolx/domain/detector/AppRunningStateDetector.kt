package com.appcontrolx.domain.detector

import com.appcontrolx.data.model.AppRunningState

/**
 * Interface for detecting the running state of applications.
 * Implementations use different methods based on execution mode.
 * 
 * Requirements: 4.1, 4.6
 */
interface AppRunningStateDetector {
    
    /**
     * Detect the running state of a single application.
     * 
     * @param packageName The package name of the app to check
     * @return The detected running state
     */
    suspend fun detectRunningState(packageName: String): AppRunningState
    
    /**
     * Detect the running states of multiple applications in batch.
     * More efficient than calling detectRunningState() for each app.
     * 
     * @param packageNames List of package names to check
     * @return Map of package name to detected running state
     */
    suspend fun detectBatchRunningStates(packageNames: List<String>): Map<String, AppRunningState>
}
