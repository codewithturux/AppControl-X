package com.appcontrolx.ui.setup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.appcontrolx.R
import com.appcontrolx.databinding.FragmentPermissionsBinding
import com.appcontrolx.service.PermissionBridge
import com.appcontrolx.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionsFragment : Fragment() {
    
    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!
    
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }
    private val permissionBridge by lazy { PermissionBridge(requireContext()) }
    
    private var rootGranted = false
    private var shizukuGranted = false
    private var notificationGranted = false
    private var queryAppsGranted = false
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        updateUI()
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupPermissionButtons()
        setupModeSelection()
        setupContinueButton()
        checkCurrentPermissions()
    }
    
    private fun setupPermissionButtons() {
        // Notification permission
        binding.btnNotification.setOnClickListener {
            requestNotificationPermission()
        }
        
        // Query apps permission (auto-granted via manifest on most devices)
        binding.btnQueryApps.setOnClickListener {
            // Open app settings if needed
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${requireContext().packageName}")
                })
            } catch (e: Exception) {
                Toast.makeText(context, R.string.error_open_settings, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupModeSelection() {
        // Root mode
        binding.cardRoot.setOnClickListener {
            checkRootAccess()
        }
        binding.btnCheckRoot.setOnClickListener {
            checkRootAccess()
        }
        
        // Shizuku mode
        binding.cardShizuku.setOnClickListener {
            checkShizukuAccess()
        }
        binding.btnCheckShizuku.setOnClickListener {
            checkShizukuAccess()
        }
        
        // View only mode
        binding.cardViewOnly.setOnClickListener {
            selectMode(Constants.MODE_NONE)
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
                selectMode(Constants.MODE_ROOT)
                Toast.makeText(context, R.string.root_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.root_denied, Toast.LENGTH_SHORT).show()
            }
            
            binding.btnCheckRoot.isEnabled = true
            binding.btnCheckRoot.text = getString(R.string.btn_check)
            updateUI()
        }
    }
    
    private fun checkShizukuAccess() {
        if (permissionBridge.isShizukuAvailable()) {
            if (permissionBridge.isShizukuPermissionGranted()) {
                shizukuGranted = true
                selectMode(Constants.MODE_SHIZUKU)
            } else {
                permissionBridge.requestShizukuPermission()
            }
        } else {
            Toast.makeText(context, R.string.error_shizuku_not_available, Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }
    
    private fun selectMode(mode: String) {
        prefs.edit().putString(Constants.PREFS_EXECUTION_MODE, mode).apply()
        
        // Update selection UI
        binding.cardRoot.isChecked = mode == Constants.MODE_ROOT
        binding.cardShizuku.isChecked = mode == Constants.MODE_SHIZUKU
        binding.cardViewOnly.isChecked = mode == Constants.MODE_NONE
        
        updateUI()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationGranted = true
            updateUI()
        }
    }
    
    private fun checkCurrentPermissions() {
        // Check notification
        notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == 
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        // Query apps is auto-granted via manifest
        queryAppsGranted = true
        
        // Check saved mode
        val savedMode = prefs.getString(Constants.PREFS_EXECUTION_MODE, null)
        when (savedMode) {
            Constants.MODE_ROOT -> {
                rootGranted = permissionBridge.isRootAvailable()
                binding.cardRoot.isChecked = true
            }
            Constants.MODE_SHIZUKU -> {
                shizukuGranted = permissionBridge.isShizukuReady()
                binding.cardShizuku.isChecked = true
            }
            Constants.MODE_NONE -> {
                binding.cardViewOnly.isChecked = true
            }
        }
        
        updateUI()
    }
    
    private fun updateUI() {
        // Notification status
        binding.tvNotificationStatus.text = if (notificationGranted) 
            getString(R.string.status_granted) else getString(R.string.status_not_granted)
        binding.tvNotificationStatus.setTextColor(resources.getColor(
            if (notificationGranted) R.color.status_positive else R.color.status_neutral, null))
        binding.btnNotification.visibility = if (notificationGranted) View.GONE else View.VISIBLE
        
        // Query apps status
        binding.tvQueryAppsStatus.text = getString(R.string.status_granted_auto)
        binding.tvQueryAppsStatus.setTextColor(resources.getColor(R.color.status_positive, null))
        binding.btnQueryApps.visibility = View.GONE
        
        // Root status
        binding.tvRootStatus.text = if (rootGranted) 
            getString(R.string.status_granted) else getString(R.string.status_not_granted)
        binding.tvRootStatus.setTextColor(resources.getColor(
            if (rootGranted) R.color.status_positive else R.color.status_neutral, null))
        binding.ivRootCheck.visibility = if (binding.cardRoot.isChecked) View.VISIBLE else View.GONE
        
        // Shizuku status
        val shizukuAvailable = permissionBridge.isShizukuAvailable()
        binding.tvShizukuStatus.text = when {
            shizukuGranted -> getString(R.string.status_granted)
            shizukuAvailable -> getString(R.string.status_not_granted)
            else -> getString(R.string.status_not_available)
        }
        binding.tvShizukuStatus.setTextColor(resources.getColor(
            if (shizukuGranted) R.color.status_positive else R.color.status_neutral, null))
        binding.ivShizukuCheck.visibility = if (binding.cardShizuku.isChecked) View.VISIBLE else View.GONE
        
        // View only check
        binding.ivViewOnlyCheck.visibility = if (binding.cardViewOnly.isChecked) View.VISIBLE else View.GONE
        
        // Continue button - enabled if a mode is selected
        val modeSelected = binding.cardRoot.isChecked || binding.cardShizuku.isChecked || binding.cardViewOnly.isChecked
        binding.btnContinue.isEnabled = modeSelected
    }
    
    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            (activity as? SetupActivity)?.completeSetup()
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkCurrentPermissions()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
