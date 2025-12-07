package com.appcontrolx.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcontrolx.R
import com.appcontrolx.databinding.BottomSheetActionLogBinding
import com.appcontrolx.executor.RootExecutor
import com.appcontrolx.model.ExecutionMode
import com.appcontrolx.rollback.ActionLog
import com.appcontrolx.rollback.RollbackManager
import com.appcontrolx.service.BatteryPolicyManager
import com.appcontrolx.service.PermissionBridge
import com.appcontrolx.ui.adapter.ActionLogAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionLogBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetActionLogBinding? = null
    private val binding get() = _binding
    
    private lateinit var adapter: ActionLogAdapter
    private var rollbackManager: RollbackManager? = null
    private var policyManager: BatteryPolicyManager? = null
    
    var onLogCleared: (() -> Unit)? = null
    
    companion object {
        const val TAG = "ActionLogBottomSheet"
        
        fun newInstance() = ActionLogBottomSheet()
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetActionLogBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupExecutor()
        setupRecyclerView()
        setupButtons()
        loadLogs()
    }
    
    private fun setupExecutor() {
        val mode = PermissionBridge(requireContext()).detectMode()
        if (mode is ExecutionMode.Root) {
            val executor = RootExecutor()
            policyManager = BatteryPolicyManager(executor)
            rollbackManager = RollbackManager(requireContext(), executor)
        }
    }
    
    private fun setupRecyclerView() {
        val b = binding ?: return
        adapter = ActionLogAdapter(
            onRollbackClick = { log ->
                showRollbackConfirmation(log)
            }
        )
        b.recyclerView.layoutManager = LinearLayoutManager(context)
        b.recyclerView.adapter = adapter
    }
    
    private fun setupButtons() {
        val b = binding ?: return
        b.btnClose.setOnClickListener { dismiss() }
        b.btnClearAll.setOnClickListener { showClearConfirmation() }
    }
    
    private fun loadLogs() {
        val b = binding ?: return
        val rm = rollbackManager
        
        if (rm == null) {
            b.emptyState.visibility = View.VISIBLE
            b.recyclerView.visibility = View.GONE
            b.tvEmptyMessage.text = getString(R.string.log_no_mode)
            return
        }
        
        lifecycleScope.launch {
            val logs = withContext(Dispatchers.IO) {
                rm.getActionLogs()
            }
            
            if (logs.isEmpty()) {
                b.emptyState.visibility = View.VISIBLE
                b.recyclerView.visibility = View.GONE
                b.tvEmptyMessage.text = getString(R.string.log_empty)
            } else {
                b.emptyState.visibility = View.GONE
                b.recyclerView.visibility = View.VISIBLE
                adapter.submitList(logs.sortedByDescending { it.timestamp })
            }
        }
    }
    
    private fun showRollbackConfirmation(log: ActionLog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.log_rollback_title)
            .setMessage(getString(R.string.log_rollback_message, log.action, log.packages.size))
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                performRollback(log)
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun performRollback(log: ActionLog) {
        val b = binding ?: return
        val pm = policyManager ?: return
        val rm = rollbackManager ?: return
        
        lifecycleScope.launch {
            b.progressBar.visibility = View.VISIBLE
            
            try {
                val results = withContext(Dispatchers.IO) {
                    log.packages.map { pkg ->
                        val result = when (log.action) {
                            "FREEZE" -> pm.unfreezeApp(pkg)
                            "UNFREEZE" -> pm.freezeApp(pkg)
                            "RESTRICT_BACKGROUND" -> pm.allowBackground(pkg)
                            "ALLOW_BACKGROUND" -> pm.restrictBackground(pkg)
                            else -> Result.failure(Exception("Cannot rollback ${log.action}"))
                        }
                        pkg to result
                    }
                }
                
                val failCount = results.count { it.second.isFailure }
                val success = failCount == 0
                
                // Log the rollback action
                val rollbackActionName = "ROLLBACK_" + log.action
                withContext(Dispatchers.IO) {
                    rm.logAction(ActionLog(
                        action = rollbackActionName,
                        packages = log.packages,
                        success = success,
                        message = if (success) "Rolled back from ${log.action}" else "$failCount failed"
                    ))
                }
                
                if (success) {
                    Toast.makeText(context, R.string.log_rollback_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.log_rollback_partial, failCount), Toast.LENGTH_SHORT).show()
                }
                
                // Refresh the log list
                loadLogs()
                
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.log_rollback_failed, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                b.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showClearConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.log_clear_title)
            .setMessage(R.string.log_clear_message)
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                clearLogs()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun clearLogs() {
        rollbackManager?.clearLogs()
        loadLogs()
        onLogCleared?.invoke()
        Toast.makeText(context, R.string.log_cleared, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
