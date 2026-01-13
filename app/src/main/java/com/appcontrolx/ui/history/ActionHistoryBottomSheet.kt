package com.appcontrolx.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcontrolx.R
import com.appcontrolx.data.model.ActionLog
import com.appcontrolx.databinding.BottomSheetActionLogBinding
import com.appcontrolx.domain.manager.ActionLogger
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Bottom sheet displaying action history in reverse chronological order.
 * Shows rollback button for the most recent action when a snapshot is available.
 * 
 * Requirements: 7.3, 7.4, 7.6 - Display actions in reverse chronological order with rollback for last action
 */
@AndroidEntryPoint
class ActionHistoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetActionLogBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var actionLogger: ActionLogger

    private lateinit var adapter: ActionHistoryAdapter

    /** Callback when logs are cleared */
    var onLogCleared: (() -> Unit)? = null
    
    /** Callback when rollback is executed */
    var onRollbackExecuted: (() -> Unit)? = null

    companion object {
        const val TAG = "ActionHistoryBottomSheet"

        fun newInstance() = ActionHistoryBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetActionLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadLogs()
    }

    /**
     * Setup RecyclerView with adapter.
     */
    private fun setupRecyclerView() {
        adapter = ActionHistoryAdapter { log ->
            showRollbackConfirmation(log)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    /**
     * Setup header buttons.
     */
    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClearAll.setOnClickListener { showClearConfirmation() }
    }

    /**
     * Load action logs from ActionLogger.
     * Displays in reverse chronological order (most recent first).
     * Also checks if rollback is available for the most recent action.
     */
    private fun loadLogs() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            val logs = withContext(Dispatchers.IO) {
                actionLogger.getActionHistory()
            }
            
            // Check if rollback is available (snapshot exists)
            // Requirements: 7.3, 7.6 - Only show rollback if hasRollbackAvailable()
            val rollbackAvailable = withContext(Dispatchers.IO) {
                actionLogger.hasRollbackAvailable()
            }

            binding.progressBar.visibility = View.GONE

            if (logs.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.tvEmptyMessage.text = getString(R.string.log_empty)
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                // Set rollback availability before submitting list
                adapter.rollbackAvailable = rollbackAvailable
                // Logs are already sorted by timestamp descending in ActionLogger
                adapter.submitList(logs)
            }
        }
    }
    
    /**
     * Show confirmation dialog before executing rollback.
     * Requirements: 7.3, 7.6 - Execute rollbackLastAction()
     */
    private fun showRollbackConfirmation(log: ActionLog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.log_rollback_title)
            .setMessage(getString(R.string.log_rollback_message, 
                formatActionName(log.action.name), 
                log.packages.size))
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                executeRollback()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    /**
     * Execute rollback of the last action.
     * Requirements: 7.3, 7.6 - Execute rollbackLastAction()
     */
    private fun executeRollback() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                actionLogger.rollbackLastAction()
            }
            
            binding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = {
                    Toast.makeText(context, R.string.log_rollback_success, Toast.LENGTH_SHORT).show()
                    onRollbackExecuted?.invoke()
                    // Reload logs to reflect the rollback action and update rollback availability
                    loadLogs()
                },
                onFailure = { error ->
                    Toast.makeText(
                        context, 
                        getString(R.string.log_rollback_failed, error.message), 
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    /**
     * Format action enum name to readable text.
     * e.g., RESTRICT_BACKGROUND -> Restrict Background
     */
    private fun formatActionName(name: String): String {
        return name.replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Show confirmation dialog before clearing all logs.
     */
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

    /**
     * Clear all action logs.
     */
    private fun clearLogs() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                actionLogger.clearHistory()
            }
            loadLogs()
            onLogCleared?.invoke()
            Toast.makeText(context, R.string.log_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
