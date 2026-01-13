package com.appcontrolx.domain.executor

/**
 * Interface for executing shell commands.
 * Implementations include RootExecutor and ShizukuExecutor.
 * 
 * Requirements: 11.1
 */
interface CommandExecutor {
    
    /**
     * Execute a single shell command.
     * 
     * @param command The shell command to execute
     * @return Result containing command output on success, or exception on failure
     */
    suspend fun execute(command: String): Result<String>
    
    /**
     * Execute multiple shell commands in sequence.
     * 
     * @param commands List of shell commands to execute
     * @return Result.success if all commands succeed, Result.failure otherwise
     */
    suspend fun executeBatch(commands: List<String>): Result<Unit>
    
    /**
     * Check if this executor is currently available and ready to execute commands.
     * 
     * @return true if executor can execute commands, false otherwise
     */
    fun isAvailable(): Boolean
}
