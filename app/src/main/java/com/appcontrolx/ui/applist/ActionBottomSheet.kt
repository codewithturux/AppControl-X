package com.appcontrolx.ui.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.appcontrolx.R
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.BatchAction
import com.appcontrolx.databinding.BottomSheetActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet for selecting batch actions on multiple selected apps.
 * Displays a grid of action buttons (Freeze, Unfreeze, Force Stop, etc.)
 * and handles action selection with confirmation for destructive actions.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */
class ActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetActionsBinding? = null
    private val binding get() = _binding!!

    private var selectedApps: List<AppInfo> = emptyList()
    
    /** Callback invoked when user selects an action */
    var onActionSelected: ((BatchAction, List<AppInfo>) -> Unit)? = null

    companion object {
        const val TAG = "ActionBottomSheet"
        private const val ARG_SELECTED_COUNT = "selected_count"
        private const val ARG_PACKAGE_NAMES = "package_names"
        private const val ARG_APP_NAMES = "app_names"

        /**
         * Create a new instance of ActionBottomSheet with selected apps.
         * @param selectedApps List of apps selected for batch operation
         */
        fun newInstance(selectedApps: List<AppInfo>): ActionBottomSheet {
            return ActionBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SELECTED_COUNT, selectedApps.size)
                    putStringArrayList(ARG_PACKAGE_NAMES, ArrayList(selectedApps.map { it.packageName }))
                    putStringArrayList(ARG_APP_NAMES, ArrayList(selectedApps.map { it.appName }))
                }
                this.selectedApps = selectedApps
            }
        }

        /**
         * Show the action bottom sheet.
         * @param fragmentManager FragmentManager to show the dialog
         * @param selectedApps List of apps selected for batch operation
         * @param onActionSelected Callback for action selection
         */
        fun show(
            fragmentManager: FragmentManager,
            selectedApps: List<AppInfo>,
            onActionSelected: (BatchAction, List<AppInfo>) -> Unit
        ) {
            val fragment = newInstance(selectedApps).apply {
                this.onActionSelected = onActionSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTitle()
        setupActionButtons()
        setupConfirmationButtons()
    }

    /**
     * Setup title showing selected app count.
     * Requirement: 1.2
     */
    private fun setupTitle() {
        val count = arguments?.getInt(ARG_SELECTED_COUNT, 0) ?: selectedApps.size
        binding.tvTitle.text = getString(R.string.action_title, count)
    }

    /**
     * Setup action button click listeners.
     * Each button triggers the corresponding BatchAction.
     * Requirements: 1.3, 1.4
     */
    private fun setupActionButtons() {
        // Force Stop - Common action
        binding.btnForceStop.setOnClickListener {
            selectAction(BatchAction.FORCE_STOP)
        }

        // Clear Cache - Common action (not in BatchAction, skip for now)
        binding.btnClearCache.setOnClickListener {
            // Clear cache is not part of BatchAction enum
            // This could be extended in the future
            dismiss()
        }

        // Clear Data - Common action (not in BatchAction, skip for now)
        binding.btnClearData.setOnClickListener {
            // Clear data is not part of BatchAction enum
            // This could be extended in the future
            dismiss()
        }

        // Uninstall - Danger zone (not in BatchAction, skip for now)
        binding.btnUninstall.setOnClickListener {
            // Uninstall is not part of BatchAction enum
            // This could be extended in the future
            dismiss()
        }

        // Freeze
        binding.btnFreeze.setOnClickListener {
            showConfirmation(BatchAction.FREEZE)
        }

        // Unfreeze
        binding.btnUnfreeze.setOnClickListener {
            selectAction(BatchAction.UNFREEZE)
        }

        // Restrict Background
        binding.btnRestrictBg.setOnClickListener {
            selectAction(BatchAction.RESTRICT_BACKGROUND)
        }

        // Allow Background
        binding.btnAllowBg.setOnClickListener {
            selectAction(BatchAction.ALLOW_BACKGROUND)
        }
    }

    /**
     * Setup confirmation dialog buttons.
     */
    private fun setupConfirmationButtons() {
        binding.btnNo.setOnClickListener {
            hideConfirmation()
        }
    }

    /**
     * Select an action and invoke callback.
     * For non-destructive actions, directly invoke callback.
     */
    private fun selectAction(action: BatchAction) {
        onActionSelected?.invoke(action, selectedApps)
        dismiss()
    }

    /**
     * Show confirmation dialog for destructive actions.
     * Requirement: Freeze requires confirmation
     */
    private fun showConfirmation(action: BatchAction) {
        binding.layoutActions.visibility = View.GONE
        binding.layoutConfirm.visibility = View.VISIBLE
        
        val message = when (action) {
            BatchAction.FREEZE -> getString(R.string.confirm_freeze)
            else -> getString(R.string.confirm_title)
        }
        binding.tvConfirmMessage.text = message
        
        binding.btnYes.setOnClickListener {
            selectAction(action)
        }
    }

    /**
     * Hide confirmation dialog and show actions again.
     */
    private fun hideConfirmation() {
        binding.layoutConfirm.visibility = View.GONE
        binding.layoutActions.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
