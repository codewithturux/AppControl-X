package com.appcontrolx.data.model

/**
 * App type filter options (User/System/All).
 * Default is USER to show only user-installed apps.
 */
enum class AppTypeFilter(val displayName: String) {
    USER("User Apps"),
    SYSTEM("System Apps"),
    ALL("All Apps")
}

/**
 * Filter type options for app list filtering.
 * Requirements: 3.4
 */
enum class FilterType(val displayName: String) {
    ALL("All"),
    RUNNING("Running"),
    FROZEN("Frozen"),
    RESTRICTED("Restricted")
}

/**
 * Sort type options for app list sorting.
 * Requirements: 3.5
 */
enum class SortType(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_DESC("Size"),
    UPDATED_DESC("Last updated")
}

/**
 * Data class representing the current filter and sort configuration for the app list.
 * Requirements: 3.4, 3.5
 * 
 * Default appTypeFilter is USER to show only user apps initially.
 */
data class AppListFilter(
    val appTypeFilter: AppTypeFilter = AppTypeFilter.USER,
    val filterType: FilterType = FilterType.ALL,
    val sortType: SortType = SortType.NAME_ASC
)
