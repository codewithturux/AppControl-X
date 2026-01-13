package com.appcontrolx.domain.executor

import com.appcontrolx.IShellService
import java.io.BufferedReader

/**
 * AIDL service implementation for executing shell commands via Shizuku.
 * This service runs in a separate process with Shizuku privileges.
 * 
 * Requirements: 11.4
 */
class ShellService : IShellService.Stub() {
    
    /**
     * Execute a shell command and return the output.
     * 
     * @param command The shell command to execute
     * @return Command output on success, or "ERROR:<message>" on failure
     */
    override fun exec(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                output.trim()
            } else {
                val errorMsg = error.ifBlank { output }.trim()
                "ERROR:${errorMsg.ifBlank { "Exit code $exitCode" }}"
            }
        } catch (e: Exception) {
            "ERROR:${e.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Execute a shell command and return the exit code.
     * 
     * @param command The shell command to execute
     * @return Exit code (0 for success), or -1 on exception
     */
    override fun execReturnCode(command: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            -1
        }
    }
}
