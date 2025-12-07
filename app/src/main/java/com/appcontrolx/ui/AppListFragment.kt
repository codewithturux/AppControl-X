package com.appcontrolx.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcontrolx.R
import com.appcontrolx.databinding.FragmentAppListBinding
import com.appcontrolx.executor.CommandExecutor
import com.appcontrolx.executor.RootExecutor
import com.appcontrolx.model.AppInfo
import com.appcontrolx.model.ExecutionMode
import com.appcontrolx.rollback.ActionLog
import com.appcontrolx.rollback.RollbackManager
import com.appcontrolx.service.AppFetcher
import com.appcontrolx.service.BatteryPolicyManager
import com.appcontrolx.service.PermissionBridge
import com.appcontrolx.ui.adapter.AppListAdapter
import com.appcontrolx.utils.SafetyValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AppListFragment : Fragment() {
    
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding
    
    private lateinit var adapter: AppListAdapter
    private lateinit var appFetcher: AppFetcher
    private var executor: CommandExecutor? = null
    private var policyManager: BatteryPolicyManager? = null
    private var rollbackManager: RollbackManager? = null
    
    private var showSystemApps = false
    private var executionMode: ExecutionMode = ExecutionMode.None
    
    // App cache
    private var cachedUserApps: List<AppInfo>? = null
    private var cachedSystemApps: List<AppInfo>? = null
    private var lastCacheTime = 0L
    
    // Package change receiver
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED -> {
                    clearCache()
                    if (_binding != null) loadApps()
                }
            }
        }
    }

    companion object {
        private const val LOAD_TIMEOUT_MS = 30000L
        private const val ACTION_TIMEOUT_MS = 60000L
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return _binding!!.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupExecutionMode()
        appFetcher = AppFetcher(requireContext())
        
        setupHeader()
        setupRecyclerView()
        setupSwipeRefresh()
        setupChips()
        setupSelectionBar()
        setupSelectAll()
        registerPackageReceiver()
        loadApps()
    }
    
    private fun setupExecutionMode() {
        executionMode = PermissionBridge().detectMode()
        
        if (executionMode is ExecutionMode.Root) {
            executor = RootExecutor()
            policyManager = BatteryPolicyManager(executor!!)
            rollbackManager = RollbackManager(requireContext(), executor!!)
        }
    }
    
    private fun setupHeader() {
        val b = binding ?: return
        val modeText = when (executionMode) {
            is ExecutionMode.Root -> "ROOT"
            is ExecutionMode.Shizuku -> "SHIZUKU"
            else -> "VIEW ONLY"
        }
        b.tvModeIndicator.text = modeText
    }
    
    private fun setupRecyclerView() {
        val b = binding ?: return
        adapter = AppListAdapter(
            onSelectionChanged = { selectedCount ->
                updateSelectionUI(selectedCount)
            },
            onInfoClick = { app ->
                showAppDetail(app)
            }
        )
        b.recyclerView.layoutManager = LinearLayoutManager(context)
        b.recyclerView.adapter = adapter
        b.recyclerView.setHasFixedSize(true)
    }
    
    private fun setupSwipeRefresh() {
        val b = binding ?: return
        b.swipeRefresh.setOnRefreshListener {
            clearCache()
            loadApps()
        }
        b.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.tertiary
        )
    }
    
    private fun showAppDetail(app: AppInfo) {
        val bottomSheet = AppDetailBottomSheet.newInstance(app)
        bottomSheet.onActionCompleted = {
            clearCache()
            loadApps()
        }
        bottomSheet.show(childFragmentManager, AppDetailBottomSheet.TAG)
    }
    
    private fun setupChips() {
        val b = binding ?: return
        b.chipUserApps.isChecked = true
        
        b.chipUserApps.setOnClickListener {
            showSystemApps = false
            b.chipUserApps.isChecked = true
            b.chipSystemApps.isChecked = false
            adapter.deselectAll()
            loadApps()
        }
        
        b.chipSystemApps.setOnClickListener {
            showSystemApps = true
            b.chipSystemApps.isChecked = true
            b.chipUserApps.isChecked = false
            adapter.deselectAll()
            loadApps()
        }
    }
    
    private fun setupSelectionBar() {
        val b = binding ?: return
        b.btnCloseSelection.setOnClickListener {
            adapter.deselectAll()
        }
        
        b.btnAction.setOnClickListener {
            showActionSheet()
        }
    }
    
    private fun setupSelectAll() {
        val b = binding ?: return
        b.btnSelectAll.setOnClickListener {
            if (adapter.isAllSelected()) {
                adapter.deselectAll()
            } else {
                adapter.selectAll()
            }
            updateSelectAllButton()
        }
    }

    private fun updateSelectionUI(selectedCount: Int) {
        val b = binding ?: return
        
        if (selectedCount > 0) {
            b.selectionBar.visibility = View.VISIBLE
            b.tvSelectedCount.text = getString(R.string.selected_count, selectedCount)
            
            b.btnAction.isEnabled = executionMode !is ExecutionMode.None
            if (executionMode is ExecutionMode.None) {
                b.btnAction.text = getString(R.string.mode_required)
            } else {
                b.btnAction.text = getString(R.string.action_execute)
            }
        } else {
            b.selectionBar.visibility = View.GONE
        }
        updateSelectAllButton()
    }
    
    private fun updateSelectAllButton() {
        val b = binding ?: return
        b.btnSelectAll.text = if (adapter.isAllSelected()) {
            getString(R.string.btn_deselect_all)
        } else {
            getString(R.string.btn_select_all)
        }
    }
    
    private fun showActionSheet() {
        val selectedApps = adapter.getSelectedApps()
        if (selectedApps.isEmpty()) return
        
        if (executionMode is ExecutionMode.None) {
            showModeRequiredDialog()
            return
        }
        
        val bottomSheet = ActionBottomSheet.newInstance(selectedApps.size)
        bottomSheet.onActionSelected = { action ->
            handleAction(action, selectedApps)
        }
        bottomSheet.show(childFragmentManager, ActionBottomSheet.TAG)
    }
    
    private fun showModeRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_mode_required_title)
            .setMessage(R.string.error_mode_required_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun handleAction(action: ActionBottomSheet.Action, apps: List<AppInfo>) {
        val packages = apps.map { it.packageName }
        
        val validation = SafetyValidator.validate(packages)
        if (!validation.canProceed) {
            showBlockedWarning(validation.blocked)
            return
        }
        
        val filteredPackages = if (action != ActionBottomSheet.Action.FORCE_STOP) {
            val forceStopOnly = packages.filter { SafetyValidator.isForceStopOnly(it) }
            if (forceStopOnly.isNotEmpty()) {
                showForceStopOnlyWarning(forceStopOnly)
            }
            packages.filter { !SafetyValidator.isForceStopOnly(it) }
        } else {
            packages
        }
        
        if (filteredPackages.isEmpty()) {
            Toast.makeText(context, R.string.error_no_valid_apps, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (validation.warnings.isNotEmpty()) {
            showWarningDialog(validation.warnings) {
                executeAction(action, filteredPackages.filter { it !in validation.warnings })
            }
        } else {
            executeAction(action, filteredPackages)
        }
    }
    
    private fun showForceStopOnlyWarning(packages: List<String>) {
        Toast.makeText(context, 
            getString(R.string.warning_force_stop_only, packages.size), 
            Toast.LENGTH_LONG).show()
    }

    private fun executeAction(action: ActionBottomSheet.Action, packages: List<String>) {
        val pm = policyManager ?: return
        val rm = rollbackManager
        val b = binding ?: return
        
        lifecycleScope.launch {
            b.progressBar.visibility = View.VISIBLE
            
            try {
                rm?.saveSnapshot(packages)
                
                val results = withTimeout(ACTION_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        packages.map { pkg ->
                            val result = when (action) {
                                ActionBottomSheet.Action.FREEZE -> pm.freezeApp(pkg)
                                ActionBottomSheet.Action.UNFREEZE -> pm.unfreezeApp(pkg)
                                ActionBottomSheet.Action.UNINSTALL -> pm.uninstallApp(pkg)
                                ActionBottomSheet.Action.FORCE_STOP -> pm.forceStop(pkg)
                                ActionBottomSheet.Action.RESTRICT_BACKGROUND -> pm.restrictBackground(pkg)
                                ActionBottomSheet.Action.ALLOW_BACKGROUND -> pm.allowBackground(pkg)
                            }
                            pkg to result
                        }
                    }
                }
                
                val failCount = results.count { it.second.isFailure }
                
                rm?.logAction(ActionLog(
                    action = action.name,
                    packages = packages,
                    success = failCount == 0,
                    message = if (failCount > 0) "$failCount failed" else null
                ))
                
                if (failCount == 0) {
                    Toast.makeText(context, R.string.action_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, getString(R.string.action_failed, "$failCount apps"), Toast.LENGTH_SHORT).show()
                }
                
                adapter.deselectAll()
                clearCache()
                loadApps()
                
            } catch (e: TimeoutCancellationException) {
                showErrorDialog(
                    getString(R.string.error_timeout_title),
                    getString(R.string.error_timeout_message)
                )
            } catch (e: Exception) {
                showErrorDialog(
                    getString(R.string.error_action_title),
                    e.message ?: getString(R.string.error_unknown)
                )
            } finally {
                binding?.progressBar?.visibility = View.GONE
            }
        }
    }
    
    private fun showBlockedWarning(blocked: List<String>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_blocked_title)
            .setMessage(getString(R.string.error_blocked_message, blocked.joinToString("\n")))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun showWarningDialog(warnings: List<String>, onProceed: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.warning_title)
            .setMessage(getString(R.string.warning_system_apps, warnings.joinToString("\n")))
            .setPositiveButton(R.string.confirm_yes) { _, _ -> onProceed() }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun showErrorDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun clearCache() {
        cachedUserApps = null
        cachedSystemApps = null
        lastCacheTime = 0L
    }
    
    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - lastCacheTime < CACHE_DURATION_MS
    }

    private fun loadApps() {
        val b = binding ?: return
        
        // Check cache first
        val cachedApps = if (showSystemApps) cachedSystemApps else cachedUserApps
        if (cachedApps != null && isCacheValid()) {
            displayApps(cachedApps)
            b.swipeRefresh.isRefreshing = false
            return
        }
        
        b.progressBar.visibility = View.VISIBLE
        b.emptyState.visibility = View.GONE
        b.recyclerView.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val apps = withTimeout(LOAD_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        if (showSystemApps) appFetcher.getSystemApps()
                        else appFetcher.getUserApps()
                    }
                }
                
                // Cache the results
                if (showSystemApps) {
                    cachedSystemApps = apps
                } else {
                    cachedUserApps = apps
                }
                lastCacheTime = System.currentTimeMillis()
                
                displayApps(apps)
                
            } catch (e: TimeoutCancellationException) {
                showEmptyState(
                    getString(R.string.error_timeout_title),
                    getString(R.string.error_load_timeout_message),
                    showRetry = true
                )
            } catch (e: Exception) {
                showEmptyState(
                    getString(R.string.error_load_title),
                    e.message ?: getString(R.string.error_unknown),
                    showRetry = true
                )
            } finally {
                binding?.progressBar?.visibility = View.GONE
                binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }
    
    private fun displayApps(apps: List<AppInfo>) {
        val b = binding ?: return
        
        if (apps.isEmpty()) {
            showEmptyState(
                getString(R.string.empty_no_apps_title),
                getString(R.string.empty_no_apps_message)
            )
        } else {
            adapter.submitList(apps)
            b.tvAppCount.text = getString(R.string.app_count, apps.size)
        }
    }
    
    private fun showEmptyState(title: String, message: String, showRetry: Boolean = false) {
        val b = binding ?: return
        
        b.recyclerView.visibility = View.GONE
        b.emptyState.visibility = View.VISIBLE
        b.tvEmptyTitle.text = title
        b.tvEmptyMessage.text = message
        b.btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
        b.btnRetry.setOnClickListener { loadApps() }
    }
    
    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        requireContext().registerReceiver(packageReceiver, filter)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        _binding = null
    }
}
