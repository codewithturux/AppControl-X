package com.appcontrolx.ui.applist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.AppListFilter
import com.appcontrolx.data.model.AppStatus
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.data.model.FilterType
import com.appcontrolx.data.model.SortType
import com.appcontrolx.domain.executor.CommandExecutor
import com.appcontrolx.domain.executor.PermissionBridge
import com.appcontrolx.domain.executor.RootExecutor
import com.appcontrolx.domain.executor.ShizukuExecutor
import com.appcontrolx.domain.scanner.AppScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * ViewModel for AppListFragment.
 * Handles app loading, filtering, sorting, and selection state.
 * 
 * Requirements: 3.6, 3.7, 3.8
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appScanner: AppScanner,
    private val permissionBridge: PermissionBridge,
    private val rootExecutor: RootExecutor,
    private val shizukuExecutor: ShizukuExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    // Cache for apps
    private var cachedApps: List<AppInfo>? = null

    // Package change receiver
    private var packageReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false
    
    // Current executor based on mode
    private var executor: CommandExecutor? = null

    companion object {
        private const val LOAD_TIMEOUT_MS = 30000L
    }

    init {
        // Detect execution mode in coroutine
        viewModelScope.launch {
            val mode = permissionBridge.detectMode()
            _uiState.update { it.copy(executionMode = mode) }
            
            // Select executor based on mode
            executor = when (mode) {
                ExecutionMode.Root -> rootExecutor
                ExecutionMode.Shizuku -> shizukuExecutor
                ExecutionMode.None -> null
            }
            
            // Setup scanner with executor
            appScanner.setExecutor(executor, mode)
        }
    }

    /**
     * Load apps from scanner.
     * Uses cache if available.
     */
    fun loadApps(forceRefresh: Boolean = false) {
        // Check cache first
        if (!forceRefresh && cachedApps != null) {
            _uiState.update { it.copy(allApps = cachedApps!!) }
            applyFiltersAndSort()
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val apps = withTimeout(LOAD_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        appScanner.scanAllApps()
                    }
                }
                
                // Cache results
                cachedApps = apps
                
                _uiState.update { it.copy(allApps = apps, isLoading = false) }
                applyFiltersAndSort()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Unknown error"
                    ) 
                }
            }
        }
    }

    /**
     * Refresh apps (force reload).
     */
    fun refreshApps() {
        _uiState.update { it.copy(isRefreshing = true) }
        clearCache()
        
        viewModelScope.launch {
            try {
                val apps = withTimeout(LOAD_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        appScanner.clearCache()
                        appScanner.scanAllApps()
                    }
                }
                
                // Cache results
                cachedApps = apps
                
                _uiState.update { it.copy(allApps = apps, isRefreshing = false, error = null) }
                applyFiltersAndSort()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = e.message ?: "Unknown error"
                    ) 
                }
            }
        }
    }

    /**
     * Handle search query change.
     * Requirement: 3.1
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    /**
     * Handle filter/sort change from FilterSortBottomSheet.
     * Requirements: 3.6, 3.7, 3.8
     */
    fun onFilterSortChanged(filter: AppListFilter) {
        _uiState.update { it.copy(appListFilter = filter) }
        applyFiltersAndSort()
    }

    /**
     * Handle selection count change.
     */
    fun onSelectionChanged(count: Int) {
        _uiState.update { it.copy(selectedCount = count) }
    }

    /**
     * Filter apps based on the current filter type.
     * Requirement: 3.6
     * 
     * @param apps List of apps to filter
     * @param filterType The filter type to apply
     * @return Filtered list of apps
     */
    fun filterApps(apps: List<AppInfo>, filterType: FilterType): List<AppInfo> {
        return when (filterType) {
            FilterType.ALL -> apps
            FilterType.RUNNING -> apps.filter { it.status == AppStatus.RUNNING }
            FilterType.FROZEN -> apps.filter { it.status == AppStatus.FROZEN }
            FilterType.RESTRICTED -> apps.filter { it.status == AppStatus.RESTRICTED }
        }
    }

    /**
     * Sort apps based on the current sort type.
     * Requirement: 3.7
     * 
     * @param apps List of apps to sort
     * @param sortType The sort type to apply
     * @return Sorted list of apps
     */
    fun sortApps(apps: List<AppInfo>, sortType: SortType): List<AppInfo> {
        return when (sortType) {
            SortType.NAME_ASC -> apps.sortedBy { it.appName.lowercase() }
            SortType.NAME_DESC -> apps.sortedByDescending { it.appName.lowercase() }
            SortType.SIZE_DESC -> apps.sortedByDescending { it.size }
            SortType.UPDATED_DESC -> apps.sortedByDescending { it.lastUpdateTime }
        }
    }

    /**
     * Apply search, filter, and sort to the app list.
     * Requirements: 3.6, 3.7
     */
    private fun applyFiltersAndSort() {
        val currentState = _uiState.value
        var result = currentState.allApps
        
        // Apply filter
        result = filterApps(result, currentState.appListFilter.filterType)
        
        // Apply search filter
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            result = result.filter { app ->
                app.appName.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
            }
        }
        
        // Apply sort
        result = sortApps(result, currentState.appListFilter.sortType)
        
        _uiState.update { it.copy(filteredApps = result) }
    }

    /**
     * Clear cached apps.
     */
    private fun clearCache() {
        cachedApps = null
    }

    /**
     * Register package change broadcast receiver.
     */
    fun registerPackageReceiver() {
        if (isReceiverRegistered) return
        
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_CHANGED -> {
                        clearCache()
                        appScanner.invalidateCache()
                        loadApps(forceRefresh = true)
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
        isReceiverRegistered = true
    }

    /**
     * Unregister package change broadcast receiver.
     */
    fun unregisterPackageReceiver() {
        if (!isReceiverRegistered) return
        
        try {
            packageReceiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            // Ignore if not registered
        }
        packageReceiver = null
        isReceiverRegistered = false
    }

    override fun onCleared() {
        super.onCleared()
        unregisterPackageReceiver()
    }
}
