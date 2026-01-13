package com.appcontrolx.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.domain.executor.PermissionBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Setup Wizard.
 * 
 * Responsibilities:
 * - Track current wizard step
 * - Handle mode selection and verification
 * - Save setup_complete flag
 * 
 * Requirements: 0.3, 0.4, 0.5, 0.6, 0.7
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionBridge: PermissionBridge
) : ViewModel() {
    
    companion object {
        private const val PREFS_NAME = "appcontrolx_prefs"
        private const val PREFS_SETUP_COMPLETE = "setup_complete"
        
        const val STEP_WELCOME = 0
        const val STEP_MODE_SELECTION = 1
        const val STEP_COMPLETE = 2
        const val TOTAL_STEPS = 3
    }
    
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * UI state for the setup wizard.
     */
    data class SetupUiState(
        val currentStep: Int = STEP_WELCOME,
        val selectedMode: ExecutionMode? = null,
        val rootAvailable: Boolean = false,
        val rootGranted: Boolean = false,
        val shizukuAvailable: Boolean = false,
        val shizukuGranted: Boolean = false,
        val isCheckingRoot: Boolean = false,
        val isCheckingShizuku: Boolean = false,
        val isSetupComplete: Boolean = false,
        val errorMessage: String? = null
    ) {
        val canProceedFromModeSelection: Boolean
            get() = selectedMode != null
        
        val isVerifyingMode: Boolean
            get() = isCheckingRoot || isCheckingShizuku
    }
    
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()
    
    init {
        // Check initial permission states
        checkInitialPermissions()
    }
    
    /**
     * Check initial permission states on ViewModel creation.
     */
    private fun checkInitialPermissions() {
        viewModelScope.launch {
            val rootAvailable = permissionBridge.isRootAvailable()
            val shizukuAvailable = permissionBridge.isShizukuAvailable()
            val shizukuGranted = permissionBridge.isShizukuReady()
            
            _uiState.update { state ->
                state.copy(
                    rootAvailable = rootAvailable,
                    rootGranted = rootAvailable,
                    shizukuAvailable = shizukuAvailable,
                    shizukuGranted = shizukuGranted
                )
            }
        }
    }

    
    // ==================== Step Navigation ====================
    
    /**
     * Navigate to the next step in the wizard.
     */
    fun nextStep() {
        _uiState.update { state ->
            val nextStep = (state.currentStep + 1).coerceAtMost(TOTAL_STEPS - 1)
            state.copy(currentStep = nextStep)
        }
    }
    
    /**
     * Navigate to a specific step in the wizard.
     */
    fun goToStep(step: Int) {
        if (step in 0 until TOTAL_STEPS) {
            _uiState.update { state ->
                state.copy(currentStep = step)
            }
        }
    }
    
    /**
     * Navigate to the previous step in the wizard.
     */
    fun previousStep() {
        _uiState.update { state ->
            val prevStep = (state.currentStep - 1).coerceAtLeast(0)
            state.copy(currentStep = prevStep)
        }
    }
    
    // ==================== Mode Selection ====================
    
    /**
     * Select an execution mode.
     * 
     * Requirement 0.4
     */
    fun selectMode(mode: ExecutionMode) {
        permissionBridge.setMode(mode)
        _uiState.update { state ->
            state.copy(
                selectedMode = mode,
                errorMessage = null
            )
        }
    }
    
    /**
     * Check and request root access.
     * 
     * Requirement 0.5
     */
    fun checkRootAccess() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingRoot = true, errorMessage = null) }
            
            val hasRoot = withContext(Dispatchers.IO) {
                permissionBridge.checkRootNow()
            }
            
            _uiState.update { state ->
                if (hasRoot) {
                    permissionBridge.setMode(ExecutionMode.Root)
                    state.copy(
                        isCheckingRoot = false,
                        rootGranted = true,
                        rootAvailable = true,
                        selectedMode = ExecutionMode.Root
                    )
                } else {
                    state.copy(
                        isCheckingRoot = false,
                        rootGranted = false,
                        errorMessage = "Root access denied or not available"
                    )
                }
            }
        }
    }
    
    /**
     * Check Shizuku availability and permission.
     * 
     * Requirement 0.6
     */
    fun checkShizukuAccess() {
        _uiState.update { it.copy(isCheckingShizuku = true, errorMessage = null) }
        
        val shizukuAvailable = permissionBridge.isShizukuAvailable()
        
        if (!shizukuAvailable) {
            _uiState.update { state ->
                state.copy(
                    isCheckingShizuku = false,
                    shizukuAvailable = false,
                    errorMessage = "Shizuku is not running. Please start Shizuku first."
                )
            }
            return
        }
        
        val shizukuGranted = permissionBridge.isShizukuPermissionGranted()
        
        if (shizukuGranted) {
            permissionBridge.setMode(ExecutionMode.Shizuku)
            _uiState.update { state ->
                state.copy(
                    isCheckingShizuku = false,
                    shizukuAvailable = true,
                    shizukuGranted = true,
                    selectedMode = ExecutionMode.Shizuku
                )
            }
        } else {
            // Request permission - the result will be handled by onShizukuPermissionResult
            permissionBridge.requestShizukuPermission()
            _uiState.update { state ->
                state.copy(
                    isCheckingShizuku = false,
                    shizukuAvailable = true,
                    shizukuGranted = false
                )
            }
        }
    }
    
    /**
     * Handle Shizuku permission result callback.
     * Call this from the fragment when Shizuku permission result is received.
     */
    fun onShizukuPermissionResult(granted: Boolean) {
        if (granted) {
            permissionBridge.setMode(ExecutionMode.Shizuku)
            _uiState.update { state ->
                state.copy(
                    shizukuGranted = true,
                    selectedMode = ExecutionMode.Shizuku,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    shizukuGranted = false,
                    errorMessage = "Shizuku permission denied"
                )
            }
        }
    }
    
    /**
     * Select View-Only mode (no permissions required).
     */
    fun selectViewOnlyMode() {
        permissionBridge.setMode(ExecutionMode.None)
        _uiState.update { state ->
            state.copy(
                selectedMode = ExecutionMode.None,
                errorMessage = null
            )
        }
    }

    
    // ==================== Setup Completion ====================
    
    /**
     * Complete the setup wizard.
     * Saves setup_complete flag to preferences.
     * 
     * Requirement 0.7
     */
    fun completeSetup() {
        prefs.edit().putBoolean(PREFS_SETUP_COMPLETE, true).apply()
        _uiState.update { state ->
            state.copy(isSetupComplete = true)
        }
    }
    
    /**
     * Check if setup is already complete.
     * 
     * @return true if setup has been completed previously
     */
    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(PREFS_SETUP_COMPLETE, false)
    }
    
    /**
     * Reset setup state (for testing or settings reset).
     */
    fun resetSetup() {
        prefs.edit().putBoolean(PREFS_SETUP_COMPLETE, false).apply()
        permissionBridge.clearSavedMode()
        _uiState.update { SetupUiState() }
    }
    
    // ==================== Permission Refresh ====================
    
    /**
     * Refresh permission states.
     * Call this when returning to the mode selection screen.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val rootAvailable = permissionBridge.isRootAvailable()
            val shizukuAvailable = permissionBridge.isShizukuAvailable()
            val shizukuGranted = permissionBridge.isShizukuReady()
            
            _uiState.update { state ->
                state.copy(
                    rootAvailable = rootAvailable,
                    rootGranted = if (rootAvailable) state.rootGranted else false,
                    shizukuAvailable = shizukuAvailable,
                    shizukuGranted = shizukuGranted
                )
            }
        }
    }
    
    /**
     * Get the currently selected mode.
     */
    fun getSelectedMode(): ExecutionMode? {
        return _uiState.value.selectedMode
    }
    
    /**
     * Get the current mode from PermissionBridge.
     */
    fun getCurrentMode(): ExecutionMode {
        return permissionBridge.mode
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }
    }
}
