package com.appcontrolx.executor

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream

class ShizukuExecutor : CommandExecutor {
    
    override fun execute(command: String): Result<String> {
        if (!Shizuku.pingBinder()) {
            return Result.failure(Exception("Shizuku not available"))
        }
        
        return try {
            // Use Shizuku's remote process to execute shell commands
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                val errorMsg = error.ifBlank { output }.trim()
                if (errorMsg.isNotBlank()) {
                    Result.failure(Exception(errorMsg))
                } else {
                    Result.failure(Exception("Command failed with exit code $exitCode"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun executeBatch(commands: List<String>): Result<Unit> {
        for (cmd in commands) {
            val result = execute(cmd)
            // Continue even if some commands fail (best effort)
        }
        return Result.success(Unit)
    }
    
    companion object {
        fun isAvailable(): Boolean {
            return try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }
        }
    }
}
