package com.appcontrolx.data.model

/**
 * Represents the execution mode for shell commands.
 * Priority: Root > Shizuku > None
 */
sealed class ExecutionMode {
    
    /**
     * Root mode - Full access via Magisk/KernelSU
     */
    data object Root : ExecutionMode()
    
    /**
     * Shizuku mode - No root needed, uses Shizuku API
     */
    data object Shizuku : ExecutionMode()
    
    /**
     * View-only mode - No shell access, browse apps only
     */
    data object None : ExecutionMode()
    
    /**
     * Human-readable name for display in UI
     */
    val displayName: String
        get() = when (this) {
            Root -> "Root"
            Shizuku -> "Shizuku"
            None -> "View Only"
        }
    
    /**
     * Whether this mode can execute shell commands/actions
     */
    val canExecuteActions: Boolean
        get() = this != None
}
