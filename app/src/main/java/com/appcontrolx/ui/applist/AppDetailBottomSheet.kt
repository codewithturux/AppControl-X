package com.appcontrolx.ui.applist

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.appcontrolx.R
import com.appcontrolx.data.model.ActionLog
import com.appcontrolx.data.model.AppAction
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.databinding.BottomSheetAppDetailBinding
import com.appcontrolx.domain.executor.CommandExecutor
import com.appcontrolx.domain.executor.PermissionBridge
import com.appcontrolx.domain.executor.RootExecutor
import com.appcontrolx.domain.executor.ShizukuExecutor
import com.appcontrolx.domain.manager.ActionLogger
import com.appcontrolx.domain.manager.AppControlManager
import com.appcontrolx.domain.manager.BatteryManager
import com.appcontrolx.domain.validator.SafetyValidator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Bottom sheet displaying app details and action buttons.
 * Shows app icon, name, package, version, status, and available actions based on SafetyValidator.
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
@AndroidEntryPoint
class AppDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAppDetailBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var permissionBridge: PermissionBridge
    @Inject lateinit var rootExecutor: RootExecutor
    @Inject lateinit var shizukuExecutor: ShizukuExecutor
    @Inject lateinit var actionLogger: ActionLogger

    private var appInfo: AppInfo? = null
    private var executor: CommandExecutor? = null
    private var appControlManager: AppControlManager? = null
    private var batteryManager: BatteryManager? = null
    private var executionMode: ExecutionMode = ExecutionMode.None
    private var appUid: Int = 0

    /** Callback when an action is completed successfully */
    var onActionCompleted: (() -> Unit)? = null

    companion object {
        const val TAG = "AppDetailBottomSheet"
        private const val ARG_PACKAGE_NAME = "package_name"
        private const val ARG_APP_NAME = "app_name"
        private const val ARG_IS_ENABLED = "is_enabled"
        private const val ARG_IS_SYSTEM = "is_system"
        private const val ARG_IS_RUNNING = "is_running"
        private const val ARG_IS_STOPPED = "is_stopped"
        private const val ARG_IS_BG_RESTRICTED = "is_bg_restricted"

        fun newInstance(app: AppInfo): AppDetailBottomSheet {
            return AppDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PACKAGE_NAME, app.packageName)
                    putString(ARG_APP_NAME, app.appName)
                    putBoolean(ARG_IS_ENABLED, app.isEnabled)
                    putBoolean(ARG_IS_SYSTEM, app.isSystemApp)
                    putBoolean(ARG_IS_RUNNING, app.isRunning)
                    putBoolean(ARG_IS_STOPPED, app.isStopped)
                    putBoolean(ARG_IS_BG_RESTRICTED, app.isBackgroundRestricted)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAppDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupExecutor()
        loadAppInfo()
        loadBatteryStatus()
        setupButtons()
    }

    /**
     * Setup executor based on current execution mode.
     */
    private fun setupExecutor() {
        executionMode = permissionBridge.mode

        executor = when (executionMode) {
            ExecutionMode.Root -> rootExecutor
            ExecutionMode.Shizuku -> shizukuExecutor
            ExecutionMode.None -> null
        }

        executor?.let { exec ->
            appControlManager = AppControlManager(exec)
            batteryManager = BatteryManager(exec)
        }
    }


    /**
     * Load app information from package manager and display in UI.
     * Requirement: 9.2
     */
    private fun loadAppInfo() {
        val packageName = arguments?.getString(ARG_PACKAGE_NAME) ?: return
        val appName = arguments?.getString(ARG_APP_NAME) ?: packageName
        val isEnabled = arguments?.getBoolean(ARG_IS_ENABLED, true) ?: true
        val isSystem = arguments?.getBoolean(ARG_IS_SYSTEM, false) ?: false

        try {
            val pm = requireContext().packageManager
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val appIcon = pm.getApplicationIcon(packageName)
            
            // Get UID for battery manager
            appUid = packageInfo.applicationInfo.uid

            // Set app header info
            binding.ivAppIcon.setImageDrawable(appIcon)
            binding.tvAppName.text = appName
            binding.tvPackageName.text = packageName
            binding.tvVersion.text = getString(
                R.string.detail_version_format,
                packageInfo.versionName ?: "Unknown",
                packageInfo.longVersionCode
            )

            // App type badge
            binding.tvAppType.text = if (isSystem) {
                getString(R.string.detail_system_app)
            } else {
                getString(R.string.detail_user_app)
            }
            binding.tvAppType.setTextColor(
                resources.getColor(
                    if (isSystem) R.color.status_neutral else R.color.status_positive,
                    null
                )
            )

            // App size
            val appFile = File(packageInfo.applicationInfo.sourceDir)
            binding.tvAppSize.text = formatFileSize(appFile.length())

            // Install path
            binding.tvInstallPath.text = packageInfo.applicationInfo.sourceDir

            // Target SDK
            binding.tvTargetSdk.text = getString(
                R.string.detail_sdk_format,
                packageInfo.applicationInfo.targetSdkVersion
            )

            // Min SDK
            binding.tvMinSdk.text = getString(
                R.string.detail_sdk_format,
                packageInfo.applicationInfo.minSdkVersion
            )

            // Permissions count
            val permCount = packageInfo.requestedPermissions?.size ?: 0
            binding.tvPermissions.text = getString(R.string.detail_permissions_count, permCount)

            // Update freeze/unfreeze button text based on current state
            binding.btnToggleEnable.text = if (isEnabled) {
                getString(R.string.action_freeze)
            } else {
                getString(R.string.action_unfreeze)
            }

            // Store app info for actions
            appInfo = AppInfo(
                packageName = packageName,
                appName = appName,
                icon = appIcon,
                versionName = packageInfo.versionName ?: "",
                versionCode = packageInfo.longVersionCode,
                isSystemApp = isSystem,
                isEnabled = isEnabled,
                isRunning = arguments?.getBoolean(ARG_IS_RUNNING, false) ?: false,
                isStopped = arguments?.getBoolean(ARG_IS_STOPPED, false) ?: false,
                isBackgroundRestricted = arguments?.getBoolean(ARG_IS_BG_RESTRICTED, false) ?: false,
                installedTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime
            )

        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_load_app_info, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private var isOpsExpanded = false

    /**
     * Load and display battery/background restriction status.
     */
    private fun loadBatteryStatus() {
        val packageName = arguments?.getString(ARG_PACKAGE_NAME) ?: return

        // Setup expand button for additional ops
        binding.btnExpandOps.setOnClickListener {
            isOpsExpanded = !isOpsExpanded
            binding.expandedOpsContainer.visibility = if (isOpsExpanded) View.VISIBLE else View.GONE
            binding.btnExpandOps.rotation = if (isOpsExpanded) 180f else 0f

            if (isOpsExpanded) {
                loadExtendedOps(packageName)
            }
        }

        lifecycleScope.launch {
            val (runInBg, runAnyInBg) = withContext(Dispatchers.IO) {
                val exec = executor
                if (exec != null) {
                    val runInBgResult = exec.execute("appops get $packageName RUN_IN_BACKGROUND")
                    val runAnyInBgResult = exec.execute("appops get $packageName RUN_ANY_IN_BACKGROUND")

                    Pair(
                        parseAppOpsOutput(runInBgResult.getOrNull() ?: ""),
                        parseAppOpsOutput(runAnyInBgResult.getOrNull() ?: "")
                    )
                } else {
                    Pair("No Mode", "No Mode")
                }
            }

            updateBatteryStatusUI(runInBg, runAnyInBg)
        }
    }

    /**
     * Load extended appops information.
     */
    private fun loadExtendedOps(packageName: String) {
        lifecycleScope.launch {
            val ops = withContext(Dispatchers.IO) {
                val exec = executor ?: return@withContext emptyMap<String, String>()

                val opsToCheck = listOf(
                    "WAKE_LOCK",
                    "START_FOREGROUND",
                    "BOOT_COMPLETED",
                    "SYSTEM_ALERT_WINDOW",
                    "REQUEST_INSTALL_PACKAGES"
                )

                opsToCheck.associateWith { op ->
                    val result = exec.execute("appops get $packageName $op")
                    parseAppOpsOutput(result.getOrNull() ?: "")
                }
            }

            // Update UI
            updateOpTextView(binding.tvWakeLock, ops["WAKE_LOCK"] ?: "-")
            updateOpTextView(binding.tvStartForeground, ops["START_FOREGROUND"] ?: "-")
            updateOpTextView(binding.tvBootCompleted, ops["BOOT_COMPLETED"] ?: "-")
            updateOpTextView(binding.tvSystemAlertWindow, ops["SYSTEM_ALERT_WINDOW"] ?: "-")
            updateOpTextView(binding.tvRequestInstall, ops["REQUEST_INSTALL_PACKAGES"] ?: "-")
        }
    }

    private fun updateOpTextView(tv: TextView, value: String) {
        tv.text = value
        tv.setTextColor(
            resources.getColor(
                when (value) {
                    "ignore", "deny" -> R.color.status_negative
                    "allow" -> R.color.status_positive
                    else -> R.color.on_surface_secondary
                },
                null
            )
        )
    }

    private fun parseAppOpsOutput(output: String): String {
        val lowerOutput = output.lowercase()
        return when {
            lowerOutput.contains(": ignore") || lowerOutput.contains(":ignore") -> "ignore"
            lowerOutput.contains(": deny") || lowerOutput.contains(":deny") -> "deny"
            lowerOutput.contains(": allow") || lowerOutput.contains(":allow") -> "allow"
            lowerOutput.contains(": default") || lowerOutput.contains(":default") -> "default"
            lowerOutput.contains("no operations") -> "default"
            output.isBlank() -> "error"
            else -> output.trim().take(20)
        }
    }

    private fun updateBatteryStatusUI(runInBg: String, runAnyInBg: String) {
        binding.tvRunInBg.text = runInBg
        binding.tvRunInBg.setTextColor(
            resources.getColor(
                if (runInBg == "ignore" || runInBg == "deny") R.color.status_negative
                else R.color.status_positive,
                null
            )
        )

        binding.tvRunAnyInBg.text = runAnyInBg
        binding.tvRunAnyInBg.setTextColor(
            resources.getColor(
                if (runAnyInBg == "ignore" || runAnyInBg == "deny") R.color.status_negative
                else R.color.status_positive,
                null
            )
        )
    }


    /**
     * Setup action buttons based on SafetyValidator rules.
     * Requirement: 9.3, 9.4
     */
    private fun setupButtons() {
        val packageName = arguments?.getString(ARG_PACKAGE_NAME) ?: return
        val isEnabled = arguments?.getBoolean(ARG_IS_ENABLED, true) ?: true
        val hasMode = executionMode != ExecutionMode.None

        // Get allowed actions from SafetyValidator
        val allowedActions = SafetyValidator.getAllowedActions(packageName)
        val isForceStopOnly = SafetyValidator.isForceStopOnly(packageName)
        val isCritical = SafetyValidator.isCritical(packageName)

        // Update freeze/unfreeze button text based on current state
        binding.btnToggleEnable.text = if (isEnabled) {
            getString(R.string.action_freeze)
        } else {
            getString(R.string.action_unfreeze)
        }

        // Set button enabled states based on SafetyValidator
        setButtonEnabled(binding.btnForceStop, hasMode && !isCritical)
        setButtonEnabled(
            binding.btnToggleEnable,
            hasMode && AppAction.FREEZE in allowedActions
        )
        setButtonEnabled(
            binding.btnRestrictBg,
            hasMode && AppAction.RESTRICT_BACKGROUND in allowedActions
        )
        setButtonEnabled(
            binding.btnAllowBg,
            hasMode && AppAction.ALLOW_BACKGROUND in allowedActions
        )
        setButtonEnabled(binding.btnClearCache, hasMode && !isCritical)
        setButtonEnabled(binding.btnClearData, hasMode && !isCritical)
        setButtonEnabled(
            binding.btnUninstall,
            hasMode && AppAction.UNINSTALL in allowedActions
        )

        // Force Stop
        binding.btnForceStop.setOnClickListener {
            if (isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            executeAction(getString(R.string.action_force_stop), AppAction.FORCE_STOP) {
                appControlManager?.forceStop(appInfo!!.packageName)
            }
        }

        // Freeze/Unfreeze
        binding.btnToggleEnable.setOnClickListener {
            if (isForceStopOnly || isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            val app = appInfo ?: return@setOnClickListener
            val actionName = if (app.isEnabled) {
                getString(R.string.action_freeze)
            } else {
                getString(R.string.action_unfreeze)
            }
            val action = if (app.isEnabled) AppAction.FREEZE else AppAction.UNFREEZE

            executeAction(actionName, action) {
                if (app.isEnabled) {
                    appControlManager?.freezeApp(app.packageName)
                } else {
                    appControlManager?.unfreezeApp(app.packageName)
                }
            }
        }

        // Restrict Background
        binding.btnRestrictBg.setOnClickListener {
            if (isForceStopOnly || isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            executeBackgroundAction(
                getString(R.string.action_restrict_bg),
                AppAction.RESTRICT_BACKGROUND
            ) {
                batteryManager?.restrictBackground(appInfo!!.packageName, appUid)
            }
        }

        // Allow Background
        binding.btnAllowBg.setOnClickListener {
            if (isForceStopOnly || isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            executeBackgroundAction(
                getString(R.string.action_allow_bg),
                AppAction.ALLOW_BACKGROUND
            ) {
                batteryManager?.allowBackground(appInfo!!.packageName, appUid)
            }
        }

        // Clear Cache
        binding.btnClearCache.setOnClickListener {
            if (isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            executeAction(getString(R.string.action_clear_cache), AppAction.CLEAR_CACHE) {
                appControlManager?.clearCache(appInfo!!.packageName)
            }
        }

        // Clear Data
        binding.btnClearData.setOnClickListener {
            if (isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            executeAction(getString(R.string.action_clear_data), AppAction.CLEAR_DATA) {
                appControlManager?.clearData(appInfo!!.packageName)
            }
        }

        // Uninstall
        binding.btnUninstall.setOnClickListener {
            if (isForceStopOnly || isCritical) {
                showProtectedWarning()
                return@setOnClickListener
            }
            executeAction(getString(R.string.action_uninstall), AppAction.UNINSTALL) {
                appControlManager?.uninstallApp(appInfo!!.packageName)
            }
        }

        // Launch App
        binding.btnLaunchApp.setOnClickListener { launchApp() }

        // Open System Settings
        binding.btnOpenSettings.setOnClickListener { openAppSettings() }
    }

    /**
     * Launch the app if it has a launcher activity.
     */
    private fun launchApp() {
        val packageName = appInfo?.packageName ?: return
        try {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, R.string.error_no_launcher, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_launch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProtectedWarning() {
        Toast.makeText(context, R.string.error_protected_app, Toast.LENGTH_SHORT).show()
    }

    /**
     * Execute an action with loading state and logging.
     */
    private fun executeAction(
        actionName: String,
        action: AppAction,
        actionBlock: suspend () -> Result<Unit>?
    ) {
        if (appControlManager == null) {
            Toast.makeText(context, R.string.error_mode_required_message, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvActionStatus.visibility = View.VISIBLE
        binding.tvActionStatus.text = getString(R.string.action_processing, actionName)
        setButtonsEnabled(false)

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { actionBlock() }
                val success = result?.isSuccess == true

                // Log the action
                appInfo?.let { app ->
                    withContext(Dispatchers.IO) {
                        actionLogger.logAction(
                            ActionLog(
                                action = action,
                                packages = listOf(app.packageName),
                                success = success,
                                errorMessage = if (success) null else result?.exceptionOrNull()?.message
                            )
                        )
                    }
                }

                if (success) {
                    binding.tvActionStatus.text = getString(R.string.action_completed, actionName)
                    binding.tvActionStatus.setTextColor(
                        resources.getColor(R.color.status_positive, null)
                    )
                    onActionCompleted?.invoke()

                    delay(800)
                    dismiss()
                } else {
                    binding.tvActionStatus.text = getString(R.string.action_failed_name, actionName)
                    binding.tvActionStatus.setTextColor(
                        resources.getColor(R.color.status_negative, null)
                    )
                    setButtonsEnabled(true)
                }
            } catch (e: Exception) {
                binding.tvActionStatus.text = e.message ?: getString(R.string.error_unknown)
                binding.tvActionStatus.setTextColor(
                    resources.getColor(R.color.status_negative, null)
                )
                setButtonsEnabled(true)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Execute a background action (restrict/allow) with loading state.
     * Refreshes battery status after completion.
     */
    private fun executeBackgroundAction(
        actionName: String,
        action: AppAction,
        actionBlock: suspend () -> Result<Unit>?
    ) {
        if (batteryManager == null) {
            Toast.makeText(context, R.string.error_mode_required_message, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvActionStatus.visibility = View.VISIBLE
        binding.tvActionStatus.text = getString(R.string.action_processing, actionName)
        setButtonsEnabled(false)

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { actionBlock() }
                val success = result?.isSuccess == true

                // Log the action
                appInfo?.let { app ->
                    withContext(Dispatchers.IO) {
                        actionLogger.logAction(
                            ActionLog(
                                action = action,
                                packages = listOf(app.packageName),
                                success = success,
                                errorMessage = if (success) null else result?.exceptionOrNull()?.message
                            )
                        )
                    }
                }

                if (success) {
                    // Refresh background status display
                    loadBatteryStatus()

                    binding.tvActionStatus.text = getString(R.string.action_completed, actionName)
                    binding.tvActionStatus.setTextColor(
                        resources.getColor(R.color.status_positive, null)
                    )
                    onActionCompleted?.invoke()
                    setButtonsEnabled(true)
                } else {
                    binding.tvActionStatus.text = getString(R.string.action_failed_name, actionName)
                    binding.tvActionStatus.setTextColor(
                        resources.getColor(R.color.status_negative, null)
                    )
                    setButtonsEnabled(true)
                }
            } catch (e: Exception) {
                binding.tvActionStatus.text = e.message ?: getString(R.string.error_unknown)
                binding.tvActionStatus.setTextColor(
                    resources.getColor(R.color.status_negative, null)
                )
                setButtonsEnabled(true)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnForceStop.isEnabled = enabled
        binding.btnToggleEnable.isEnabled = enabled
        binding.btnRestrictBg.isEnabled = enabled
        binding.btnAllowBg.isEnabled = enabled
        binding.btnClearCache.isEnabled = enabled
        binding.btnClearData.isEnabled = enabled
        binding.btnUninstall.isEnabled = enabled
        binding.btnLaunchApp.isEnabled = enabled
        binding.btnOpenSettings.isEnabled = enabled
    }

    private fun setButtonEnabled(button: MaterialButton, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun openAppSettings() {
        val packageName = appInfo?.packageName ?: return
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_open_settings, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(size: Long): String = when {
        size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024))
        size >= 1024 -> String.format("%.2f KB", size / 1024.0)
        else -> "$size B"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
