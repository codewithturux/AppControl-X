package com.appcontrolx.ui.settings

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.domain.executor.PermissionBridge
import com.appcontrolx.domain.manager.ActionLogger
import com.appcontrolx.domain.manager.DisplayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 * 
 * Manages:
 * - Theme selection (Light/Dark/System)
 * - Dynamic colors toggle (Android 12+)
 * - Execution mode display and change
 * - Display refresh rate settings (when Root/Shizuku available)
 * - Safety settings
 * - Rollback/snapshot settings
 * - About section
 * 
 * Requirements: 8.7, 8.8, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionBridge: PermissionBridge,
    private val displayManager: DisplayManager,
    private val actionLogger: ActionLogger
) : ViewModel() {
    
    companion object {
        private const val PREFS_NAME = "appcontrolx_prefs"
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        private const val KEY_CONFIRM_ACTIONS = "confirm_actions"
        private const val KEY_PROTECT_SYSTEM = "protect_system"
        private const val KEY_AUTO_SNAPSHOT = "auto_snapshot"
    }
    
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load all settings from preferences and system.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // Load theme
            val theme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            
            // Load dynamic colors setting (default true on Android 12+)
            val dynamicColors = prefs.getBoolean(KEY_DYNAMIC_COLORS, true)
            val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            
            // Load execution mode
            val mode = permissionBridge.mode
            
            // Load safety settings
            val confirmActions = prefs.getBoolean(KEY_CONFIRM_ACTIONS, true)
            val protectSystem = prefs.getBoolean(KEY_PROTECT_SYSTEM, true)
            val autoSnapshot = prefs.getBoolean(KEY_AUTO_SNAPSHOT, true)
            
            // Load counts
            val logCount = actionLogger.getActionHistory().size
            val snapshotCount = getSnapshotCount()
            
            // Load display settings if mode allows
            val canControlDisplay = mode.canExecuteActions
            var minRefreshRate = 60f
            var maxRefreshRate = 60f
            var supportedRates = listOf(60f)
            
            if (canControlDisplay) {
                supportedRates = displayManager.getSupportedRefreshRates()
                minRefreshRate = displayManager.getMinRefreshRate().getOrNull() ?: supportedRates.minOrNull() ?: 60f
                maxRefreshRate = displayManager.getMaxRefreshRate().getOrNull() ?: supportedRates.maxOrNull() ?: 60f
            }
            
            // Get app version
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
            
            _uiState.update {
                it.copy(
                    theme = theme,
                    dynamicColors = dynamicColors,
                    supportsDynamicColors = supportsDynamicColors,
                    executionMode = mode,
                    confirmActions = confirmActions,
                    protectSystem = protectSystem,
                    autoSnapshot = autoSnapshot,
                    logCount = logCount,
                    snapshotCount = snapshotCount,
                    canControlDisplay = canControlDisplay,
                    minRefreshRate = minRefreshRate,
                    maxRefreshRate = maxRefreshRate,
                    supportedRefreshRates = supportedRates,
                    appVersion = appVersion
                )
            }
        }
    }
    
    /**
     * Set the app theme.
     * Requirement 8.8: Light/Dark theme support
     */
    fun setTheme(theme: Int) {
        prefs.edit().putInt(KEY_THEME, theme).apply()
        AppCompatDelegate.setDefaultNightMode(theme)
        _uiState.update { it.copy(theme = theme) }
    }
    
    /**
     * Set dynamic colors preference.
     * Requirement 8.7: Dynamic colors support (Android 12+)
     * 
     * Note: Changing this requires app restart to take effect.
     */
    fun setDynamicColors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply()
        _uiState.update { it.copy(dynamicColors = enabled) }
        // Emit restart event since dynamic colors require app restart
        viewModelScope.launch {
            _events.emit(SettingsEvent.RestartRequired)
        }
    }
    
    /**
     * Set confirm actions preference.
     */
    fun setConfirmActions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_ACTIONS, enabled).apply()
        _uiState.update { it.copy(confirmActions = enabled) }
    }
    
    /**
     * Set protect system apps preference.
     */
    fun setProtectSystem(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROTECT_SYSTEM, enabled).apply()
        _uiState.update { it.copy(protectSystem = enabled) }
    }
    
    /**
     * Set auto snapshot preference.
     */
    fun setAutoSnapshot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SNAPSHOT, enabled).apply()
        _uiState.update { it.copy(autoSnapshot = enabled) }
    }
    
    /**
     * Set minimum refresh rate.
     * Requirement 10.6
     */
    fun setMinRefreshRate(hz: Float) {
        viewModelScope.launch {
            val result = displayManager.setMinRefreshRate(hz)
            if (result.isSuccess) {
                _uiState.update { it.copy(minRefreshRate = hz) }
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Failed to set refresh rate"))
            }
        }
    }
    
    /**
     * Set maximum refresh rate.
     * Requirement 10.7
     */
    fun setMaxRefreshRate(hz: Float) {
        viewModelScope.launch {
            val result = displayManager.setMaxRefreshRate(hz)
            if (result.isSuccess) {
                _uiState.update { it.copy(maxRefreshRate = hz) }
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Failed to set refresh rate"))
            }
        }
    }
    
    /**
     * Reset refresh rate to default.
     * Requirement 10.8
     */
    fun resetRefreshRate() {
        viewModelScope.launch {
            val result = displayManager.resetToDefault()
            if (result.isSuccess) {
                val supportedRates = displayManager.getSupportedRefreshRates()
                _uiState.update { 
                    it.copy(
                        minRefreshRate = supportedRates.minOrNull() ?: 60f,
                        maxRefreshRate = supportedRates.maxOrNull() ?: 60f
                    )
                }
                _events.emit(SettingsEvent.ShowMessage("Refresh rate reset to default"))
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Failed to reset refresh rate"))
            }
        }
    }
    
    /**
     * Clear all snapshots.
     */
    fun clearSnapshots() {
        viewModelScope.launch {
            val snapshotDir = File(context.filesDir, "snapshots")
            snapshotDir.deleteRecursively()
            _uiState.update { it.copy(snapshotCount = 0) }
            _events.emit(SettingsEvent.ShowMessage("Snapshots cleared"))
        }
    }
    
    /**
     * Refresh log count after viewing logs.
     */
    fun refreshLogCount() {
        viewModelScope.launch {
            val logCount = actionLogger.getActionHistory().size
            _uiState.update { it.copy(logCount = logCount) }
        }
    }
    
    /**
     * Check root access and set mode if available.
     */
    suspend fun checkAndSetRootMode(): Boolean {
        val hasRoot = permissionBridge.checkRootNow()
        if (hasRoot) {
            permissionBridge.setMode(ExecutionMode.Root)
            _uiState.update { it.copy(executionMode = ExecutionMode.Root, canControlDisplay = true) }
            loadDisplaySettings()
        }
        return hasRoot
    }
    
    /**
     * Check Shizuku and set mode if available.
     */
    fun checkAndSetShizukuMode(): Boolean {
        if (permissionBridge.isShizukuReady()) {
            permissionBridge.setMode(ExecutionMode.Shizuku)
            _uiState.update { it.copy(executionMode = ExecutionMode.Shizuku, canControlDisplay = true) }
            viewModelScope.launch { loadDisplaySettings() }
            return true
        }
        return false
    }
    
    /**
     * Set view-only mode.
     */
    fun setViewOnlyMode() {
        permissionBridge.setMode(ExecutionMode.None)
        _uiState.update { it.copy(executionMode = ExecutionMode.None, canControlDisplay = false) }
    }
    
    /**
     * Load display settings when mode changes.
     */
    private suspend fun loadDisplaySettings() {
        val supportedRates = displayManager.getSupportedRefreshRates()
        val minRate = displayManager.getMinRefreshRate().getOrNull() ?: supportedRates.minOrNull() ?: 60f
        val maxRate = displayManager.getMaxRefreshRate().getOrNull() ?: supportedRates.maxOrNull() ?: 60f
        
        _uiState.update {
            it.copy(
                supportedRefreshRates = supportedRates,
                minRefreshRate = minRate,
                maxRefreshRate = maxRate
            )
        }
    }
    
    private fun getSnapshotCount(): Int {
        val snapshotDir = File(context.filesDir, "snapshots")
        return snapshotDir.listFiles()?.size ?: 0
    }
}

/**
 * UI state for Settings screen.
 */
data class SettingsUiState(
    val theme: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val dynamicColors: Boolean = true,
    val supportsDynamicColors: Boolean = false,
    val executionMode: ExecutionMode = ExecutionMode.None,
    val confirmActions: Boolean = true,
    val protectSystem: Boolean = true,
    val autoSnapshot: Boolean = true,
    val logCount: Int = 0,
    val snapshotCount: Int = 0,
    val canControlDisplay: Boolean = false,
    val minRefreshRate: Float = 60f,
    val maxRefreshRate: Float = 60f,
    val supportedRefreshRates: List<Float> = listOf(60f),
    val appVersion: String = ""
) {
    val themeDisplayName: String
        get() = when (theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Light"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
            else -> "System Default"
        }
}

/**
 * One-time events from Settings screen.
 */
sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
    object NavigateToSetup : SettingsEvent()
    object RestartApp : SettingsEvent()
    object RestartRequired : SettingsEvent()
}
