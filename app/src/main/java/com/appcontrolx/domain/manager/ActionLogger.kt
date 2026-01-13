package com.appcontrolx.domain.manager

import android.content.Context
import com.appcontrolx.data.model.ActionLog
import com.appcontrolx.data.model.AppAction
import com.appcontrolx.data.model.AppState
import com.appcontrolx.data.model.StateSnapshot
import com.appcontrolx.domain.executor.CommandExecutor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logs actions performed on apps and supports rollback of the last batch action.
 * Persists action history and snapshots to JSON files.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Singleton
class ActionLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val executor: CommandExecutor?
) {
    
    companion object {
        private const val HISTORY_FILE = "action_history.json"
        private const val SNAPSHOT_FILE = "last_snapshot.json"
        private const val MAX_HISTORY_SIZE = 100
    }
    
    private val gson = Gson()
    private val mutex = Mutex()
    
    private val historyFile: File
        get() = File(context.filesDir, HISTORY_FILE)
    
    private val snapshotFile: File
        get() = File(context.filesDir, SNAPSHOT_FILE)
    
    /**
     * Log an action that was performed.
     * Maintains history of last 100 actions (oldest removed when limit exceeded).
     * 
     * @param action The action log to record
     */
    suspend fun logAction(action: ActionLog) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val history = loadHistoryInternal().toMutableList()
            
            // Add new action at the beginning (most recent first)
            history.add(0, action)
            
            // Trim to max size (remove oldest entries)
            while (history.size > MAX_HISTORY_SIZE) {
                history.removeAt(history.size - 1)
            }
            
            saveHistoryInternal(history)
        }
    }
    
    /**
     * Get action history in reverse chronological order (most recent first).
     * View only - no modification allowed.
     * 
     * @return List of action logs, most recent first
     */
    suspend fun getActionHistory(): List<ActionLog> = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadHistoryInternal()
        }
    }

    
    /**
     * Save a state snapshot before a batch action for potential rollback.
     * Only one snapshot is kept at a time (the most recent).
     * 
     * @param packages List of packages to snapshot
     * @return StateSnapshot if saved successfully, null if failed
     */
    suspend fun saveSnapshot(packages: List<String>): StateSnapshot? = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val states = packages.mapNotNull { packageName ->
                    captureAppState(packageName)
                }
                
                if (states.isEmpty()) return@withContext null
                
                val snapshot = StateSnapshot(states = states)
                saveSnapshotInternal(snapshot)
                snapshot
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get the last saved snapshot.
     * 
     * @return StateSnapshot if exists, null otherwise
     */
    suspend fun getLastSnapshot(): StateSnapshot? = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadSnapshotInternal()
        }
    }
    
    /**
     * Rollback the last batch action using the saved snapshot.
     * Only works if a snapshot exists and executor is available.
     * 
     * @return Result.success if rollback succeeded, Result.failure with error
     */
    suspend fun rollbackLastAction(): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val snapshot = loadSnapshotInternal()
                ?: return@withContext Result.failure(
                    NoSnapshotException("No snapshot available for rollback")
                )
            
            val exec = executor
                ?: return@withContext Result.failure(
                    ExecutorNotAvailableException("Executor not available for rollback")
                )
            
            try {
                // Restore each app state
                for (state in snapshot.states) {
                    restoreAppState(exec, state)
                }
                
                // Clear snapshot after successful rollback
                clearSnapshotInternal()
                
                // Log the rollback action
                val rollbackLog = ActionLog(
                    action = AppAction.UNFREEZE, // Generic action for rollback
                    packages = snapshot.states.map { it.packageName },
                    success = true,
                    errorMessage = "Rollback from snapshot ${snapshot.id}"
                )
                val history = loadHistoryInternal().toMutableList()
                history.add(0, rollbackLog)
                while (history.size > MAX_HISTORY_SIZE) {
                    history.removeAt(history.size - 1)
                }
                saveHistoryInternal(history)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if a rollback is available (snapshot exists).
     */
    suspend fun hasRollbackAvailable(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            snapshotFile.exists() && snapshotFile.length() > 0
        }
    }
    
    /**
     * Clear all action history.
     */
    suspend fun clearHistory() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (historyFile.exists()) {
                historyFile.delete()
            }
        }
    }
    
    /**
     * Clear the saved snapshot.
     */
    suspend fun clearSnapshot() = mutex.withLock {
        withContext(Dispatchers.IO) {
            clearSnapshotInternal()
        }
    }
    
    // ========== Internal methods (no locking) ==========
    
    private fun loadHistoryInternal(): List<ActionLog> {
        return try {
            if (!historyFile.exists()) return emptyList()
            val json = historyFile.readText()
            val type = object : TypeToken<List<ActionLog>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveHistoryInternal(history: List<ActionLog>) {
        try {
            val json = gson.toJson(history)
            historyFile.writeText(json)
        } catch (e: Exception) {
            // Silently fail - logging should not crash the app
        }
    }
    
    private fun loadSnapshotInternal(): StateSnapshot? {
        return try {
            if (!snapshotFile.exists()) return null
            val json = snapshotFile.readText()
            gson.fromJson(json, StateSnapshot::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun saveSnapshotInternal(snapshot: StateSnapshot) {
        try {
            val json = gson.toJson(snapshot)
            snapshotFile.writeText(json)
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    private fun clearSnapshotInternal() {
        if (snapshotFile.exists()) {
            snapshotFile.delete()
        }
    }
    
    /**
     * Capture current state of an app for snapshot.
     */
    private suspend fun captureAppState(packageName: String): AppState? {
        val exec = executor ?: return null
        
        return try {
            // Get enabled state
            val enabledResult = exec.execute("pm list packages -e | grep $packageName")
            val isEnabled = enabledResult.isSuccess && 
                enabledResult.getOrNull()?.contains(packageName) == true
            
            // Get background restriction state (simplified)
            val runInBgResult = exec.execute("appops get $packageName RUN_IN_BACKGROUND")
            val runInBg = runInBgResult.getOrNull()?.trim() ?: "default"
            
            val wakeLockResult = exec.execute("appops get $packageName WAKE_LOCK")
            val wakeLock = wakeLockResult.getOrNull()?.trim() ?: "default"
            
            AppState(
                packageName = packageName,
                isEnabled = isEnabled,
                runInBackground = runInBg,
                wakeLock = wakeLock
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Restore an app to its previous state.
     */
    private suspend fun restoreAppState(exec: CommandExecutor, state: AppState) {
        // Restore enabled state
        if (state.isEnabled) {
            exec.execute("pm enable ${state.packageName}")
        } else {
            exec.execute("pm disable-user --user 0 ${state.packageName}")
        }
        
        // Restore background state (simplified - just restore RUN_IN_BACKGROUND)
        val bgMode = when {
            state.runInBackground.contains("ignore", ignoreCase = true) -> "ignore"
            state.runInBackground.contains("allow", ignoreCase = true) -> "allow"
            else -> "default"
        }
        exec.execute("appops set ${state.packageName} RUN_IN_BACKGROUND $bgMode")
    }
}

/**
 * Exception thrown when no snapshot is available for rollback.
 */
class NoSnapshotException(message: String) : Exception(message)

/**
 * Exception thrown when executor is not available.
 */
class ExecutorNotAvailableException(message: String) : Exception(message)
