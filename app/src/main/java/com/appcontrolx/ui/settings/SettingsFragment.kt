package com.appcontrolx.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.appcontrolx.R
import com.appcontrolx.data.model.AnimationScale
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.databinding.FragmentSettingsBinding
import com.appcontrolx.ui.history.ActionHistoryBottomSheet
import com.appcontrolx.ui.setup.SetupActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Settings fragment for app configuration.
 * 
 * Provides:
 * - Theme selection (Light/Dark/System)
 * - Execution mode selection (Root/Shizuku/View-Only)
 * - Display refresh rate settings (when Root/Shizuku available)
 * - Safety settings (confirm actions, protect system apps)
 * - Rollback settings (auto snapshot, view logs, clear snapshots)
 * - About section
 * - Reset setup option
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    
    private val themeOptions = arrayOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES
    )
    
    private val shizukuPermissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (viewModel.checkAndSetShizukuMode()) {
                    showModeChangedDialog()
                }
            } else {
                Toast.makeText(context, R.string.error_shizuku_not_available, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeUiState()
        observeEvents()
        
        // Register Shizuku listener
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Exception) {
            // Shizuku not available
        }
    }
    
    private fun setupClickListeners() {
        // Theme selection
        binding.itemTheme.setOnClickListener {
            showThemeDialog()
        }
        
        // Mode change
        binding.btnChangeMode.setOnClickListener {
            showModeSelectionDialog()
        }
        
        // Display settings
        binding.itemMinRefreshRate.setOnClickListener {
            showRefreshRateDialog(isMin = true)
        }
        
        binding.itemMaxRefreshRate.setOnClickListener {
            showRefreshRateDialog(isMin = false)
        }
        
        binding.itemResetRefreshRate.setOnClickListener {
            showResetRefreshRateConfirmation()
        }
        
        // Animation scale
        binding.itemAnimationScale.setOnClickListener {
            showAnimationScaleDialog()
        }
        
        // Safety settings
        binding.switchConfirmActions.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setConfirmActions(isChecked)
        }
        
        binding.switchProtectSystem.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setProtectSystem(isChecked)
        }
        
        // Rollback settings
        binding.switchAutoSnapshot.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoSnapshot(isChecked)
        }
        
        binding.itemViewLogs.setOnClickListener {
            showActionHistoryBottomSheet()
        }
        
        binding.itemClearSnapshots.setOnClickListener {
            showClearSnapshotsConfirmation()
        }
        
        // Action Logs section
        binding.itemViewActionLogs.setOnClickListener {
            showActionHistoryBottomSheet()
        }
        
        binding.itemRollbackLastAction.setOnClickListener {
            showRollbackConfirmation()
        }
        
        binding.itemClearActionLogs.setOnClickListener {
            showClearActionLogsConfirmation()
        }
        
        // About
        binding.itemAbout.setOnClickListener {
            try {
                findNavController().navigate(R.id.aboutFragment)
            } catch (e: Exception) {
                // Navigation not available
            }
        }
        
        // Reset setup
        binding.itemResetSetup.setOnClickListener {
            showResetSetupConfirmation()
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }
    
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }
    
    private fun updateUi(state: SettingsUiState) {
        // Theme
        binding.tvCurrentTheme.text = when (state.theme) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
        
        // Execution mode
        binding.tvCurrentMode.text = state.executionMode.displayName
        
        // Display section visibility
        binding.sectionDisplay.visibility = if (state.canControlDisplay) View.VISIBLE else View.GONE
        
        // Refresh rates
        if (state.canControlDisplay) {
            binding.tvMinRefreshRate.text = getString(R.string.settings_refresh_rate_format, state.minRefreshRate)
            binding.tvMaxRefreshRate.text = getString(R.string.settings_refresh_rate_format, state.maxRefreshRate)
            binding.tvAnimationScale.text = state.animationScale.getPresetName()
        }
        
        // Safety settings
        binding.switchConfirmActions.isChecked = state.confirmActions
        binding.switchProtectSystem.isChecked = state.protectSystem
        
        // Rollback settings
        binding.switchAutoSnapshot.isChecked = state.autoSnapshot
        binding.tvLogCount.text = getString(R.string.settings_log_count, state.logCount)
        binding.tvSnapshotCount.text = getString(R.string.settings_snapshot_count, state.snapshotCount)
        
        // Action Logs section
        binding.tvActionLogCount.text = getString(R.string.settings_log_count, state.logCount)
        
        // Rollback availability - disable if no snapshot available
        binding.itemRollbackLastAction.isEnabled = state.rollbackAvailable
        binding.itemRollbackLastAction.alpha = if (state.rollbackAvailable) 1.0f else 0.5f
        binding.tvRollbackStatus.text = if (state.rollbackAvailable) {
            getString(R.string.settings_action_logs_rollback_desc)
        } else {
            getString(R.string.settings_action_logs_rollback_unavailable)
        }
        
        // App version
        binding.tvAppVersion.text = getString(R.string.settings_version_format, state.appVersion)
    }
    
    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ShowMessage -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is SettingsEvent.ShowError -> {
                Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
            is SettingsEvent.NavigateToSetup -> {
                navigateToSetup()
            }
            is SettingsEvent.RestartApp -> {
                restartApp()
            }
            is SettingsEvent.RestartRequired -> {
                showRestartRequiredDialog()
            }
        }
    }
    
    /**
     * Show dialog prompting user to restart app for dynamic colors change.
     */
    private fun showRestartRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_mode_changed)
            .setMessage(R.string.settings_dynamic_colors_restart)
            .setPositiveButton(R.string.settings_restart_now) { _, _ ->
                restartApp()
            }
            .setNegativeButton(R.string.settings_restart_later, null)
            .show()
    }
    
    private fun showThemeDialog() {
        val currentTheme = viewModel.uiState.value.theme
        val currentIndex = themeOptions.indexOf(currentTheme).coerceAtLeast(0)
        
        val themeNames = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_theme)
            .setSingleChoiceItems(themeNames, currentIndex) { dialog, which ->
                viewModel.setTheme(themeOptions[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showModeSelectionDialog() {
        val modes = arrayOf(
            getString(R.string.mode_root),
            getString(R.string.mode_shizuku),
            getString(R.string.mode_view_only)
        )
        
        val currentMode = viewModel.uiState.value.executionMode
        val currentIndex = when (currentMode) {
            ExecutionMode.Root -> 0
            ExecutionMode.Shizuku -> 1
            ExecutionMode.None -> 2
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_change_mode_title)
            .setSingleChoiceItems(modes, currentIndex) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> checkAndSetRootMode()
                    1 -> checkAndSetShizukuMode()
                    2 -> {
                        viewModel.setViewOnlyMode()
                        showModeChangedDialog()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun checkAndSetRootMode() {
        val progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mode_root)
            .setMessage(R.string.btn_checking)
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        viewLifecycleOwner.lifecycleScope.launch {
            val hasRoot = withContext(Dispatchers.IO) {
                viewModel.checkAndSetRootMode()
            }
            
            progressDialog.dismiss()
            
            if (hasRoot) {
                Toast.makeText(context, R.string.root_granted, Toast.LENGTH_SHORT).show()
                showModeChangedDialog()
            } else {
                Toast.makeText(context, R.string.error_root_not_available, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun checkAndSetShizukuMode() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(context, R.string.error_shizuku_not_available, Toast.LENGTH_LONG).show()
                return
            }
            
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (viewModel.checkAndSetShizukuMode()) {
                    showModeChangedDialog()
                }
            } else {
                Shizuku.requestPermission(0)
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_shizuku_not_available, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showModeChangedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_mode_changed)
            .setMessage(R.string.settings_restart_required)
            .setPositiveButton(R.string.settings_restart_now) { _, _ ->
                restartApp()
            }
            .setNegativeButton(R.string.settings_restart_later, null)
            .show()
    }
    
    private fun showRefreshRateDialog(isMin: Boolean) {
        val state = viewModel.uiState.value
        val rates = state.supportedRefreshRates
        
        if (rates.isEmpty()) {
            Toast.makeText(context, R.string.settings_refresh_rate_error, Toast.LENGTH_SHORT).show()
            return
        }
        
        val rateStrings = rates.map { getString(R.string.settings_refresh_rate_format, it) }.toTypedArray()
        val currentRate = if (isMin) state.minRefreshRate else state.maxRefreshRate
        val currentIndex = rates.indexOf(currentRate).coerceAtLeast(0)
        
        val title = if (isMin) R.string.settings_min_refresh_rate else R.string.settings_max_refresh_rate
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(rateStrings, currentIndex) { dialog, which ->
                val selectedRate = rates[which]
                if (isMin) {
                    viewModel.setMinRefreshRate(selectedRate)
                } else {
                    viewModel.setMaxRefreshRate(selectedRate)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showResetRefreshRateConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_reset_refresh_rate)
            .setMessage(R.string.settings_reset_refresh_rate_desc)
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                viewModel.resetRefreshRate()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun showAnimationScaleDialog() {
        val state = viewModel.uiState.value
        val currentScale = state.animationScale.getUniformScale() ?: 1.0f
        
        // Build preset options with "Custom" at the end
        val presetNames = AnimationScale.PRESETS.map { it.second }.toMutableList()
        presetNames.add(getString(R.string.settings_animation_scale_custom))
        
        // Find current selection index
        val currentIndex = AnimationScale.PRESETS.indexOfFirst { it.first == currentScale }
            .let { if (it >= 0) it else presetNames.size - 1 } // Default to Custom if not found
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_animation_scale_select)
            .setSingleChoiceItems(presetNames.toTypedArray(), currentIndex) { dialog, which ->
                if (which < AnimationScale.PRESETS.size) {
                    // Preset selected
                    val selectedScale = AnimationScale.PRESETS[which].first
                    viewModel.setAllAnimationScales(selectedScale)
                    dialog.dismiss()
                } else {
                    // Custom selected
                    dialog.dismiss()
                    showCustomAnimationScaleDialog(currentScale)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showCustomAnimationScaleDialog(currentScale: Float) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_animation_scale, null)
        val slider = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderAnimationScale)
        val tvValue = dialogView.findViewById<android.widget.TextView>(R.id.tvScaleValue)
        
        slider.value = currentScale.coerceIn(AnimationScale.MIN_SCALE, AnimationScale.MAX_SCALE)
        tvValue.text = getString(R.string.settings_animation_scale_format, slider.value)
        
        slider.addOnChangeListener { _, value, _ ->
            tvValue.text = getString(R.string.settings_animation_scale_format, value)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_animation_scale_custom_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_apply) { _, _ ->
                viewModel.setAllAnimationScales(slider.value)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun showClearSnapshotsConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_clear_snapshots)
            .setMessage(R.string.settings_clear_snapshots_confirm)
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                viewModel.clearSnapshots()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    /**
     * Show confirmation dialog before executing rollback.
     * Requirements: 6.4 - Execute rollbackLastAction()
     */
    private fun showRollbackConfirmation() {
        // Check if rollback is available
        if (!viewModel.uiState.value.rollbackAvailable) {
            Toast.makeText(context, R.string.settings_action_logs_rollback_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.log_rollback_title)
            .setMessage(R.string.settings_rollback_confirm)
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                viewModel.rollbackLastAction()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    /**
     * Show confirmation dialog before clearing action logs.
     * Requirements: 6.2 - Clear Action Logs option
     */
    private fun showClearActionLogsConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_action_logs_clear)
            .setMessage(R.string.settings_action_logs_clear_confirm)
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                viewModel.clearActionLogs()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun showResetSetupConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_reset_setup)
            .setMessage(R.string.settings_reset_setup_confirm)
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                navigateToSetup()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun navigateToSetup() {
        // Clear setup complete flag
        requireContext().getSharedPreferences("appcontrolx_prefs", 0)
            .edit()
            .putBoolean("setup_complete", false)
            .apply()
        
        startActivity(Intent(requireContext(), SetupActivity::class.java))
        requireActivity().finish()
    }
    
    private fun restartApp() {
        val intent = requireContext().packageManager
            .getLaunchIntentForPackage(requireContext().packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
            Runtime.getRuntime().exit(0)
        }
    }
    
    /**
     * Show the action history bottom sheet.
     */
    private fun showActionHistoryBottomSheet() {
        val bottomSheet = ActionHistoryBottomSheet.newInstance()
        bottomSheet.onLogCleared = {
            // Refresh log count when logs are cleared
            viewModel.refreshLogCount()
        }
        bottomSheet.onRollbackExecuted = {
            // Refresh state when rollback is executed from bottom sheet
            viewModel.refreshLogCount()
        }
        bottomSheet.show(childFragmentManager, ActionHistoryBottomSheet.TAG)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Exception) {
            // Shizuku not available
        }
        _binding = null
    }
}
