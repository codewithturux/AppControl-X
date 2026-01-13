package com.appcontrolx.domain.executor

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CommandExecutor implementation using root (su) shell via libsu.
 * 
 * Security features:
 * - Command whitelist enforcement
 * - Dangerous pattern blocking
 * - Package name validation
 * 
 * Requirements: 11.1, 11.2, 11.3
 */
@Singleton
class RootExecutor @Inject constructor() : CommandExecutor {
    
    companion object {
        /**
         * Whitelist of allowed command prefixes for security.
         * Only commands starting with these prefixes will be executed.
         */
        val ALLOWED_COMMANDS = setOf(
            "pm disable",
            "pm enable",
            "pm uninstall",
            "pm clear",
            "pm list",
            "am force-stop",
            "appops set",
            "appops get",
            "cmd appops",
            "cmd activity",
            "getprop",
            "dumpsys activity",
            "dumpsys package",
            "dumpsys battery",
            "ps -A",
            "cat /proc",
            "cat /sys",
            // Added for display settings (Requirement 10.5-10.9)
            "settings put system",
            "settings get system",
            "settings delete system"
        )
        
        /**
         * Dangerous patterns that should never be executed.
         * Commands containing these patterns will be blocked regardless of prefix.
         */
        val BLOCKED_PATTERNS = listOf(
            "rm -rf /",
            "rm -rf /*",
            "format",
            "mkfs",
            "dd if=",
            "> /dev/",
            "reboot",
            "shutdown",
            "su -c",
            "chmod 777 /",
            "chown root /",
            "; rm",
            "&& rm",
            "| rm",
            "rm -rf",
            "`rm",
            "$(rm"
        )
    }

    
    /**
     * Validate if a command is allowed to be executed.
     * 
     * @param command The command to validate
     * @return true if command is allowed, false otherwise
     */
    fun isCommandAllowed(command: String): Boolean {
        val trimmed = command.trim().lowercase()
        
        // Check for blocked patterns first (highest priority)
        if (BLOCKED_PATTERNS.any { trimmed.contains(it.lowercase()) }) {
            return false
        }
        
        // Check if command starts with an allowed prefix
        return ALLOWED_COMMANDS.any { trimmed.startsWith(it.lowercase()) }
    }
    
    /**
     * Validate command and throw SecurityException if not allowed.
     * 
     * @param command The command to validate
     * @throws SecurityException if command is not allowed
     */
    private fun validateCommand(command: String) {
        if (!isCommandAllowed(command)) {
            throw SecurityException("Command not allowed: $command")
        }
    }
    
    override suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Security validation
            validateCommand(command)
            
            // Build su shell and execute command
            val shell = Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
                .build("su")
            
            if (!shell.isRoot) {
                return@withContext Result.failure(
                    RootNotAvailableException("Root access not available - su denied")
                )
            }
            
            val result = shell.newJob().add(command).exec()
            
            if (result.isSuccess) {
                Result.success(result.out.joinToString("\n"))
            } else {
                val error = result.err.joinToString("\n").ifEmpty { "Command failed" }
                Result.failure(CommandFailedException(error))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun executeBatch(commands: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Security validation for all commands
            val blockedCommands = commands.filter { !isCommandAllowed(it) }
            if (blockedCommands.isNotEmpty()) {
                return@withContext Result.failure(
                    SecurityException("Blocked commands: ${blockedCommands.joinToString()}")
                )
            }
            
            // Build su shell
            val shell = Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
                .build("su")
            
            if (!shell.isRoot) {
                return@withContext Result.failure(
                    RootNotAvailableException("Root access not available - su denied")
                )
            }
            
            val job = shell.newJob()
            commands.forEach { job.add(it) }
            val result = job.exec()
            
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(CommandFailedException(result.err.joinToString("\n")))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun isAvailable(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Exception thrown when root access is not available.
 */
class RootNotAvailableException(message: String) : Exception(message)

/**
 * Exception thrown when a command execution fails.
 */
class CommandFailedException(message: String) : Exception(message)
