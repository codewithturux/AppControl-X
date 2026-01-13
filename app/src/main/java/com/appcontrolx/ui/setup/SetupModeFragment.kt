package com.appcontrolx.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appcontrolx.R
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.databinding.FragmentSetupModeBinding
import com.appcontrolx.domain.executor.PermissionBridge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Mode selection fragment for the setup wizard.
 * 
 * Allows user to select execution mode: Root, Shizuku, or View-Only.
 * 
 * Requirements: 0.4, 0.5, 0.6
 */
@AndroidEntryPoint
class SetupModeFragment : Fragment() {
    
    private var _binding: FragmentSetupModeBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var permissionBridge: PermissionBridge
    
    private var selectedMode: ExecutionMode? = null
    private var rootGranted = false
    private var shizukuGranted = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupModeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupModeCards()
        setupContinueButton()
        checkCurrentPermissions()
    }
    
    private fun setupModeCards() {
        // Root mode card
        binding.cardRoot.setOnClickListener {
            if (rootGranted) {
                selectMode(ExecutionMode.Root)
            } else {
                checkRootAccess()
            }
        }
        binding.btnCheckRoot.setOnClickListener {
            checkRootAccess()
        }
        
        // Shizuku mode card
        binding.cardShizuku.setOnClickListener {
            if (shizukuGranted) {
                selectMode(ExecutionMode.Shizuku)
            } else {
                checkShizukuAccess()
            }
        }
        binding.btnCheckShizuku.setOnClickListener {
            checkShizukuAccess()
        }
        
        // View only mode card
        binding.cardViewOnly.setOnClickListener {
            selectMode(ExecutionMode.None)
        }
    }
    
    private fun checkRootAccess() {
        binding.btnCheckRoot.isEnabled = false
        binding.btnCheckRoot.text = getString(R.string.btn_checking)
        
        lifecycleScope.launch {
            val hasRoot = withContext(Dispatchers.IO) {
                permissionBridge.checkRootNow()
            }
            
            rootGranted = hasRoot
            
            if (hasRoot) {
                selectMode(ExecutionMode.Root)
                binding.btnCheckRoot.text = getString(R.string.status_access_granted)
                binding.btnCheckRoot.isEnabled = false
                Toast.makeText(context, R.string.root_granted, Toast.LENGTH_SHORT).show()
            } else {
                binding.btnCheckRoot.isEnabled = true
                binding.btnCheckRoot.text = getString(R.string.btn_check)
                Toast.makeText(context, R.string.root_denied, Toast.LENGTH_SHORT).show()
            }
            
            updateUI()
        }
    }
    
    private fun checkShizukuAccess() {
        binding.btnCheckShizuku.isEnabled = false
        binding.btnCheckShizuku.text = getString(R.string.btn_checking)
        
        if (permissionBridge.isShizukuAvailable()) {
            if (permissionBridge.isShizukuPermissionGranted()) {
                shizukuGranted = true
                selectMode(ExecutionMode.Shizuku)
                binding.btnCheckShizuku.text = getString(R.string.status_access_granted)
            } else {
                permissionBridge.requestShizukuPermission()
                binding.btnCheckShizuku.isEnabled = true
                binding.btnCheckShizuku.text = getString(R.string.btn_check)
            }
        } else {
            binding.btnCheckShizuku.isEnabled = true
            binding.btnCheckShizuku.text = getString(R.string.btn_check)
            Toast.makeText(context, R.string.error_shizuku_not_available, Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    
    private fun selectMode(mode: ExecutionMode) {
        selectedMode = mode
        permissionBridge.setMode(mode)
        
        // Update card selection states
        binding.cardRoot.isChecked = mode == ExecutionMode.Root
        binding.cardShizuku.isChecked = mode == ExecutionMode.Shizuku
        binding.cardViewOnly.isChecked = mode == ExecutionMode.None
        
        updateUI()
    }
    
    private fun checkCurrentPermissions() {
        // Check current mode from PermissionBridge
        lifecycleScope.launch {
            val currentMode = permissionBridge.mode
            
            when (currentMode) {
                ExecutionMode.Root -> {
                    rootGranted = permissionBridge.isRootAvailable()
                    if (rootGranted) {
                        selectedMode = ExecutionMode.Root
                        binding.cardRoot.isChecked = true
                    }
                }
                ExecutionMode.Shizuku -> {
                    shizukuGranted = permissionBridge.isShizukuReady()
                    if (shizukuGranted) {
                        selectedMode = ExecutionMode.Shizuku
                        binding.cardShizuku.isChecked = true
                    }
                }
                ExecutionMode.None -> {
                    selectedMode = ExecutionMode.None
                    binding.cardViewOnly.isChecked = true
                }
            }
            
            updateUI()
        }
    }
    
    private fun updateUI() {
        val context = context ?: return
        
        // Root status
        binding.tvRootStatus.text = if (rootGranted) 
            getString(R.string.status_granted) else getString(R.string.status_not_granted)
        binding.tvRootStatus.setTextColor(ContextCompat.getColor(context,
            if (rootGranted) R.color.status_positive else R.color.status_neutral))
        
        // Root check icon
        binding.ivRootCheck.visibility = if (rootGranted && binding.cardRoot.isChecked) 
            View.VISIBLE else View.GONE
        if (rootGranted) {
            binding.btnCheckRoot.text = getString(R.string.status_access_granted)
            binding.btnCheckRoot.isEnabled = false
        }
        
        // Shizuku status
        val shizukuAvailable = permissionBridge.isShizukuAvailable()
        binding.tvShizukuStatus.text = when {
            shizukuGranted -> getString(R.string.status_granted)
            shizukuAvailable -> getString(R.string.status_not_granted)
            else -> getString(R.string.status_not_available)
        }
        binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(context,
            if (shizukuGranted) R.color.status_positive else R.color.status_neutral))
        
        // Shizuku check icon
        binding.ivShizukuCheck.visibility = if (shizukuGranted && binding.cardShizuku.isChecked) 
            View.VISIBLE else View.GONE
        if (shizukuGranted) {
            binding.btnCheckShizuku.text = getString(R.string.status_access_granted)
            binding.btnCheckShizuku.isEnabled = false
        }
        
        // View only check icon
        binding.ivViewOnlyCheck.visibility = if (binding.cardViewOnly.isChecked) 
            View.VISIBLE else View.GONE
        
        // Continue button - enabled if a mode is selected
        binding.btnContinue.isEnabled = selectedMode != null
    }
    
    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            if (selectedMode != null) {
                // Navigate to Complete screen
                (activity as? SetupActivity)?.nextStep()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning to this fragment
        checkCurrentPermissions()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
