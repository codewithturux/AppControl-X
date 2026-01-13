package com.appcontrolx.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.appcontrolx.R
import com.appcontrolx.data.model.*
import com.appcontrolx.databinding.FragmentDashboardBinding
import com.appcontrolx.ui.history.ActionHistoryBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Dashboard fragment displaying real-time system information and quick access to features.
 * 
 * Shows:
 * - CPU usage and temperature (wide card at top)
 * - Grid of system info cards (Battery, Network, RAM, Storage, Display, GPU)
 * - Apps count card
 * - Feature quick access cards
 * - Device info card at bottom
 * 
 * Requirements: 0.1, 0.2, 0.3
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeUiState()
        observeNavigationEvents()
    }

    private fun setupClickListeners() {
        // Settings button in header
        binding.btnSettings.setOnClickListener {
            viewModel.onSettingsClicked()
        }

        // Feature cards - delegate to ViewModel
        binding.cardFeatureApps.setOnClickListener {
            viewModel.onFeatureCardClicked(FeatureDestination.APP_MANAGER)
        }

        binding.cardFeatureBattery.setOnClickListener {
            viewModel.onFeatureCardClicked(FeatureDestination.BATTERY_MANAGER)
        }

        binding.cardFeatureActivity.setOnClickListener {
            viewModel.onFeatureCardClicked(FeatureDestination.ACTIVITY_LAUNCHER)
        }

        binding.cardFeatureTools.setOnClickListener {
            viewModel.onFeatureCardClicked(FeatureDestination.TOOLS)
        }

        binding.cardFeatureLogs.setOnClickListener {
            viewModel.onFeatureCardClicked(FeatureDestination.ACTION_LOGS)
        }

        binding.cardFeatureSettings.setOnClickListener {
            viewModel.onFeatureCardClicked(FeatureDestination.SETTINGS)
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
    
    /**
     * Observe navigation events from ViewModel.
     * Handles one-time navigation events for feature card clicks.
     */
    private fun observeNavigationEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    handleNavigationEvent(event)
                }
            }
        }
    }
    
    /**
     * Handle navigation events from ViewModel.
     */
    private fun handleNavigationEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateToFeature -> {
                navigateToFeature(event.destination)
            }
        }
    }
    
    /**
     * Navigate to the specified feature destination.
     */
    private fun navigateToFeature(destination: FeatureDestination) {
        try {
            val navController = findNavController()
            when (destination) {
                FeatureDestination.APP_MANAGER -> navController.navigate(R.id.appListFragment)
                FeatureDestination.BATTERY_MANAGER -> navController.navigate(R.id.appListFragment) // Battery manager is part of app list
                FeatureDestination.ACTIVITY_LAUNCHER -> navController.navigate(R.id.appListFragment) // Activity launcher can be accessed from app list
                FeatureDestination.TOOLS -> navController.navigate(R.id.settingsFragment) // Tools features are in settings
                FeatureDestination.ACTION_LOGS -> showActionHistoryBottomSheet()
                FeatureDestination.SETTINGS -> navController.navigate(R.id.settingsFragment)
            }
        } catch (e: Exception) {
            // Navigation not available or destination not found
        }
    }
    
    /**
     * Show the action history bottom sheet.
     */
    private fun showActionHistoryBottomSheet() {
        ActionHistoryBottomSheet.newInstance()
            .show(childFragmentManager, ActionHistoryBottomSheet.TAG)
    }

    private fun updateUi(state: DashboardUiState) {
        // Update mode indicator
        updateModeIndicator(state.executionMode)
        
        // Update system snapshot
        state.systemSnapshot?.let { snapshot ->
            updateCpuCard(snapshot.cpu)
            updateBatteryCard(snapshot.battery)
            updateNetworkCard(snapshot.network)
            updateRamCard(snapshot.ram)
            updateStorageCard(snapshot.storage)
        }
        
        // Update display info
        state.displayInfo?.let { updateDisplayCard(it) }
        
        // Update GPU info
        updateGpuCard(state.gpuInfo)
        
        // Update app counts
        state.appCounts?.let { updateAppsCard(it) }
        
        // Update device info
        state.deviceInfo?.let { updateDeviceInfoCard(it) }
    }

    private fun updateModeIndicator(mode: ExecutionMode) {
        val modeText = when (mode) {
            ExecutionMode.Root -> getString(R.string.mode_root) + " ✓"
            ExecutionMode.Shizuku -> getString(R.string.mode_shizuku) + " ✓"
            ExecutionMode.None -> getString(R.string.mode_view_only)
        }
        binding.tvCurrentMode.text = modeText
    }

    private fun updateCpuCard(cpu: CpuInfo) {
        val usagePercent = cpu.usagePercent.toInt()
        binding.tvCpuUsage.text = getString(R.string.dashboard_percent_format, usagePercent)
        binding.progressCpu.progress = usagePercent
        
        cpu.temperature?.let { temp ->
            binding.tvCpuTemp.text = getString(R.string.dashboard_temp_format, temp)
            binding.tvCpuTemp.visibility = View.VISIBLE
        } ?: run {
            binding.tvCpuTemp.visibility = View.GONE
        }
        
        binding.tvCpuCores.text = getString(R.string.dashboard_cpu_cores, cpu.cores)
    }

    private fun updateBatteryCard(battery: BatteryInfo) {
        binding.tvBatteryPercent.text = getString(R.string.dashboard_percent_format, battery.percent)
        
        val statusText = buildString {
            append(getString(R.string.dashboard_temp_format, battery.temperature))
            if (battery.isCharging) {
                append(" • ")
                append(getString(R.string.dashboard_charging))
            }
        }
        binding.tvBatteryTemp.text = statusText
        
        // Update battery icon color based on level
        val iconTint = when {
            battery.percent <= 20 -> R.color.status_negative
            battery.isCharging -> R.color.status_positive
            else -> R.color.status_positive
        }
        binding.ivBatteryIcon.setColorFilter(requireContext().getColor(iconTint))
    }

    private fun updateNetworkCard(network: NetworkInfo) {
        val typeText = when (network.type) {
            NetworkType.WIFI -> "WiFi"
            NetworkType.MOBILE -> "Mobile"
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.NONE -> getString(R.string.dashboard_network_disconnected)
        }
        binding.tvNetworkType.text = typeText
        
        val detailText = when {
            network.type == NetworkType.WIFI && network.ssid != null -> network.ssid
            network.type == NetworkType.MOBILE -> "Cellular"
            network.isConnected -> getString(R.string.status_available)
            else -> getString(R.string.status_not_available)
        }
        binding.tvNetworkDetail.text = detailText
        
        // Update icon color based on connection status
        val iconTint = if (network.isConnected) R.color.status_positive else R.color.status_neutral
        binding.ivNetworkIcon.setColorFilter(requireContext().getColor(iconTint))
    }

    private fun updateRamCard(ram: RamInfo) {
        val usedGb = bytesToGb(ram.usedBytes)
        val totalGb = bytesToGb(ram.totalBytes)
        val percent = ram.usedPercent.toInt()
        
        binding.tvRamUsed.text = formatSize(ram.usedBytes)
        binding.tvRamDetail.text = "of ${formatSize(ram.totalBytes)} • $percent%"
    }

    private fun updateStorageCard(storage: StorageInfo) {
        val percent = storage.usedPercent.toInt()
        
        binding.tvStorageUsed.text = formatSize(storage.usedBytes)
        binding.tvStorageDetail.text = "of ${formatSize(storage.totalBytes)} • $percent%"
    }

    private fun updateDisplayCard(display: DisplayInfo) {
        binding.tvDisplayResolution.text = display.resolution
        binding.tvDisplayRefresh.text = getString(R.string.dashboard_hz_format, display.refreshRate)
    }

    private fun updateGpuCard(gpu: GpuInfo?) {
        if (gpu != null) {
            binding.tvGpuModel.text = gpu.model
            gpu.vendor?.let {
                binding.tvGpuVendor.text = it
                binding.tvGpuVendor.visibility = View.VISIBLE
            } ?: run {
                binding.tvGpuVendor.visibility = View.GONE
            }
        } else {
            binding.tvGpuModel.text = getString(R.string.dashboard_gpu_unavailable)
            binding.tvGpuVendor.visibility = View.GONE
        }
    }

    private fun updateAppsCard(counts: AppCounts) {
        binding.tvAppsCount.text = getString(
            R.string.dashboard_apps_count,
            counts.userApps,
            counts.systemApps
        )
    }


    private fun updateDeviceInfoCard(device: DeviceInfo) {
        // Device name
        binding.tvDeviceName.text = "${device.brand} ${device.model}"
        
        // Android version
        binding.tvAndroidVersion.text = "Android ${device.androidVersion} (API ${device.apiLevel})"
        
        // Uptime
        binding.tvUptime.text = formatDuration(device.uptimeMs)
        
        // Deep sleep (only shown in Root mode)
        device.deepSleepMs?.let { deepSleep ->
            binding.layoutDeepSleep.visibility = View.VISIBLE
            binding.tvDeepSleep.text = formatDuration(deepSleep)
        } ?: run {
            binding.layoutDeepSleep.visibility = View.GONE
        }
        
        // Kernel version
        binding.tvKernel.text = device.kernelVersion
        
        // Build number
        binding.tvBuild.text = device.buildNumber
    }

    // ==================== Utility Functions ====================

    private fun bytesToGb(bytes: Long): Float {
        return bytes / (1024f * 1024f * 1024f)
    }

    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) {
            String.format("%.1f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }

    private fun formatDuration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m")
        }.trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
