package com.appcontrolx.ui.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.appcontrolx.R
import com.appcontrolx.data.model.BatchAction
import com.appcontrolx.databinding.BottomSheetBatchProgressSimpleBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog that shows progress during batch operations.
 * Displays current progress (X/Y), current app being processed, and cancel option.
 * 
 * This dialog observes BatchExecutionState.Executing from the ViewModel
 * and updates the UI accordingly.
 * 
 * Requirements: 2.2
 */
class BatchProgressDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBatchProgressSimpleBinding? = null
    private val binding get() = _binding!!

    private var action: BatchAction = BatchAction.FORCE_STOP
    private var totalCount: Int = 0
    
    /** Callback invoked when user cancels the operation */
    var onCancelled: (() -> Unit)? = null

    companion object {
        const val TAG = "BatchProgressDialog"
        private const val ARG_ACTION = "action"
        private const val ARG_TOTAL_COUNT = "total_count"

        /**
         * Create a new instance of BatchProgressDialog.
         * @param action The batch action being executed
         * @param totalCount Total number of apps to process
         */
        fun newInstance(action: BatchAction, totalCount: Int): BatchProgressDialog {
            return BatchProgressDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACTION, action.name)
                    putInt(ARG_TOTAL_COUNT, totalCount)
                }
            }
        }

        /**
         * Show the progress dialog.
         * @param fragmentManager FragmentManager to show the dialog
         * @param action The batch action being executed
         * @param totalCount Total number of apps to process
         * @param onCancelled Callback for cancellation
         */
        fun show(
            fragmentManager: FragmentManager,
            action: BatchAction,
            totalCount: Int,
            onCancelled: () -> Unit
        ): BatchProgressDialog {
            val fragment = newInstance(action, totalCount).apply {
                this.onCancelled = onCancelled
            }
            fragment.show(fragmentManager, TAG)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Restore arguments
        arguments?.let { args ->
            action = BatchAction.valueOf(args.getString(ARG_ACTION, BatchAction.FORCE_STOP.name))
            totalCount = args.getInt(ARG_TOTAL_COUNT, 0)
        }
        
        // Prevent dismissal by tapping outside or back button during execution
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBatchProgressSimpleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupCancelButton()
    }

    /**
     * Setup initial UI state.
     */
    private fun setupUI() {
        // Set title based on action
        binding.tvTitle.text = getString(R.string.batch_executing, getActionDisplayName(action))
        
        // Setup progress bar
        binding.progressBar.max = totalCount
        binding.progressBar.progress = 0
        
        // Initial progress text
        binding.tvProgress.text = getString(R.string.batch_progress, 0, totalCount)
        
        // Initial current app
        binding.tvCurrentApp.text = getString(R.string.batch_preparing)
    }

    /**
     * Setup cancel button click listener.
     */
    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            onCancelled?.invoke()
            dismiss()
        }
    }

    /**
     * Update progress display.
     * Called from AppListFragment when BatchExecutionState.Executing changes.
     * 
     * @param currentIndex Current progress index (1-based)
     * @param currentPackageName Package name of app currently being processed
     */
    fun updateProgress(currentIndex: Int, currentPackageName: String) {
        if (_binding == null) return
        
        binding.progressBar.progress = currentIndex
        binding.tvProgress.text = getString(R.string.batch_progress, currentIndex, totalCount)
        binding.tvCurrentApp.text = currentPackageName
    }

    /**
     * Get display name for batch action.
     */
    private fun getActionDisplayName(action: BatchAction): String {
        return when (action) {
            BatchAction.FREEZE -> getString(R.string.action_freeze)
            BatchAction.UNFREEZE -> getString(R.string.action_unfreeze)
            BatchAction.FORCE_STOP -> getString(R.string.action_force_stop)
            BatchAction.RESTRICT_BACKGROUND -> getString(R.string.action_restrict_bg)
            BatchAction.ALLOW_BACKGROUND -> getString(R.string.action_allow_bg)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
