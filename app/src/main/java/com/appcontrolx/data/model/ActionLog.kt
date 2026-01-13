package com.appcontrolx.data.model

import java.util.UUID

/**
 * Types of actions that can be performed on apps.
 */
enum class AppAction {
    FREEZE,
    UNFREEZE,
    FORCE_STOP,
    UNINSTALL,
    CLEAR_CACHE,
    CLEAR_DATA,
    RESTRICT_BACKGROUND,
    ALLOW_BACKGROUND
}

/**
 * Record of an action performed on one or more apps.
 */
data class ActionLog(
    val id: String = UUID.randomUUID().toString(),
    val action: AppAction,
    val packages: List<String>,
    val success: Boolean,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * State of a single app for rollback purposes.
 */
data class AppState(
    val packageName: String,
    val isEnabled: Boolean,
    val runInBackground: String,
    val wakeLock: String
)

/**
 * Snapshot of app states before a batch action.
 * Used for rollback functionality.
 */
data class StateSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val states: List<AppState>,
    val createdAt: Long = System.currentTimeMillis()
)
