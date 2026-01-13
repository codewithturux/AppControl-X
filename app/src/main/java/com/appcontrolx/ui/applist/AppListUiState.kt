package com.appcontrolx.ui.applist

import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.AppListFilter
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
    val error: String? = null
)
