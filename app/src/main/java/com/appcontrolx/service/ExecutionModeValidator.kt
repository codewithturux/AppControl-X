package com.appcontrolx.service

import android.content.Context
import androidx.preference.PreferenceManager
import com.appcontrolx.utils.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionModeValidator @Inject constructor(
    private val context: Context,
    private val permissionBridge: PermissionBridge
) {
    
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    
    /**
     * Validate current execution mode and fallback to view-only if needed
     */
    fun validateAndFallback(): ValidationResult {
        val currentMode = prefs.getString(Constants.PREFS_EXECUTION_MODE, Constants.MODE_NONE) ?: Constants.MODE_NONE
        
        return when (currentMode) {
            Constants.MODE_ROOT -> {
                if (permissionBridge.isRootAvailable()) {
                    ValidationResult.Valid(currentMode)
                } else {
                    prefs.edit().putString(Constants.PREFS_EXECUTION_MODE, Constants.MODE_NONE).apply()
                    ValidationResult.Fallback(Constants.MODE_NONE, "Root access is no longer available")
                }
            }
            
            Constants.MODE_SHIZUKU -> {
                if (permissionBridge.isShizukuReady()) {
                    ValidationResult.Valid(currentMode)
                } else {
                    prefs.edit().putString(Constants.PREFS_EXECUTION_MODE, Constants.MODE_NONE).apply()
                    ValidationResult.Fallback(Constants.MODE_NONE, "Shizuku is no longer available")
                }
            }
            
            else -> ValidationResult.Valid(currentMode)
        }
    }
    
    /**
     * Check if we can switch to the requested mode
     */
    suspend fun canSwitchTo(mode: String): SwitchResult {
        return when (mode) {
            Constants.MODE_ROOT -> {
                if (permissionBridge.checkRootNow()) {
                    SwitchResult.Success
                } else {
                    SwitchResult.Failed("Root access denied or not available")
                }
            }
            
            Constants.MODE_SHIZUKU -> {
                when {
                    !permissionBridge.isShizukuAvailable() -> {
                        SwitchResult.Failed("Shizuku is not installed or running")
                    }
                    !permissionBridge.isShizukuPermissionGranted() -> {
                        SwitchResult.Failed("Shizuku permission not granted")
                    }
                    else -> SwitchResult.Success
                }
            }
            
            Constants.MODE_NONE -> SwitchResult.Success
            
            else -> SwitchResult.Failed("Unknown execution mode")
        }
    }
    
    sealed class ValidationResult {
        data class Valid(val mode: String) : ValidationResult()
        data class Fallback(val newMode: String, val reason: String) : ValidationResult()
    }
    
    sealed class SwitchResult {
        object Success : SwitchResult()
        data class Failed(val reason: String) : SwitchResult()
    }
}
