package com.appcontrolx.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.appcontrolx.R
import com.appcontrolx.databinding.DialogBatchProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BatchProgressDialog : DialogFragment() {
    
    private var _binding: DialogBatchProgressBinding? = null
    private val binding get() = _binding
    
    private var totalCount = 0
    private var actionName = ""
    private val logEntries = mutableListOf<String>()
    
    companion object {
        const val TAG = "BatchProgressDialog"
        
        fun newInstance(actionName: String, totalCount: Int): BatchProgressDialog {
            return BatchProgressDialog().apply {
                this.actionName = actionName
                this.totalCount = totalCount
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogBatchProgressBinding.inflate(layoutInflater)
        
        binding?.let { b ->
            b.tvAction.text = actionName.replace("_", " ")
            b.progressBar.max = totalCount
            b.progressBar.progress = 0
            b.tvProgress.text = getString(R.string.batch_progress, 0, totalCount)
            b.tvLog.text = getString(R.string.batch_preparing)
        }
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(_binding?.root)
            .setCancelable(false)
            .create()
    }
    
    fun addLogEntry(packageName: String, status: String, isSuccess: Boolean) {
        val statusIcon = if (isSuccess) "✓" else "✗"
        val entry = "$statusIcon $packageName → $status"
        logEntries.add(entry)
        
        binding?.let { b ->
            b.progressBar.progress = logEntries.size
            b.tvProgress.text = getString(R.string.batch_progress, logEntries.size, totalCount)
            b.tvLog.text = logEntries.takeLast(10).joinToString("\n")
            
            // Auto scroll to bottom
            b.scrollView.post {
                b.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }
    
    fun setCompleted(successCount: Int, failCount: Int) {
        binding?.let { b ->
            b.progressBar.progress = totalCount
            b.tvProgress.text = getString(R.string.batch_completed)
            
            val summary = if (failCount == 0) {
                getString(R.string.batch_all_success, successCount)
            } else {
                getString(R.string.batch_partial_success, successCount, failCount)
            }
            logEntries.add("\n$summary")
            b.tvLog.text = logEntries.joinToString("\n")
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
