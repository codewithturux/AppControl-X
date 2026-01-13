package com.appcontrolx.data.model

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
 */
data class AppListFilter(
    val filterType: FilterType = FilterType.ALL,
    val sortType: SortType = SortType.NAME_ASC
)
