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
                "com.android.settings" to "com.android.settings.display.ReduceBrightColorsPreferenceFragment",
                "com.android.settings" to "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity"
            )
            tryOpenSettings(intents)
        }
        
        // QColor / Display Color Settings
        binding.itemQColor.setOnClickListener {
            val intents = listOf(
                // Qualcomm QColor (Snapdragon)
                "com.qualcomm.qti.qcolor" to "com.qualcomm.qti.qcolor.QColorActivity",
                // MediaTek MiraVision
                "com.mediatek.miravision.ui" to "com.mediatek.miravision.ui.MiraVisionActivity",
                // AOSP Display Color Mode
                "com.android.settings" to "com.android.settings.display.ColorModePreferenceFragment",
                "com.android.settings" to "com.android.settings.Settings\$ColorModeSettingsActivity",
                // Samsung Screen Mode
                "com.samsung.android.app.screenmode" to "com.samsung.android.app.screenmode.ScreenModeSettingsActivity"
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
        
        // Battery Optimization - App Battery Usage
        binding.itemBatteryOptimization.setOnClickListener {
            val intents = listOf(
                "com.android.settings" to "com.android.settings.Settings\$AppBatteryUsageActivity",
                "com.android.settings" to "com.android.settings.Settings\$HighPowerApplicationsActivity",
                "com.android.settings" to "com.android.settings.fuelgauge.PowerUsageSummary"
            )
            tryOpenSettings(intents)
        }
        
        // Power Mode Settings
        binding.itemPerformanceMode.setOnClickListener {
            val intents = listOf(
                "com.android.settings" to "com.android.settings.fuelgauge.PowerModeSettings",
                "com.android.settings" to "com.android.settings.Settings\$PowerModeSettingsActivity",
                "com.android.settings" to "com.android.settings.fuelgauge.batterysaver.BatterySaverSettings",
                "com.miui.powerkeeper" to "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
            tryOpenSettings(intents)
        }
        
        // Device Info
        binding.itemDeveloperOptions.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
            } catch (e: Exception) {
                val intents = listOf(
                    "com.android.settings" to "com.android.settings.Settings\$DeviceInfoSettingsActivity",
                    "com.android.settings" to "com.android.settings.DeviceInfoSettings"
                )
                tryOpenSettings(intents)
            }
        }
        
        // Device Diagnostic
        binding.itemRunningServices.setOnClickListener {
            val intents = listOf(
                "com.android.devicediagnostics" to "com.android.devicediagnostics.MainActivity",
                "com.android.settings" to "com.android.settings.deviceinfo.aboutphone.DeviceDiagnostic",
                "com.android.settings" to "com.android.settings.Settings\$DevRunningServicesActivity",
                "com.android.settings" to "com.android.settings.applications.RunningServices"
            )
            tryOpenSettings(intents)
        }
        
        // Unknown Sources / Install from unknown apps
        binding.itemUnknownSources.setOnClickListener {
            val intents = listOf(
                "com.android.settings" to "com.android.settings.Settings\$ManageExternalSourcesActivity",
                "com.android.settings" to "com.android.settings.applications.manageapplications.ManageExternalSourcesActivity"
            )
            tryOpenSettings(intents)
        }
        
        // Manage Apps (AOSP)
        binding.itemManageApps.setOnClickListener {
            val intents = listOf(
                "com.android.settings" to "com.android.settings.Settings\$ManageApplicationsActivity",
                "com.android.settings" to "com.android.settings.applications.ManageApplications"
            )
            tryOpenSettings(intents)
        }
        
        // Autostart Manager (multi-brand)
        binding.itemAutostartManager.setOnClickListener {
            val intents = listOf(
                // Xiaomi/MIUI/HyperOS
                "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
                // OPPO/Realme (ColorOS)
                "com.coloros.safecenter" to "com.coloros.privacypermissionsentry.PermissionTopActivity",
                "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
                // Vivo (Funtouch/OriginOS)
                "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                // Huawei/Honor (EMUI/MagicUI)
                "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                // OnePlus (OxygenOS)
                "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
                // Samsung (OneUI)
                "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
                // ASUS (ZenUI)
                "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
                // Sony Xperia
                "com.sonymobile.cta" to "com.sonymobile.cta.SomcCTAMainActivity",
                // Lenovo/Motorola
                "com.lenovo.security" to "com.lenovo.security.purebackground.PureBackgroundActivity",
                // ZTE
                "com.zte.heartyservice" to "com.zte.heartyservice.autorun.AppAutoRunManager",
                // Meizu (Flyme)
                "com.meizu.safe" to "com.meizu.safe.permission.SmartBGActivity",
                // Tecno/Infinix/itel (Transsion)
                "com.transsion.phonemanager" to "com.transsion.phonemanager.permission.PermissionActivity"
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
