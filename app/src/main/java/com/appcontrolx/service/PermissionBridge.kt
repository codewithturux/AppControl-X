package com.appcontrolx.service

import android.content.Context
import androidx.preference.PreferenceManager
import com.appcontrolx.model.ExecutionMode
import com.appcontrolx.utils.Constants
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

class PermissionBridge(private val context: Context? = null) {
    
    private val prefs by lazy { 
        context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
    }
    
    fun detectMode(): ExecutionMode {
        // First check saved preference - trust it if set
        val savedMode = prefs?.getString(Constants.PREFS_EXECUTION_MODE, null)
        
        // If user explicitly selected a mode, use it (don't auto-detect)
        // Only validate if the mode requires special permissions
        return when (savedMode) {
            Constants.MODE_ROOT -> {
                // Trust saved root mode - Shell might not be ready yet
                // Root will be validated when actually used
                ExecutionMode.Root
            }
            Constants.MODE_SHIZUKU -> {
                // Trust saved shizuku mode
                ExecutionMode.Shizuku
            }
            Constants.MODE_NONE -> ExecutionMode.None
            else -> {
                // No saved mode - auto detect
                detectAvailableMode()
            }
        }
    }
    
    private fun detectAvailableMode(): ExecutionMode {
        // 1. Check root first (highest priority)
        if (checkRootNow()) {
            saveMode(Constants.MODE_ROOT)
            return ExecutionMode.Root
        }
        
        // 2. Check Shizuku
        if (isShizukuReady()) {
            saveMode(Constants.MODE_SHIZUKU)
            return ExecutionMode.Shizuku
        }
        
        // 3. Fallback - don't save, let user choose
        return ExecutionMode.None
    }
    
    private fun saveMode(mode: String) {
        prefs?.edit()?.putString(Constants.PREFS_EXECUTION_MODE, mode)?.apply()
    }
    
    /**
     * Check root availability NOW (blocking)
     * Use this only during initial setup or explicit check
     */
    fun checkRootNow(): Boolean {
        return try {
            // Get shell instance - this will trigger root request if needed
            Shell.getShell()
            
            // Check if root was granted
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Quick check without blocking - for UI display
     */
    fun isRootAvailable(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            false
        }
    }
    
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    fun isShizukuPermissionGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    fun isShizukuReady(): Boolean {
        return isShizukuAvailable() && isShizukuPermissionGranted()
    }
    
    fun requestShizukuPermission() {
        try {
            if (isShizukuAvailable() && !isShizukuPermissionGranted()) {
                Shizuku.requestPermission(0)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
