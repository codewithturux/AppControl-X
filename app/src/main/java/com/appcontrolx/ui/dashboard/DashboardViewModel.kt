package com.appcontrolx.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.data.model.*
import com.appcontrolx.domain.executor.PermissionBridge
import com.appcontrolx.domain.monitor.SystemMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * 
 * Collects real-time system information from SystemMonitor and exposes it to the UI.
 * Also manages execution mode state and feature card navigation.
 * 
 * Requirements: 0.1.9 - Real-time dashboard updates
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val systemMonitor: SystemMonitor,
    private val permissionBridge: PermissionBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Navigation events channel for one-time navigation events
    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadInitialData()
        startMonitoring()
        observeExecutionMode()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Load execution mode
                val mode = permissionBridge.mode
                _uiState.update { it.copy(executionMode = mode) }
                
                // Load static data (display, GPU, device info, app counts)
                loadStaticData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * Observe execution mode changes from PermissionBridge.
     * Updates UI state when mode changes (e.g., mode loss detection).
     */
    private fun observeExecutionMode() {
        viewModelScope.launch {
            permissionBridge.currentMode.collect { mode ->
                _uiState.update { it.copy(executionMode = mode) }
            }
        }
    }

    private suspend fun loadStaticData() {
        // Display info (doesn't change frequently)
        val displayInfo = systemMonitor.getDisplayInfo()
        _uiState.update { it.copy(displayInfo = displayInfo) }
        
        // GPU info (requires root, static)
        val gpuInfo = systemMonitor.getGpuInfo()
        _uiState.update { it.copy(gpuInfo = gpuInfo) }
        
        // Device info
        val deviceInfo = systemMonitor.getDeviceInfo()
        _uiState.update { it.copy(deviceInfo = deviceInfo) }
        
        // App counts
        val appCounts = systemMonitor.getAppCounts()
        _uiState.update { it.copy(appCounts = appCounts) }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            systemMonitor.startMonitoring(intervalMs = 2000).collect { snapshot ->
                _uiState.update { it.copy(systemSnapshot = snapshot) }
                
                // Also update device info for uptime/deep sleep
                val deviceInfo = systemMonitor.getDeviceInfo()
                _uiState.update { it.copy(deviceInfo = deviceInfo) }
            }
        }
    }

    /**
     * Refresh all data manually.
     * Called when user pulls to refresh or returns to dashboard.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Refresh execution mode
                val mode = permissionBridge.mode
                _uiState.update { it.copy(executionMode = mode) }
                
                loadStaticData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * Clear any displayed error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // ==================== Feature Card Navigation ====================
    
    /**
     * Handle feature card click events.
     * Emits navigation events to be consumed by the Fragment.
     * 
     * @param feature The feature card that was clicked
     */
    fun onFeatureCardClicked(feature: FeatureDestination) {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToFeature(feature))
        }
    }
    
    /**
     * Navigate to settings screen.
     */
    fun onSettingsClicked() {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToFeature(FeatureDestination.SETTINGS))
        }
    }

    override fun onCleared() {
        super.onCleared()
        systemMonitor.stopMonitoring()
    }
}

/**
 * UI state for the Dashboard screen.
 */
data class DashboardUiState(
    val executionMode: ExecutionMode = ExecutionMode.None,
    val systemSnapshot: SystemSnapshot? = null,
    val displayInfo: DisplayInfo? = null,
    val gpuInfo: GpuInfo? = null,
    val deviceInfo: DeviceInfo? = null,
    val appCounts: AppCounts? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation events emitted by the ViewModel.
 * These are one-time events consumed by the Fragment for navigation.
 */
sealed class NavigationEvent {
    data class NavigateToFeature(val destination: FeatureDestination) : NavigationEvent()
}

/**
 * Feature destinations available from the Dashboard.
 * Maps to navigation graph destinations.
 */
enum class FeatureDestination {
    APP_MANAGER,
    BATTERY_MANAGER,
    ACTIVITY_LAUNCHER,
    TOOLS,
    ACTION_LOGS,
    SETTINGS
}
