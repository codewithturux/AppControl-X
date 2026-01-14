package com.appcontrolx.ui.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appcontrolx.R
import com.appcontrolx.data.model.ActionStatus
import com.appcontrolx.data.model.AppActionResult
import com.appcontrolx.data.model.BatchExecutionResult
import com.appcontrolx.databinding.BottomSheetBatchResultBinding
import com.appcontrolx.databinding.ItemBatchAppBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog that shows the results of a batch operation.
 * Displays success/failure/skipped counts with expandable details.
 * 
 * Requirements: 3.1, 3.2, 3.3
 */
class BatchResultDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBatchResultBinding? = null
    private val binding get() = _binding!!

    private var result: BatchExecutionResult? = null
    private var isDetailsExpanded = false
    
    /** Callback invoked when user dismisses the dialog */
    var onDismissed: (() -> Unit)? = null

    companion object {
        const val TAG = "BatchResultDialog"
        private const val ARG_SUCCESS_COUNT = "success_count"
        private const val ARG_FAILURE_COUNT = "failure_count"
        private const val ARG_SKIPPED_COUNT = "skipped_count"

        /**
         * Create a new instance of BatchResultDialog.
         * @param result The batch execution result to display
         */
        fun newInstance(result: BatchExecutionResult): BatchResultDialog {
            return BatchResultDialog().apply {
                this.result = result
                arguments = Bundle().apply {
                    putInt(ARG_SUCCESS_COUNT, result.successCount)
                    putInt(ARG_FAILURE_COUNT, result.failureCount)
                    putInt(ARG_SKIPPED_COUNT, result.skippedCount)
                }
            }
        }

        /**
         * Show the result dialog.
         * @param fragmentManager FragmentManager to show the dialog
         * @param result The batch execution result
         * @param onDismissed Callback for when dialog is dismissed
         */
        fun show(
            fragmentManager: FragmentManager,
            result: BatchExecutionResult,
            onDismissed: () -> Unit
        ): BatchResultDialog {
            val fragment = newInstance(result).apply {
                this.onDismissed = onDismissed
            }
            fragment.show(fragmentManager, TAG)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Restore counts from arguments if result is null (e.g., after config change)
        if (result == null) {
            arguments?.let { args ->
                // We can only restore counts, not full results after config change
                // This is acceptable as the dialog is typically short-lived
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBatchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupButtons()
        setupDetailsRecyclerView()
    }

    /**
     * Setup initial UI state with result counts.
     */
    private fun setupUI() {
        val successCount = result?.successCount ?: arguments?.getInt(ARG_SUCCESS_COUNT, 0) ?: 0
        val failureCount = result?.failureCount ?: arguments?.getInt(ARG_FAILURE_COUNT, 0) ?: 0
        val skippedCount = result?.skippedCount ?: arguments?.getInt(ARG_SKIPPED_COUNT, 0) ?: 0

        // Success count
        binding.tvSuccessCount.text = getString(R.string.batch_result_success, successCount)
        
        // Failure count - hide if zero
        binding.tvFailureCount.text = getString(R.string.batch_result_failed, failureCount)
        binding.layoutFailure.isVisible = failureCount > 0
        
        // Skipped count - hide if zero
        binding.tvSkippedCount.text = getString(R.string.batch_result_skipped, skippedCount)
        binding.layoutSkipped.isVisible = skippedCount > 0
        
        // Hide view details button if no results to show
        val hasDetails = result?.results?.isNotEmpty() == true
        binding.btnViewDetails.isVisible = hasDetails
    }

    /**
     * Setup button click listeners.
     */
    private fun setupButtons() {
        binding.btnViewDetails.setOnClickListener {
            toggleDetails()
        }
        
        binding.btnDone.setOnClickListener {
            onDismissed?.invoke()
            dismiss()
        }
    }

    /**
     * Toggle the details section visibility.
     */
    private fun toggleDetails() {
        isDetailsExpanded = !isDetailsExpanded
        
        binding.cardDetails.isVisible = isDetailsExpanded
        binding.btnViewDetails.text = if (isDetailsExpanded) {
            getString(R.string.batch_result_hide_details)
        } else {
            getString(R.string.batch_result_view_details)
        }
    }

    /**
     * Setup the RecyclerView for showing detailed results.
     */
    private fun setupDetailsRecyclerView() {
        val results = result?.results ?: return
        
        binding.rvDetails.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ResultDetailsAdapter(results)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for displaying individual app results in the details section.
     */
    private inner class ResultDetailsAdapter(
        private val results: List<AppActionResult>
    ) : RecyclerView.Adapter<ResultDetailsAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemBatchAppBinding) : 
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemBatchAppBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = results[position]
            
            holder.binding.tvAppName.text = item.appName
            
            // Set status text and color based on result
            val (statusText, statusColor) = when (item.status) {
                ActionStatus.SUCCESS -> {
                    getString(R.string.log_status_success) to 
                        requireContext().getColor(R.color.status_positive)
                }
                ActionStatus.FAILED -> {
                    (item.errorMessage ?: getString(R.string.log_status_failed)) to 
                        requireContext().getColor(R.color.status_negative)
                }
                ActionStatus.SKIPPED -> {
                    getString(R.string.batch_result_skipped_status) to 
                        requireContext().getColor(R.color.status_warning)
                }
            }
            
            holder.binding.tvStatus.text = statusText
            holder.binding.tvStatus.setTextColor(statusColor)
        }

        override fun getItemCount(): Int = results.size
    }
}
