package com.appcontrolx.ui.components

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.appcontrolx.R
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.databinding.DialogModeLossBinding
import com.appcontrolx.domain.manager.ModeLossAction
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog shown when execution mode (Root/Shizuku) is lost.
 * Provides options to retry, switch mode, or continue in view-only mode.
 * 
 * Requirements: 10.1.4, 10.1.5, 10.1.6, 10.1.7
 */
class ModeLossDialog : DialogFragment() {

    private var _binding: DialogModeLossBinding? = null
    private val binding get() = _binding!!

    private var previousMode: ExecutionMode = ExecutionMode.None
    private var reason: String = ""
    private var onActionSelected: ((ModeLossAction) -> Unit)? = null
    private var onDismissed: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogModeLossBinding.inflate(layoutInflater)
        
        // Restore arguments
        arguments?.let { args ->
            previousMode = when (args.getString(ARG_PREVIOUS_MODE)) {
                MODE_ROOT -> ExecutionMode.Root
                MODE_SHIZUKU -> ExecutionMode.Shizuku
                else -> ExecutionMode.None
            }
            reason = args.getString(ARG_REASON, "")
        }
        
        setupUI()
        setupClickListeners()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    private fun setupUI() {
        // Set title based on previous mode
        binding.textTitle.text = when (previousMode) {
            ExecutionMode.Root -> getString(R.string.mode_loss_title_root)
            ExecutionMode.Shizuku -> getString(R.string.mode_loss_title_shizuku)
            ExecutionMode.None -> getString(R.string.warning_title)
        }
        
        // Set message - use provided reason or default message
        binding.textMessage.text = reason.ifEmpty {
            when (previousMode) {
                ExecutionMode.Root -> getString(R.string.mode_loss_message_root)
                ExecutionMode.Shizuku -> getString(R.string.mode_loss_message_shizuku_stopped)
                ExecutionMode.None -> getString(R.string.mode_loss_message_generic, previousMode.displayName)
            }
        }
    }

    private fun setupClickListeners() {
        // Retry button - Requirements 10.1.5
        binding.btnRetry.setOnClickListener {
            onActionSelected?.invoke(ModeLossAction.RETRY)
            dismiss()
        }
        
        // Switch Mode button - Requirements 10.1.6
        binding.btnSwitchMode.setOnClickListener {
            onActionSelected?.invoke(ModeLossAction.SWITCH_MODE)
            dismiss()
        }
        
        // Continue View-Only button - Requirements 10.1.7
        binding.btnViewOnly.setOnClickListener {
            onActionSelected?.invoke(ModeLossAction.CONTINUE_VIEW_ONLY)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissed?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Set callback for when user selects an action.
     */
    fun setOnActionSelectedListener(listener: (ModeLossAction) -> Unit) {
        onActionSelected = listener
    }
    
    /**
     * Set callback for when dialog is dismissed.
     */
    fun setOnDismissedListener(listener: () -> Unit) {
        onDismissed = listener
    }

    companion object {
        const val TAG = "ModeLossDialog"
        
        private const val ARG_PREVIOUS_MODE = "previous_mode"
        private const val ARG_REASON = "reason"
        
        private const val MODE_ROOT = "root"
        private const val MODE_SHIZUKU = "shizuku"
        private const val MODE_NONE = "none"

        /**
         * Create a new instance of ModeLossDialog.
         * 
         * @param previousMode The execution mode that was lost
         * @param reason The reason why the mode was lost
         * @return New ModeLossDialog instance
         */
        fun newInstance(
            previousMode: ExecutionMode,
            reason: String
        ): ModeLossDialog {
            return ModeLossDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PREVIOUS_MODE, when (previousMode) {
                        ExecutionMode.Root -> MODE_ROOT
                        ExecutionMode.Shizuku -> MODE_SHIZUKU
                        ExecutionMode.None -> MODE_NONE
                    })
                    putString(ARG_REASON, reason)
                }
            }
        }
    }
}
