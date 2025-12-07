package com.appcontrolx.rollback

import android.content.Context
import android.util.Log
import com.appcontrolx.executor.CommandExecutor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

class RollbackManager(
    private val context: Context,
    private val executor: CommandExecutor? = null
) {
    private val gson = Gson()
    private val snapshotDir = File(context.filesDir, "snapshots").apply { mkdirs() }
    private val snapshotFile = File(snapshotDir, "rollback_snapshot.json")
    private val historyFile = File(snapshotDir, "action_history.json")
    
    // Secondary constructor for read-only access (no executor needed)
    constructor(context: Context) : this(context, null)
    
    companion object {
        private const val TAG = "RollbackManager"
    }
    
    fun saveSnapshot(packages: List<String>): StateSnapshot? {
        val exec = executor ?: return null
        val states = packages.map { pkg ->
            val bgStatus = exec.execute("appops get $pkg RUN_IN_BACKGROUND")
            val wlStatus = exec.execute("appops get $pkg WAKE_LOCK")
            val enabledStatus = exec.execute("pm list packages -e | grep $pkg")
            
            AppState(
                packageName = pkg,
                runInBackground = parseAppOpsValue(bgStatus.getOrDefault("")),
                wakeLock = parseAppOpsValue(wlStatus.getOrDefault("")),
                isEnabled = enabledStatus.getOrDefault("").contains(pkg),
                timestamp = System.currentTimeMillis()
            )
        }
        
        val snapshot = StateSnapshot(
            id = UUID.randomUUID().toString(),
            states = states,
            createdAt = System.currentTimeMillis()
        )
        
        snapshotFile.writeText(gson.toJson(snapshot))
        return snapshot
    }
    
    fun getLastSnapshot(): StateSnapshot? {
        if (!snapshotFile.exists()) return null
        return try {
            gson.fromJson(snapshotFile.readText(), StateSnapshot::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun rollback(): Result<Unit> {
        val exec = executor ?: return Result.failure(Exception("No executor available"))
        val snapshot = getLastSnapshot() 
            ?: return Result.failure(Exception("No snapshot found"))
        
        val commands = snapshot.states.flatMap { state ->
            mutableListOf<String>().apply {
                add("appops set ${state.packageName} RUN_IN_BACKGROUND ${state.runInBackground}")
                add("appops set ${state.packageName} WAKE_LOCK ${state.wakeLock}")
                if (state.isEnabled) {
                    add("pm enable ${state.packageName}")
                }
            }
        }
        
        return exec.executeBatch(commands)
    }
    
    fun logAction(action: ActionLog) {
        try {
            val history = getActionHistory().toMutableList()
            history.add(0, action) // Add to beginning
            
            // Keep only last 100 actions
            val trimmed = history.take(100)
            val json = gson.toJson(trimmed)
            historyFile.writeText(json)
            Log.d(TAG, "Action logged: ${action.action}, total: ${trimmed.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log action: ${action.action}", e)
        }
    }
    
    fun getActionHistory(): List<ActionLog> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val content = historyFile.readText()
            val type = object : TypeToken<List<ActionLog>>() {}.type
            gson.fromJson<List<ActionLog>>(content, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read history", e)
            emptyList()
        }
    }
    
    fun clearHistory() {
        historyFile.delete()
        snapshotFile.delete()
    }
    
    // Alias methods for UI
    fun getActionLogs(): List<ActionLog> = getActionHistory()
    fun clearLogs() = clearHistory()
    fun getLogCount(): Int = getActionHistory().size
    
    private fun parseAppOpsValue(output: String): String {
        return when {
            output.contains("ignore") -> "ignore"
            output.contains("deny") -> "deny"
            else -> "allow"
        }
    }
}

data class StateSnapshot(
    val id: String,
    val states: List<AppState>,
    val createdAt: Long
)

data class AppState(
    val packageName: String,
    val runInBackground: String,
    val wakeLock: String,
    val isEnabled: Boolean,
    val timestamp: Long
)

data class ActionLog(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val packages: List<String>,
    val success: Boolean,
    val message: String?,
    val timestamp: Long = System.currentTimeMillis()
)
