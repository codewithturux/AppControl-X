package com.appcontrolx.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
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
import com.appcontrolx.utils.Constants
import com.appcontrolx.utils.SafetyValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListFragment : Fragment() {
    
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: AppListAdapter
    private lateinit var appFetcher: AppFetcher
    private var executor: CommandExecutor? = null
    private var policyManager: BatteryPolicyManager? = null
    private var rollbackManager: RollbackManager? = null
    
    private var showSystemApps = false
    private var executionMode: ExecutionMode = ExecutionMode.None
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupExecutionMode()
        appFetcher = AppFetcher(requireContext())
        
        setupRecyclerView()
        setupChips()
        setupFab()
        loadApps()
    }
    
    private fun setupExecutionMode() {
        executionMode = PermissionBridge().detectMode()
        
        if (executionMode is ExecutionMode.Root) {
            executor = RootExecutor()
            policyManager = BatteryPolicyManager(executor!!)
            rollbackManager = RollbackManager(requireContext(), executor!!)
        }
        // TODO: Add Shizuku executor
    }
    
    private fun setupRecyclerView() {
        adapter = AppListAdapter { _ ->
            updateFabVisibility()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupChips() {
        binding.chipUserApps.isChecked = true
        
        binding.chipUserApps.setOnClickListener {
            showSystemApps = false
            binding.chipUserApps.isChecked = true
            binding.chipSystemApps.isChecked = false
            loadApps()
        }
        
        binding.chipSystemApps.setOnClickListener {
            showSystemApps = true
            binding.chipSystemApps.isChecked = true
            binding.chipUserApps.isChecked = false
            loadApps()
        }
    }
    
    private fun setupFab() {
        binding.fabAction.visibility = View.GONE
        binding.fabAction.setOnClickListener {
            showActionSheet()
        }
    }
    
    private fun updateFabVisibility() {
        val selectedCount = adapter.getSelectedApps().size
        binding.fabAction.visibility = if (selectedCount > 0 && executionMode !is ExecutionMode.None) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    private fun showActionSheet() {
        val selectedApps = adapter.getSelectedApps()
        if (selectedApps.isEmpty()) return
        
        val bottomSheet = ActionBottomSheet.newInstance(selectedApps.size)
        bottomSheet.onActionSelected = { action ->
            handleAction(action, selectedApps)
        }
        bottomSheet.show(childFragmentManager, ActionBottomSheet.TAG)
    }
    
    private fun handleAction(action: ActionBottomSheet.Action, apps: List<AppInfo>) {
        val packages = apps.map { it.packageName }
        
        // Validate safety
        val validation = SafetyValidator.validate(packages)
        if (!validation.canProceed) {
            showBlockedWarning(validation.blocked)
            return
        }
        
        if (validation.warnings.isNotEmpty()) {
            showWarningDialog(validation.warnings) {
                executeAction(action, validation.safe)
            }
        } else {
            executeAction(action, packages)
        }
    }
    
    private fun executeAction(action: ActionBottomSheet.Action, packages: List<String>) {
        val pm = policyManager ?: return
        val rm = rollbackManager
        
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            
            // Save snapshot before action
            rm?.saveSnapshot(packages)
            
            val results = withContext(Dispatchers.IO) {
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
            
            binding.progressBar.visibility = View.GONE
            
            val failCount = results.count { it.second.isFailure }
            
            // Log action
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
            
            // Reload apps
            loadApps()
        }
    }
    
    private fun showBlockedWarning(blocked: List<String>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Blocked")
            .setMessage("Cannot perform action on critical system apps:\n${blocked.joinToString("\n")}")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun showWarningDialog(warnings: List<String>, onProceed: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning")
            .setMessage("The following apps may affect system functionality:\n${warnings.joinToString("\n")}\n\nProceed anyway?")
            .setPositiveButton(R.string.confirm_yes) { _, _ -> onProceed() }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }
    
    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                if (showSystemApps) appFetcher.getSystemApps()
                else appFetcher.getUserApps()
            }
            
            adapter.submitList(apps)
            binding.progressBar.visibility = View.GONE
            binding.tvAppCount.text = "${apps.size} apps"
            updateFabVisibility()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
