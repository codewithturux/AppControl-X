package com.appcontrolx.domain.manager

import android.content.Context
import android.view.WindowManager
import com.appcontrolx.domain.executor.CommandExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages display refresh rate settings.
 * Uses `settings put system` commands to control min/max refresh rates.
 * 
 * Requirements: 10.5, 10.6, 10.7, 10.8, 10.9
 */
@Singleton
class DisplayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val executor: CommandExecutor
) {
    
    companion object {
        // Settings keys for refresh rate
        const val SETTING_MIN_REFRESH_RATE = "min_refresh_rate"
        const val SETTING_PEAK_REFRESH_RATE = "peak_refresh_rate"
    }
    
    /**
     * Get the current minimum refresh rate setting.
     * 
     * @return Current min refresh rate in Hz, or device default if not set
     */
    suspend fun getMinRefreshRate(): Result<Float> {
        val result = executor.execute("settings get system $SETTING_MIN_REFRESH_RATE")
        return result.map { output ->
            parseRefreshRate(output) ?: getDefaultRefreshRate()
        }
    }
    
    /**
     * Get the current maximum (peak) refresh rate setting.
     * 
     * @return Current max refresh rate in Hz, or device default if not set
     */
    suspend fun getMaxRefreshRate(): Result<Float> {
        val result = executor.execute("settings get system $SETTING_PEAK_REFRESH_RATE")
        return result.map { output ->
            parseRefreshRate(output) ?: getDeviceMaxRefreshRate()
        }
    }
    
    /**
     * Get the current display refresh rate from WindowManager.
     * 
     * @return Current refresh rate in Hz
     */
    fun getCurrentRefreshRate(): Float {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay.refreshRate
    }
    
    /**
     * Get list of supported refresh rates for the device.
     * 
     * @return List of supported refresh rates in Hz
     */
    fun getSupportedRefreshRates(): List<Float> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        
        return try {
            display.supportedModes
                .map { it.refreshRate }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            listOf(60f) // Fallback to 60Hz
        }
    }

    
    /**
     * Set the minimum refresh rate.
     * Validates that the rate is within supported range and <= max rate.
     * 
     * @param hz The minimum refresh rate in Hz
     * @return Result.success if set, Result.failure with error
     */
    suspend fun setMinRefreshRate(hz: Float): Result<Unit> {
        // Validate bounds
        val validation = validateRefreshRate(hz, isMin = true)
        if (validation.isFailure) return validation
        
        val command = "settings put system $SETTING_MIN_REFRESH_RATE $hz"
        return executor.execute(command).map { }
    }
    
    /**
     * Set the maximum (peak) refresh rate.
     * Validates that the rate is within supported range and >= min rate.
     * 
     * @param hz The maximum refresh rate in Hz
     * @return Result.success if set, Result.failure with error
     */
    suspend fun setMaxRefreshRate(hz: Float): Result<Unit> {
        // Validate bounds
        val validation = validateRefreshRate(hz, isMin = false)
        if (validation.isFailure) return validation
        
        val command = "settings put system $SETTING_PEAK_REFRESH_RATE $hz"
        return executor.execute(command).map { }
    }
    
    /**
     * Reset refresh rate settings to device defaults.
     * Deletes both min and peak refresh rate settings.
     * 
     * @return Result.success if reset, Result.failure with error
     */
    suspend fun resetToDefault(): Result<Unit> {
        val commands = listOf(
            "settings delete system $SETTING_MIN_REFRESH_RATE",
            "settings delete system $SETTING_PEAK_REFRESH_RATE"
        )
        return executor.executeBatch(commands)
    }
    
    /**
     * Validate that a refresh rate is within supported bounds.
     * 
     * @param hz The refresh rate to validate
     * @param isMin Whether this is for min rate (true) or max rate (false)
     * @return Result.success if valid, Result.failure with error
     */
    private suspend fun validateRefreshRate(hz: Float, isMin: Boolean): Result<Unit> {
        val supported = getSupportedRefreshRates()
        
        if (supported.isEmpty()) {
            return Result.failure(
                RefreshRateException("Cannot determine supported refresh rates")
            )
        }
        
        val minSupported = supported.minOrNull() ?: 60f
        val maxSupported = supported.maxOrNull() ?: 60f
        
        // Check if within device supported range
        if (hz < minSupported || hz > maxSupported) {
            return Result.failure(
                RefreshRateException("Refresh rate $hz Hz is outside supported range ($minSupported - $maxSupported Hz)")
            )
        }
        
        // Check min <= max constraint
        if (isMin) {
            val currentMax = getMaxRefreshRate().getOrNull() ?: maxSupported
            if (hz > currentMax) {
                return Result.failure(
                    RefreshRateException("Min refresh rate ($hz Hz) cannot exceed max rate ($currentMax Hz)")
                )
            }
        } else {
            val currentMin = getMinRefreshRate().getOrNull() ?: minSupported
            if (hz < currentMin) {
                return Result.failure(
                    RefreshRateException("Max refresh rate ($hz Hz) cannot be less than min rate ($currentMin Hz)")
                )
            }
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Parse refresh rate from settings output.
     */
    private fun parseRefreshRate(output: String): Float? {
        val trimmed = output.trim()
        if (trimmed == "null" || trimmed.isEmpty()) return null
        return trimmed.toFloatOrNull()
    }
    
    /**
     * Get device default refresh rate (usually 60Hz).
     */
    private fun getDefaultRefreshRate(): Float {
        return getSupportedRefreshRates().minOrNull() ?: 60f
    }
    
    /**
     * Get device maximum supported refresh rate.
     */
    private fun getDeviceMaxRefreshRate(): Float {
        return getSupportedRefreshRates().maxOrNull() ?: 60f
    }
}

/**
 * Exception for refresh rate validation errors.
 */
class RefreshRateException(message: String) : Exception(message)
