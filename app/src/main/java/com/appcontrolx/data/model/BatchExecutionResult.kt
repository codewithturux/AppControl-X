package com.appcontrolx.data.model

/**
 * Result of executing a batch action on multiple apps.
 * Contains the action performed, individual results, and summary counts.
 */
data class BatchExecutionResult(
    val action: BatchAction,
    val results: List<AppActionResult>,
    val successCount: Int = results.count { it.status == ActionStatus.SUCCESS },
    val failureCount: Int = results.count { it.status == ActionStatus.FAILED },
    val skippedCount: Int = results.count { it.status == ActionStatus.SKIPPED }
) {
    /**
     * Total number of apps processed
     */
    val totalCount: Int
        get() = results.size

    /**
     * Whether all apps were processed successfully (no failures)
     */
    val isFullSuccess: Boolean
        get() = failureCount == 0 && successCount > 0

    /**
     * Whether any apps were skipped due to safety validation
     */
    val hasSkippedApps: Boolean
        get() = skippedCount > 0
}
