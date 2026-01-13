package com.appcontrolx.domain.manager

import com.appcontrolx.data.model.AnimationScale
import com.appcontrolx.domain.executor.CommandExecutor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnimationScaleManager using shell commands.
 * Uses `settings put global` commands to control animation scales.
 * 
 * Requirements: 7.4
 */
@Singleton
class AnimationScaleManagerImpl @Inject constructor(
    private val executor: CommandExecutor
) : AnimationScaleManager {

    companion object {
        // Settings keys for animation scales
        const val SETTING_WINDOW_ANIMATION_SCALE = "window_animation_scale"
        const val SETTING_TRANSITION_ANIMATION_SCALE = "transition_animation_scale"
        const val SETTING_ANIMATOR_DURATION_SCALE = "animator_duration_scale"
    }

    override suspend fun getWindowAnimationScale(): Result<Float> {
        return getAnimationScale(SETTING_WINDOW_ANIMATION_SCALE)
    }

    override suspend fun getTransitionAnimationScale(): Result<Float> {
        return getAnimationScale(SETTING_TRANSITION_ANIMATION_SCALE)
    }

    override suspend fun getAnimatorDurationScale(): Result<Float> {
        return getAnimationScale(SETTING_ANIMATOR_DURATION_SCALE)
    }

    override suspend fun getAllAnimationScales(): Result<AnimationScale> {
        return try {
            val windowScale = getWindowAnimationScale().getOrThrow()
            val transitionScale = getTransitionAnimationScale().getOrThrow()
            val animatorScale = getAnimatorDurationScale().getOrThrow()
            Result.success(AnimationScale(windowScale, transitionScale, animatorScale))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setWindowAnimationScale(scale: Float): Result<Unit> {
        return setAnimationScale(SETTING_WINDOW_ANIMATION_SCALE, scale)
    }

    override suspend fun setTransitionAnimationScale(scale: Float): Result<Unit> {
        return setAnimationScale(SETTING_TRANSITION_ANIMATION_SCALE, scale)
    }

    override suspend fun setAnimatorDurationScale(scale: Float): Result<Unit> {
        return setAnimationScale(SETTING_ANIMATOR_DURATION_SCALE, scale)
    }

    override suspend fun setAllAnimationScales(scale: Float): Result<Unit> {
        val clampedScale = scale.coerceIn(AnimationScale.MIN_SCALE, AnimationScale.MAX_SCALE)
        val commands = listOf(
            buildSetCommand(SETTING_WINDOW_ANIMATION_SCALE, clampedScale),
            buildSetCommand(SETTING_TRANSITION_ANIMATION_SCALE, clampedScale),
            buildSetCommand(SETTING_ANIMATOR_DURATION_SCALE, clampedScale)
        )
        return executor.executeBatch(commands)
    }

    /**
     * Get animation scale value from settings.
     */
    private suspend fun getAnimationScale(settingKey: String): Result<Float> {
        val command = "settings get global $settingKey"
        return executor.execute(command).map { output ->
            parseAnimationScale(output)
        }
    }

    /**
     * Set animation scale value in settings.
     * Requirements: 7.4
     */
    private suspend fun setAnimationScale(settingKey: String, scale: Float): Result<Unit> {
        val clampedScale = scale.coerceIn(AnimationScale.MIN_SCALE, AnimationScale.MAX_SCALE)
        val command = buildSetCommand(settingKey, clampedScale)
        return executor.execute(command).map { }
    }

    /**
     * Build the shell command to set an animation scale.
     * Format: settings put global {setting_key} {value}
     * Requirements: 7.4
     */
    private fun buildSetCommand(settingKey: String, scale: Float): String {
        return "settings put global $settingKey $scale"
    }

    /**
     * Parse animation scale from settings output.
     * Returns 1.0 (default) if parsing fails or value is null.
     */
    private fun parseAnimationScale(output: String): Float {
        val trimmed = output.trim()
        if (trimmed == "null" || trimmed.isEmpty()) return 1.0f
        return trimmed.toFloatOrNull() ?: 1.0f
    }
}

/**
 * Exception for animation scale errors.
 */
class AnimationScaleException(message: String) : Exception(message)
