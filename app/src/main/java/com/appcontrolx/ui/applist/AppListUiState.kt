package com.appcontrolx.ui.applist

import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.ExecutionMode

/**
 * Status filter options for app list.
 * Requirement: 8.4
 */
enum class StatusFilter {
    ALL,
    RUNNING,
    STOPPED,
    FROZEN,
    RESTRICTED
}

/**
 * UI state for AppListFragment.
 */
data class AppListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val showSystemApps: Boolean = false,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val selectedCount: Int = 0,
    val executionMode: ExecutionMode = ExecutionMode.None,
    val error: String? = null
)
