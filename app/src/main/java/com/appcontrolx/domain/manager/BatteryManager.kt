package com.appcontrolx.domain.manager

import com.appcontrolx.domain.executor.CommandExecutor
import com.appcontrolx.domain.validator.SafetyValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages background restriction and battery optimization for apps.
 * Uses appops commands to control background execution permissions.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Singleton
class BatteryManager @Inject constructor(
    private val executor: CommandExecutor
) {
    
    companion object {
        // AppOps operations for background restriction
        const val OP_RUN_IN_BACKGROUND = "RUN_IN_BACKGROUND"
        const val OP_RUN_ANY_IN_BACKGROUND = "RUN_ANY_IN_BACKGROUND"
        const val OP_WAKE_LOCK = "WAKE_LOCK"
        const val OP_BOOT_COMPLETED = "BOOT_COMPLETED"
        
        // AppOps modes
        const val MODE_ALLOW = "allow"
        const val MODE_IGNORE = "ignore"
        const val MODE_DEFAULT = "default"
    }
    
    /**
     * Restrict background execution for an app.
     * Sets all background-related appops to ignore and force-stops the app.
     * 
     * Operations restricted:
     * - RUN_IN_BACKGROUND
     * - RUN_ANY_IN_BACKGROUND
     * - WAKE_LOCK
     * - BOOT_COMPLETED
     * 
     * @param packageName The package to restrict
     * @param uid The UID of the package
     * @return Result.success if restricted, Result.failure with error
     */
    suspend fun restrictBackground(packageName: String, uid: Int): Result<Unit> {
        // Validate package name
        val validation = SafetyValidator.validatePackageName(packageName)
        if (validation.isFailure) return validation
        
        // Check if package is critical
        if (SafetyValidator.isCritical(packageName)) {
            return Result.failure(
                CriticalPackageException("Cannot restrict background for critical package: $packageName")
            )
        }
        
        // Generate all restriction commands
        val commands = generateRestrictCommands(packageName, uid)
        
        // Execute all commands
        val result = executor.executeBatch(commands)
        if (result.isFailure) return result
        
        // Force-stop the app to kill current processes (Requirement 4.4)
        return executor.execute("am force-stop $packageName").map { }
    }

    
    /**
     * Allow background execution for an app.
     * Sets all background-related appops back to allow.
     * 
     * @param packageName The package to allow
     * @param uid The UID of the package
     * @return Result.success if allowed, Result.failure with error
     */
    suspend fun allowBackground(packageName: String, uid: Int): Result<Unit> {
        // Validate package name
        val validation = SafetyValidator.validatePackageName(packageName)
        if (validation.isFailure) return validation
        
        // Generate all allow commands
        val commands = generateAllowCommands(packageName, uid)
        
        // Execute all commands
        return executor.executeBatch(commands)
    }
    
    /**
     * Get the background restriction status for an app.
     * 
     * @param packageName The package to check
     * @param uid The UID of the package
     * @return BackgroundStatus indicating current restriction state
     */
    suspend fun getBackgroundStatus(packageName: String, uid: Int): Result<BackgroundStatus> {
        // Validate package name
        val validation = SafetyValidator.validatePackageName(packageName)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }
        
        // Query RUN_IN_BACKGROUND status as primary indicator
        val command = "appops get $uid $OP_RUN_IN_BACKGROUND"
        val result = executor.execute(command)
        
        return result.map { output ->
            parseBackgroundStatus(output)
        }
    }
    
    /**
     * Generate commands to restrict all background operations.
     */
    fun generateRestrictCommands(packageName: String, uid: Int): List<String> {
        return listOf(
            "appops set $uid $OP_RUN_IN_BACKGROUND $MODE_IGNORE",
            "appops set $uid $OP_RUN_ANY_IN_BACKGROUND $MODE_IGNORE",
            "appops set $uid $OP_WAKE_LOCK $MODE_IGNORE",
            "appops set $uid $OP_BOOT_COMPLETED $MODE_IGNORE"
        )
    }
    
    /**
     * Generate commands to allow all background operations.
     */
    fun generateAllowCommands(packageName: String, uid: Int): List<String> {
        return listOf(
            "appops set $uid $OP_RUN_IN_BACKGROUND $MODE_ALLOW",
            "appops set $uid $OP_RUN_ANY_IN_BACKGROUND $MODE_ALLOW",
            "appops set $uid $OP_WAKE_LOCK $MODE_ALLOW",
            "appops set $uid $OP_BOOT_COMPLETED $MODE_ALLOW"
        )
    }
    
    /**
     * Parse the output of appops get command to determine status.
     */
    private fun parseBackgroundStatus(output: String): BackgroundStatus {
        val trimmed = output.trim().lowercase()
        return when {
            trimmed.contains("ignore") -> BackgroundStatus.RESTRICTED
            trimmed.contains("allow") -> BackgroundStatus.ALLOWED
            trimmed.contains("default") -> BackgroundStatus.DEFAULT
            else -> BackgroundStatus.DEFAULT
        }
    }
}

/**
 * Background restriction status for an app.
 */
enum class BackgroundStatus {
    /** App is restricted from running in background */
    RESTRICTED,
    /** App is explicitly allowed to run in background */
    ALLOWED,
    /** App uses system default background policy */
    DEFAULT
}
