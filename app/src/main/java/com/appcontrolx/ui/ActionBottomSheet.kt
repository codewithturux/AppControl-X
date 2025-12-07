package com.appcontrolx.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.appcontrolx.R
import com.appcontrolx.databinding.BottomSheetActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ActionBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetActionsBinding? = null
    private val binding get() = _binding!!
    
    var onActionSelected: ((Action) -> Unit)? = null
    
    enum class Action {
        FREEZE, UNFREEZE, UNINSTALL, FORCE_STOP,
        RESTRICT_BACKGROUND, ALLOW_BACKGROUND,
        CLEAR_CACHE, CLEAR_DATA
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetActionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val selectedCount = arguments?.getInt(ARG_SELECTED_COUNT, 0) ?: 0
        binding.tvTitle.text = getString(R.string.action_title, selectedCount)
        
        binding.btnFreeze.setOnClickListener { 
            onActionSelected?.invoke(Action.FREEZE)
            dismiss()
        }
        
        binding.btnUnfreeze.setOnClickListener { 
            onActionSelected?.invoke(Action.UNFREEZE)
            dismiss()
        }
        
        binding.btnUninstall.setOnClickListener { 
            onActionSelected?.invoke(Action.UNINSTALL)
            dismiss()
        }
        
        binding.btnForceStop.setOnClickListener { 
            onActionSelected?.invoke(Action.FORCE_STOP)
            dismiss()
        }
        
        binding.btnRestrictBg.setOnClickListener { 
            onActionSelected?.invoke(Action.RESTRICT_BACKGROUND)
            dismiss()
        }
        
        binding.btnAllowBg.setOnClickListener { 
            onActionSelected?.invoke(Action.ALLOW_BACKGROUND)
            dismiss()
        }
        
        binding.btnClearCache.setOnClickListener { 
            onActionSelected?.invoke(Action.CLEAR_CACHE)
            dismiss()
        }
        
        binding.btnClearData.setOnClickListener { 
            onActionSelected?.invoke(Action.CLEAR_DATA)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val TAG = "ActionBottomSheet"
        private const val ARG_SELECTED_COUNT = "selected_count"
        
        fun newInstance(selectedCount: Int): ActionBottomSheet {
            return ActionBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SELECTED_COUNT, selectedCount)
                }
            }
        }
    }
}
