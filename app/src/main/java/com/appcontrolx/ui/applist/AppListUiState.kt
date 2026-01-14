package com.appcontrolx.ui.applist

import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.AppListFilter
import com.appcontrolx.data.model.BatchAction
import com.appcontrolx.data.model.BatchExecutionResult
import com.appcontrolx.data.model.ExecutionMode

/**
 * UI state for AppListFragment.
 * Requirements: 3.6, 3.7, 3.8
 */
data class AppListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val appListFilter: AppListFilter = AppListFilter(),
    val selectedCount: Int = 0,
    val executionMode: ExecutionMode = ExecutionMode.None,
    val error: String? = null,
    // Batch execution state - Requirements: 2.1, 2.2, 2.3, 2.4
    val batchExecutionState: BatchExecutionState = BatchExecutionState.Idle
)

/**
 * State of batch execution operation.
 * Requirements: 2.1, 2.2, 2.3, 2.4
 */
sealed class BatchExecutionState {
    /** No batch operation in progress */
    data object Idle : BatchExecutionState()
    
    /** Batch operation is executing */
    data class Executing(
        val action: BatchAction,
        val currentIndex: Int,
        val totalCount: Int,
        val currentPackageName: String
    ) : BatchExecutionState()
    
    /** Batch operation completed */
    data class Completed(
        val result: BatchExecutionResult
    ) : BatchExecutionState()
    
    /** Batch operation failed with error */
    data class Error(
        val message: String
    ) : BatchExecutionState()
}
