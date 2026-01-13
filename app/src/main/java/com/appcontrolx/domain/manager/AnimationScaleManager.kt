package com.appcontrolx.domain.manager

import com.appcontrolx.data.model.AnimationScale
import com.appcontrolx.domain.executor.CommandExecutor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for managing system animation scale settings.
 * Requirements: 7.4
 */
interface AnimationScaleManager {
    /**
     * Get the current window animation scale.
     * @return Current scale value (0.0 - 10.0)
     */
    suspend fun getWindowAnimationScale(): Result<Float>

    /**
     * Get the current transition animation scale.
     * @return Current scale value (0.0 - 10.0)
     */
    suspend fun getTransitionAnimationScale(): Result<Float>

    /**
     * Get the current animator duration scale.
     * @return Current scale value (0.0 - 10.0)
     */
    suspend fun getAnimatorDurationScale(): Result<Float>

    /**
     * Get all animation scales as an AnimationScale object.
     * @return AnimationScale with all three scale values
     */
    suspend fun getAllAnimationScales(): Result<AnimationScale>

    /**
     * Set the window animation scale.
     * @param scale Scale value (0.0 - 10.0)
     * @return Result.success if set, Result.failure with error
     */
    suspend fun setWindowAnimationScale(scale: Float): Result<Unit>

    /**
     * Set the transition animation scale.
     * @param scale Scale value (0.0 - 10.0)
     * @return Result.success if set, Result.failure with error
     */
    suspend fun setTransitionAnimationScale(scale: Float): Result<Unit>

    /**
     * Set the animator duration scale.
     * @param scale Scale value (0.0 - 10.0)
     * @return Result.success if set, Result.failure with error
     */
    suspend fun setAnimatorDurationScale(scale: Float): Result<Unit>

    /**
     * Set all animation scales to the same value.
     * @param scale Scale value (0.0 - 10.0)
     * @return Result.success if all set, Result.failure with error
     */
    suspend fun setAllAnimationScales(scale: Float): Result<Unit>
}
