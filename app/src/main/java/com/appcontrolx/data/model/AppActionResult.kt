package com.appcontrolx.data.model

/**
 * Status of an action executed on a single app.
 */
enum class ActionStatus {
    SUCCESS,
    FAILED,
    SKIPPED
}

/**
 * Result of executing an action on a single app.
 * Used to track individual app results in batch operations.
 */
data class AppActionResult(
    val packageName: String,
    val appName: String,
    val status: ActionStatus,
    val errorMessage: String? = null
)
