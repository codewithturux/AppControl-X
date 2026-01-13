package com.appcontrolx.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.databinding.FragmentSetupCompleteBinding
import com.appcontrolx.domain.executor.PermissionBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Setup complete fragment for the setup wizard.
 * 
 * Shows setup completion status and selected mode.
 * 
 * Requirements: 0.7
 */
@AndroidEntryPoint
class SetupCompleteFragment : Fragment() {
    
    private var _binding: FragmentSetupCompleteBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var permissionBridge: PermissionBridge
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        updateModeDisplay()
        
        binding.btnGoDashboard.setOnClickListener {
            // Complete setup and navigate to Dashboard
            (activity as? SetupActivity)?.completeSetup()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateModeDisplay()
    }
    
    private fun updateModeDisplay() {
        val mode = permissionBridge.mode
        binding.tvMode.text = mode.displayName
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
