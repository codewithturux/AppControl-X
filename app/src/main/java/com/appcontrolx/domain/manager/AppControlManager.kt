package com.appcontrolx.domain.manager

import com.appcontrolx.data.model.AppAction
import com.appcontrolx.domain.executor.CommandExecutor
import com.appcontrolx.domain.validator.SafetyValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app control actions like freeze, unfreeze, force-stop, etc.
 * Integrates with SafetyValidator to prevent actions on critical packages.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8
 */
@Singleton
class AppControlManager @Inject constructor(
    private val executor: CommandExecutor
) {
    
    /**
     * Freeze an app by disabling it for the current user.
     * Command: pm disable-user --user 0 <package>
     * 
     * @param packageName The package to freeze
     * @return Result.success if frozen, Result.failure with error
     */
    suspend fun freezeApp(packageName: String): Result<Unit> {
        val validation = validatePackageForAction(packageName, AppAction.FREEZE)
        if (validation.isFailure) return validation
        
        val command = "pm disable-user --user 0 $packageName"
        return executor.execute(command).map { }
    }
    
    /**
     * Unfreeze an app by enabling it.
     * Command: pm enable <package>
     * 
     * @param packageName The package to unfreeze
     * @return Result.success if unfrozen, Result.failure with error
     */
    suspend fun unfreezeApp(packageName: String): Result<Unit> {
        val validation = validatePackageForAction(packageName, AppAction.UNFREEZE)
        if (validation.isFailure) return validation
        
        val command = "pm enable $packageName"
        return executor.execute(command).map { }
    }
    
    /**
     * Force stop an app.
     * Command: am force-stop <package>
     * 
     * @param packageName The package to force stop
     * @return Result.success if stopped, Result.failure with error
     */
    suspend fun forceStop(packageName: String): Result<Unit> {
        val validation = validatePackageForAction(packageName, AppAction.FORCE_STOP)
        if (validation.isFailure) return validation
        
        val command = "am force-stop $packageName"
        return executor.execute(command).map { }
    }

    
    /**
     * Uninstall an app for the current user (keeps data).
     * Command: pm uninstall -k --user 0 <package>
     * 
     * @param packageName The package to uninstall
     * @return Result.success if uninstalled, Result.failure with error
     */
    suspend fun uninstallApp(packageName: String): Result<Unit> {
        val validation = validatePackageForAction(packageName, AppAction.UNINSTALL)
        if (validation.isFailure) return validation
        
        val command = "pm uninstall -k --user 0 $packageName"
        return executor.execute(command).map { }
    }
    
    /**
     * Clear app cache only.
     * Command: pm clear --cache-only <package>
     * 
     * @param packageName The package to clear cache
     * @return Result.success if cleared, Result.failure with error
     */
    suspend fun clearCache(packageName: String): Result<Unit> {
        val validation = validatePackageForAction(packageName, AppAction.CLEAR_CACHE)
        if (validation.isFailure) return validation
        
        val command = "pm clear --cache-only $packageName"
        return executor.execute(command).map { }
    }
    
    /**
     * Clear all app data (including cache).
     * Command: pm clear <package>
     * 
     * @param packageName The package to clear data
     * @return Result.success if cleared, Result.failure with error
     */
    suspend fun clearData(packageName: String): Result<Unit> {
        val validation = validatePackageForAction(packageName, AppAction.CLEAR_DATA)
        if (validation.isFailure) return validation
        
        val command = "pm clear $packageName"
        return executor.execute(command).map { }
    }
    
    /**
     * Execute a batch action on multiple packages with progress callback.
     * Continues processing even if some packages fail.
     * 
     * @param packages List of package names to process
     * @param action The action to perform
     * @param onProgress Callback with (current, total, packageName)
     * @return BatchResult with success/failure counts
     */
    suspend fun executeBatchAction(
        packages: List<String>,
        action: AppAction,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): BatchResult {
        val total = packages.size
        var successCount = 0
        var failureCount = 0
        val failedPackages = mutableListOf<String>()
        val errors = mutableMapOf<String, String>()
        
        packages.forEachIndexed { index, packageName ->
            onProgress(index + 1, total, packageName)
            
            val result = executeAction(packageName, action)
            if (result.isSuccess) {
                successCount++
            } else {
                failureCount++
                failedPackages.add(packageName)
                errors[packageName] = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
        
        return BatchResult(
            total = total,
            successCount = successCount,
            failureCount = failureCount,
            failedPackages = failedPackages,
            errors = errors
        )
    }
    
    /**
     * Execute a single action on a package.
     */
    private suspend fun executeAction(packageName: String, action: AppAction): Result<Unit> {
        return when (action) {
            AppAction.FREEZE -> freezeApp(packageName)
            AppAction.UNFREEZE -> unfreezeApp(packageName)
            AppAction.FORCE_STOP -> forceStop(packageName)
            AppAction.UNINSTALL -> uninstallApp(packageName)
            AppAction.CLEAR_CACHE -> clearCache(packageName)
            AppAction.CLEAR_DATA -> clearData(packageName)
            AppAction.RESTRICT_BACKGROUND, AppAction.ALLOW_BACKGROUND -> {
                // These are handled by BatteryManager
                Result.failure(UnsupportedOperationException("Use BatteryManager for background actions"))
            }
        }
    }
    
    /**
     * Validate package name and check if action is allowed.
     */
    private fun validatePackageForAction(packageName: String, action: AppAction): Result<Unit> {
        // First validate package name format and injection
        val nameValidation = SafetyValidator.validatePackageName(packageName)
        if (nameValidation.isFailure) return nameValidation
        
        // Check if package is critical (blocked for all actions)
        if (SafetyValidator.isCritical(packageName)) {
            return Result.failure(
                CriticalPackageException("Cannot perform $action on critical package: $packageName")
            )
        }
        
        // Check if package is force-stop-only
        if (SafetyValidator.isForceStopOnly(packageName) && action != AppAction.FORCE_STOP) {
            return Result.failure(
                ForceStopOnlyException("Package $packageName only allows FORCE_STOP action")
            )
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Generate the shell command for a given action and package.
     * Useful for testing and debugging.
     */
    fun generateCommand(packageName: String, action: AppAction): String {
        return when (action) {
            AppAction.FREEZE -> "pm disable-user --user 0 $packageName"
            AppAction.UNFREEZE -> "pm enable $packageName"
            AppAction.FORCE_STOP -> "am force-stop $packageName"
            AppAction.UNINSTALL -> "pm uninstall -k --user 0 $packageName"
            AppAction.CLEAR_CACHE -> "pm clear --cache-only $packageName"
            AppAction.CLEAR_DATA -> "pm clear $packageName"
            AppAction.RESTRICT_BACKGROUND, AppAction.ALLOW_BACKGROUND -> {
                throw UnsupportedOperationException("Use BatteryManager for background actions")
            }
        }
    }
}

/**
 * Result of a batch operation.
 */
data class BatchResult(
    val total: Int,
    val successCount: Int,
    val failureCount: Int,
    val failedPackages: List<String>,
    val errors: Map<String, String>
) {
    val isAllSuccess: Boolean get() = failureCount == 0
    val isAllFailure: Boolean get() = successCount == 0
}

/**
 * Exception thrown when trying to modify a critical system package.
 */
class CriticalPackageException(message: String) : Exception(message)

/**
 * Exception thrown when trying to perform non-force-stop action on force-stop-only package.
 */
class ForceStopOnlyException(message: String) : Exception(message)
