package com.appcontrolx.data.model

/**
 * Data class representing animation scale settings for the device.
 * Requirements: 7.3
 */
data class AnimationScale(
    val windowScale: Float,
    val transitionScale: Float,
    val animatorScale: Float
) {
    companion object {
        val DEFAULT = AnimationScale(1.0f, 1.0f, 1.0f)
        val OFF = AnimationScale(0f, 0f, 0f)

        /**
         * Preset animation scale values with display names.
         * Requirements: 7.3
         */
        val PRESETS = listOf(
            0f to "Off",
            0.5f to "0.5x",
            0.75f to "0.75x",
            1.0f to "1x (Default)",
            1.5f to "1.5x",
            2.0f to "2x"
        )

        /**
         * Minimum allowed animation scale value.
         */
        const val MIN_SCALE = 0.0f

        /**
         * Maximum allowed animation scale value.
         */
        const val MAX_SCALE = 10.0f

        /**
         * Creates an AnimationScale with all three scales set to the same value.
         */
        fun uniform(scale: Float): AnimationScale {
            val clampedScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
            return AnimationScale(clampedScale, clampedScale, clampedScale)
        }
    }

    /**
     * Returns true if all scales are set to the same value.
     */
    fun isUniform(): Boolean = windowScale == transitionScale && transitionScale == animatorScale

    /**
     * Returns the uniform scale value if all scales are the same, or null otherwise.
     */
    fun getUniformScale(): Float? = if (isUniform()) windowScale else null

    /**
     * Returns the preset name if this matches a preset, or "Custom" otherwise.
     */
    fun getPresetName(): String {
        val uniformScale = getUniformScale() ?: return "Custom"
        return PRESETS.find { it.first == uniformScale }?.second ?: "Custom"
    }
}
