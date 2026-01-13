package com.appcontrolx.domain.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.domain.executor.CommandExecutor
import com.appcontrolx.domain.executor.RootExecutor
import com.appcontrolx.domain.executor.ShizukuExecutor
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension for preferences
private val Context.modeDataStore: DataStore<Preferences> by preferencesDataStore(name = "mode_prefs")

/**
 * Watches and manages execution mode availability.
 * Detects mode loss and provides recovery options.
 * 
 * Requirements: 10.1.1, 10.1.2, 10.1.3, 10.1.4, 10.1.5, 10.1.6, 10.1.7
 */
@Singleton
class ModeWatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val KEY_EXECUTION_MODE = stringPreferencesKey("execution_mode")
        private const val MODE_ROOT = "root"
        private const val MODE_SHIZUKU = "shizuku"
        private const val MODE_NONE = "none"
    }
    
    private val _modeStatus = MutableStateFlow<ModeStatus>(ModeStatus.Available)
    
    /**
     * Verify current execution mode availability.
     * Should be called on app resume.
     * 
     * @return ModeStatus indicating if mode is available or lost
     */
    suspend fun verifyCurrentMode(): ModeStatus = withContext(Dispatchers.IO) {
        val savedMode = getSavedMode()
        
        val status = when (savedMode) {
            ExecutionMode.Root -> {
                if (isRootAvailable()) {
                    ModeStatus.Available
                } else {
                    ModeStatus.Lost(
                        previousMode = ExecutionMode.Root,
                        reason = "Root access is no longer available. This may happen if Magisk was updated or su was denied."
                    )
                }
            }
            ExecutionMode.Shizuku -> {
                if (isShizukuReady()) {
                    ModeStatus.Available
                } else {
                    val reason = when {
                        !isShizukuAvailable() -> "Shizuku service is not running."
                        !isShizukuPermissionGranted() -> "Shizuku permission was revoked."
                        else -> "Shizuku is no longer available."
                    }
                    ModeStatus.Lost(
                        previousMode = ExecutionMode.Shizuku,
                        reason = reason
                    )
                }
            }
            ExecutionMode.None -> ModeStatus.Available
        }
        
        _modeStatus.value = status
        status
    }

    
    /**
     * Observe mode status changes.
     * 
     * @return Flow of ModeStatus updates
     */
    fun observeModeStatus(): Flow<ModeStatus> = _modeStatus.asStateFlow()
    
    /**
     * Handle mode loss with user-selected action.
     * 
     * @param action The action to take (Retry, Switch, ViewOnly)
     * @return Result.success if action succeeded, Result.failure with error
     */
    suspend fun handleModeLoss(action: ModeLossAction): Result<Unit> = withContext(Dispatchers.IO) {
        when (action) {
            ModeLossAction.RETRY -> {
                // Re-verify current mode
                val status = verifyCurrentMode()
                if (status is ModeStatus.Available) {
                    Result.success(Unit)
                } else {
                    Result.failure(ModeStillLostException("Mode is still not available after retry"))
                }
            }
            
            ModeLossAction.SWITCH_MODE -> {
                // Detect best available mode
                val newMode = detectAvailableMode()
                saveMode(newMode)
                _modeStatus.value = ModeStatus.Available
                Result.success(Unit)
            }
            
            ModeLossAction.CONTINUE_VIEW_ONLY -> {
                // Switch to None mode
                saveMode(ExecutionMode.None)
                _modeStatus.value = ModeStatus.Available
                Result.success(Unit)
            }
        }
    }
    
    /**
     * Get the currently saved execution mode.
     */
    suspend fun getSavedMode(): ExecutionMode {
        return context.modeDataStore.data.map { prefs ->
            when (prefs[KEY_EXECUTION_MODE]) {
                MODE_ROOT -> ExecutionMode.Root
                MODE_SHIZUKU -> ExecutionMode.Shizuku
                MODE_NONE -> ExecutionMode.None
                else -> ExecutionMode.None
            }
        }.first()
    }
    
    /**
     * Save execution mode to preferences.
     */
    suspend fun saveMode(mode: ExecutionMode) {
        context.modeDataStore.edit { prefs ->
            prefs[KEY_EXECUTION_MODE] = when (mode) {
                ExecutionMode.Root -> MODE_ROOT
                ExecutionMode.Shizuku -> MODE_SHIZUKU
                ExecutionMode.None -> MODE_NONE
            }
        }
    }
    
    /**
     * Detect the best available execution mode.
     * Priority: Root > Shizuku > None
     */
    suspend fun detectAvailableMode(): ExecutionMode = withContext(Dispatchers.IO) {
        when {
            checkRootNow() -> ExecutionMode.Root
            isShizukuReady() -> ExecutionMode.Shizuku
            else -> ExecutionMode.None
        }
    }
    
    /**
     * Check root availability (blocking).
     */
    private fun checkRootNow(): Boolean {
        return try {
            val result = Shell.cmd("id").exec()
            if (result.isSuccess) {
                val output = result.out.joinToString("\n")
                if (output.contains("uid=0")) {
                    return true
                }
            }
            
            val suResult = Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
                .build("su")
            
            suResult.isRoot
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Quick check for root availability.
     */
    private fun isRootAvailable(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if Shizuku service is running.
     */
    private fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if Shizuku permission is granted.
     */
    private fun isShizukuPermissionGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if Shizuku is fully ready (available + permission granted).
     */
    private fun isShizukuReady(): Boolean {
        return isShizukuAvailable() && isShizukuPermissionGranted()
    }
    
    /**
     * Get the appropriate CommandExecutor for the current mode.
     */
    suspend fun getExecutor(): CommandExecutor? {
        return when (getSavedMode()) {
            ExecutionMode.Root -> RootExecutor()
            ExecutionMode.Shizuku -> ShizukuExecutor()
            ExecutionMode.None -> null
        }
    }
}

/**
 * Status of the current execution mode.
 */
sealed class ModeStatus {
    /** Mode is available and working */
    data object Available : ModeStatus()
    
    /** Mode was lost and needs user action */
    data class Lost(
        val previousMode: ExecutionMode,
        val reason: String
    ) : ModeStatus()
}

/**
 * Actions user can take when mode is lost.
 */
enum class ModeLossAction {
    /** Retry checking the current mode */
    RETRY,
    /** Switch to a different available mode */
    SWITCH_MODE,
    /** Continue in view-only mode */
    CONTINUE_VIEW_ONLY
}

/**
 * Exception thrown when mode is still lost after retry.
 */
class ModeStillLostException(message: String) : Exception(message)
