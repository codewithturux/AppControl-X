package com.appcontrolx.domain.executor

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.appcontrolx.BuildConfig
import com.appcontrolx.IShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandExecutor implementation using Shizuku API.
 * Executes commands through a bound AIDL service running with Shizuku privileges.
 * 
 * Requirements: 11.4
 */
@Singleton
class ShizukuExecutor @Inject constructor() : CommandExecutor {
    
    private var shellService: IShellService? = null
    private var serviceLatch = CountDownLatch(1)
    private var isBound = false
    
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shellService = IShellService.Stub.asInterface(service)
            serviceLatch.countDown()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            isBound = false
        }
    }
    
    /**
     * Bind to the Shizuku shell service.
     * Call this when Shizuku becomes available.
     */
    fun bindService() {
        if (!Shizuku.pingBinder()) return
        if (isBound) return
        
        try {
            // Reset latch for new binding
            serviceLatch = CountDownLatch(1)
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
            isBound = true
        } catch (e: Exception) {
            // Shizuku not ready
        }
    }

    
    /**
     * Unbind from the Shizuku shell service.
     * Call this when cleaning up or when Shizuku becomes unavailable.
     */
    fun unbindService() {
        try {
            if (isBound) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        } catch (e: Exception) {
            // Ignore unbind errors
        }
        shellService = null
        isBound = false
    }
    
    /**
     * Get the shell service, waiting for connection if necessary.
     * 
     * @param timeoutMs Maximum time to wait for service connection
     * @return The shell service or null if not available
     */
    private fun getService(timeoutMs: Long = 3000): IShellService? {
        if (shellService != null) return shellService
        
        // Try to bind if not already bound
        if (!isBound) {
            bindService()
        }
        
        // Wait for service to connect
        serviceLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return shellService
    }
    
    override suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        val service = getService()
            ?: return@withContext Result.failure(
                ShizukuNotAvailableException("Shizuku service not available")
            )
        
        try {
            val output = service.exec(command)
            if (output.startsWith("ERROR:")) {
                Result.failure(CommandFailedException(output.removePrefix("ERROR:")))
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun executeBatch(commands: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        val service = getService()
            ?: return@withContext Result.failure(
                ShizukuNotAvailableException("Shizuku service not available")
            )
        
        try {
            var lastError: String? = null
            
            for (cmd in commands) {
                val output = service.exec(cmd)
                if (output.startsWith("ERROR:")) {
                    lastError = output.removePrefix("ERROR:")
                    // Continue processing remaining commands (best effort)
                }
            }
            
            if (lastError != null) {
                Result.failure(CommandFailedException(lastError))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        /**
         * Check if Shizuku service is running (binder is alive).
         */
        fun isShizukuRunning(): Boolean {
            return try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Check if Shizuku permission is granted.
         */
        fun isPermissionGranted(): Boolean {
            return try {
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        }
    }
}

/**
 * Exception thrown when Shizuku is not available.
 */
class ShizukuNotAvailableException(message: String) : Exception(message)
