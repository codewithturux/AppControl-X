package com.appcontrolx.data.model

/**
 * Actions available for batch operations on multiple apps.
 * Used by ActionBottomSheet for batch execution.
 */
enum class BatchAction {
    FREEZE,
    UNFREEZE,
    FORCE_STOP,
    RESTRICT_BACKGROUND,
    ALLOW_BACKGROUND;

    /**
     * Human-readable display name for UI
     */
    val displayName: String
        get() = when (this) {
            FREEZE -> "Freeze"
            UNFREEZE -> "Unfreeze"
            FORCE_STOP -> "Force Stop"
            RESTRICT_BACKGROUND -> "Restrict Background"
            ALLOW_BACKGROUND -> "Allow Background"
        }

    /**
     * Convert to AppAction for execution via AppControlManager
     */
    fun toAppAction(): AppAction = when (this) {
        FREEZE -> AppAction.FREEZE
        UNFREEZE -> AppAction.UNFREEZE
        FORCE_STOP -> AppAction.FORCE_STOP
        RESTRICT_BACKGROUND -> AppAction.RESTRICT_BACKGROUND
        ALLOW_BACKGROUND -> AppAction.ALLOW_BACKGROUND
    }
}
