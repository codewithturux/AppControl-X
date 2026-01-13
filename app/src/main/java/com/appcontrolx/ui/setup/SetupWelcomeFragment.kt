package com.appcontrolx.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appcontrolx.databinding.FragmentSetupWelcomeBinding

/**
 * Welcome screen fragment for the setup wizard.
 * 
 * Displays app introduction and features overview.
 * 
 * Requirements: 0.1, 0.2
 */
class SetupWelcomeFragment : Fragment() {
    
    private var _binding: FragmentSetupWelcomeBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnGetStarted.setOnClickListener {
            // Navigate to next step (Mode Selection)
            (activity as? SetupActivity)?.nextStep()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
