package com.appcontrolx.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.appcontrolx.R
import com.appcontrolx.databinding.FragmentToolsBinding

class ToolsFragment : Fragment() {
    
    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHiddenSettings()
        setupActivityLauncher()
    }
    
    private fun setupHiddenSettings() {
        // Display Settings - Extra Dim (Android 12+)
        binding.itemExtraDim.setOnClickListener {
            val intents = listOf(
                // Android 12+ Extra Dim
                "com.android.settings" to "com.android.settings.display.ReduceBrightColorsPreferenceFragment",
                // Alternative
                "com.android.settings" to "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity"
            )
            tryOpenSettings(intents)
        }
        
        // Notification Log - Xiaomi/Universal
        binding.itemNotificationLog.setOnClickListener {
            val intents = listOf(
                // AOSP Notification Log
                "com.android.settings" to "com.android.settings.notification.NotificationStation",
                // Xiaomi MIUI
                "com.miui.securitycenter" to "com.miui.notificationlog.ui.main.NotificationLogActivity",
                // Alternative AOSP
                "com.android.settings" to "com.android.settings.Settings\$NotificationStationActivity"
            )
            tryOpenSettings(intents)
        }
        
        // Notification History
        binding.itemNotificationHistory.setOnClickListener {
            val intents = listOf(
                "com.android.settings" to "com.android.settings.notification.history.NotificationHistoryActivity",
                "com.android.settings" to "com.android.settings.Settings\$NotificationHistoryActivity"
            )
            tryOpenSettings(intents)
        }
        
        // Battery Optimization
        binding.itemBatteryOptimization.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: Exception) {
                val intents = listOf(
                    "com.android.settings" to "com.android.settings.Settings\$HighPowerApplicationsActivity",
                    "com.android.settings" to "com.android.settings.fuelgauge.PowerUsageSummary"
                )
                tryOpenSettings(intents)
            }
        }
        
        // Power Mode Settings
        binding.itemPerformanceMode.setOnClickListener {
            val intents = listOf(
                // AOSP Power Mode Settings
                "com.android.settings" to "com.android.settings.Settings\$PowerModeSettingsActivity",
                // Alternative Power/Battery Settings
                "com.android.settings" to "com.android.settings.fuelgauge.batterysaver.BatterySaverSettings",
                // Xiaomi Power Mode
                "com.miui.powerkeeper" to "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                // Samsung Power Mode
                "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
            tryOpenSettings(intents)
        }
        
        // Developer Options
        binding.itemDeveloperOptions.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (e: Exception) {
                openHiddenSetting("com.android.settings", "com.android.settings.Settings\$DevelopmentSettingsActivity")
            }
        }
        
        // Running Services (AOSP)
        binding.itemRunningServices.setOnClickListener {
            val intents = listOf(
                "com.android.settings" to "com.android.settings.Settings\$DevRunningServicesActivity",
                "com.android.settings" to "com.android.settings.applications.RunningServices"
            )
            tryOpenSettings(intents)
        }
    }
    
    private fun tryOpenSettings(intents: List<Pair<String, String>>) {
        for ((pkg, cls) in intents) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(pkg, cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                return
            } catch (e: Exception) {
                continue
            }
        }
        showError()
    }
    
    private fun setupActivityLauncher() {
        binding.itemActivityLauncher.setOnClickListener {
            val bottomSheet = ActivityLauncherBottomSheet.newInstance()
            bottomSheet.show(childFragmentManager, ActivityLauncherBottomSheet.TAG)
        }
    }
    
    private fun openHiddenSetting(packageName: String, className: String) {
        try {
            val intent = Intent().apply {
                component = ComponentName(packageName, className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Try alternative method
            try {
                val intent = Intent().apply {
                    action = "android.settings.SETTINGS"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                Toast.makeText(context, R.string.tools_navigate_manually, Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                showError()
            }
        }
    }
    
    private fun showError() {
        Toast.makeText(context, R.string.tools_not_available, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
