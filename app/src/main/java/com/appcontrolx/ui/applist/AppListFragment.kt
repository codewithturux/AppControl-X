package com.appcontrolx.ui.applist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.appcontrolx.R
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.AppListFilter
import com.appcontrolx.data.model.AppTypeFilter
import com.appcontrolx.data.model.BatchAction
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.databinding.FragmentAppListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment displaying the list of installed applications with search, filter, and batch selection.
 * 
 * Features:
 * - RecyclerView with app items showing icon, name, package, and status badges
 * - Search bar for filtering by name or package name
 * - Filter/Sort bottom sheet for filtering and sorting apps
 * - Selection bar for batch operations
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.6, 3.7, 3.8
 */
@AndroidEntryPoint
class AppListFragment : Fragment(), FilterSortCallback {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppListViewModel by viewModels()
    
    private lateinit var adapter: AppListAdapter
    
    // Track progress dialog for batch operations
    private var progressDialog: BatchProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        setupFilterButton()
        setupSelectionBar()
        setupSelectAll()
        setupSwipeRefresh()
        observeUiState()
        
        // Initial load
        viewModel.loadApps()
    }

    /**
     * FilterSortCallback implementation.
     * Called when user applies filter/sort from bottom sheet.
     * Requirements: 3.6, 3.7, 3.8
     */
    override fun onFilterSortChanged(filter: AppListFilter) {
        viewModel.onFilterSortChanged(filter)
        updateFilterButtonText(filter)
    }


    /**
     * Setup RecyclerView with adapter.
     * Requirement: 8.1
     */
    private fun setupRecyclerView() {
        adapter = AppListAdapter(
            onSelectionChanged = { selectedCount ->
                viewModel.onSelectionChanged(selectedCount)
            },
            onInfoClick = { app ->
                showAppDetail(app)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
    }

    /**
     * Setup search functionality with real-time filtering.
     * Requirement: 8.3
     */
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onSearchQueryChanged(s?.toString()?.trim() ?: "")
            }
        })
        
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Search is already applied via TextWatcher
                true
            } else false
        }
    }

    /**
     * Setup filter button to show FilterSortBottomSheet.
     * Requirements: 3.3, 3.8
     */
    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            val currentFilter = viewModel.uiState.value.appListFilter
            FilterSortBottomSheet.show(
                fragmentManager = childFragmentManager,
                currentFilter = currentFilter,
                callback = this
            )
        }
    }

    /**
     * Update filter button text based on current filter.
     * Shows app type filter (User/System/All) as the primary label.
     * Requirement: 3.8
     */
    private fun updateFilterButtonText(filter: AppListFilter) {
        binding.btnFilter.text = filter.appTypeFilter.displayName
    }

    /**
     * Setup selection bar for batch operations.
     * Requirement: 8.6 (batch mode)
     */
    private fun setupSelectionBar() {
        binding.btnCloseSelection.setOnClickListener {
            adapter.deselectAll()
            viewModel.onSelectionChanged(0)
        }
        
        binding.btnAction.setOnClickListener {
            showActionSheet()
        }
    }

    /**
     * Setup select all button.
     */
    private fun setupSelectAll() {
        binding.btnSelectAll.setOnClickListener {
            if (adapter.isAllSelected()) {
                adapter.deselectAll()
            } else {
                adapter.selectAll()
            }
            updateSelectAllButton()
        }
    }

    /**
     * Setup swipe-to-refresh.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshApps()
        }
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.tertiary
        )
    }


    /**
     * Observe ViewModel UI state and update UI accordingly.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                    handleBatchExecutionState(state.batchExecutionState)
                }
            }
        }
    }

    /**
     * Update UI based on current state.
     */
    private fun updateUi(state: AppListUiState) {
        // Update filter button text
        updateFilterButtonText(state.appListFilter)
        
        // Update loading state
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.swipeRefresh.isRefreshing = state.isRefreshing
        
        // Update app list
        if (state.filteredApps.isEmpty() && !state.isLoading) {
            showEmptyState(state)
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.submitList(state.filteredApps)
            binding.tvAppCount.text = getString(R.string.app_count, state.filteredApps.size)
        }
        
        // Update selection bar
        updateSelectionBar(state.selectedCount, state.executionMode)
        
        // Update error state
        state.error?.let { error ->
            showErrorState(error)
        }
    }

    /**
     * Update selection bar visibility and state.
     */
    private fun updateSelectionBar(selectedCount: Int, mode: ExecutionMode) {
        if (selectedCount > 0) {
            binding.selectionBar.visibility = View.VISIBLE
            binding.tvSelectedCount.text = getString(R.string.selected_count, selectedCount)
            
            binding.btnAction.isEnabled = mode != ExecutionMode.None
            if (mode == ExecutionMode.None) {
                binding.btnAction.text = getString(R.string.mode_required)
            } else {
                binding.btnAction.text = getString(R.string.action_execute)
            }
        } else {
            binding.selectionBar.visibility = View.GONE
        }
        updateSelectAllButton()
    }

    /**
     * Update select all button text.
     */
    private fun updateSelectAllButton() {
        binding.btnSelectAll.text = if (adapter.isAllSelected()) {
            getString(R.string.btn_deselect_all)
        } else {
            getString(R.string.btn_select_all)
        }
    }

    /**
     * Show empty state based on current filters.
     */
    private fun showEmptyState(state: AppListUiState) {
        binding.recyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
        
        if (state.searchQuery.isNotBlank()) {
            binding.tvEmptyTitle.text = getString(R.string.search_no_results)
            binding.tvEmptyMessage.text = "\"${state.searchQuery}\""
            binding.btnRetry.visibility = View.GONE
        } else {
            binding.tvEmptyTitle.text = getString(R.string.empty_no_apps_title)
            binding.tvEmptyMessage.text = getString(R.string.empty_no_apps_message)
            binding.btnRetry.visibility = View.GONE
        }
    }

    /**
     * Show error state with retry option.
     */
    private fun showErrorState(error: String) {
        binding.recyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = getString(R.string.error_load_title)
        binding.tvEmptyMessage.text = error
        binding.btnRetry.visibility = View.VISIBLE
        binding.btnRetry.setOnClickListener {
            viewModel.loadApps()
        }
    }

    /**
     * Show app detail bottom sheet.
     * Requirement: 9.1
     */
    private fun showAppDetail(app: AppInfo) {
        val bottomSheet = AppDetailBottomSheet.newInstance(app)
        bottomSheet.onActionCompleted = {
            // Refresh app list when action is completed
            viewModel.refreshApps()
        }
        bottomSheet.show(childFragmentManager, AppDetailBottomSheet.TAG)
    }

    /**
     * Show action sheet for batch operations.
     * Creates and shows ActionBottomSheet with selected apps.
     * 
     * Requirements: 1.1, 1.6
     */
    private fun showActionSheet() {
        val selectedApps = adapter.getSelectedApps()
        if (selectedApps.isEmpty()) return
        
        // Requirement 1.6: Show error dialog if execution mode is None
        if (viewModel.uiState.value.executionMode == ExecutionMode.None) {
            showModeRequiredDialog()
            return
        }
        
        // Requirement 1.1: Display ActionBottomSheet when user taps Execute
        ActionBottomSheet.show(
            fragmentManager = childFragmentManager,
            selectedApps = selectedApps,
            onActionSelected = { action, apps ->
                executeBatchAction(action, apps)
            }
        )
    }
    
    /**
     * Execute batch action on selected apps.
     * Delegates to ViewModel for actual execution.
     * 
     * Requirements: 2.1
     */
    private fun executeBatchAction(action: BatchAction, apps: List<AppInfo>) {
        viewModel.executeBatchAction(action, apps)
    }
    
    /**
     * Handle batch execution state changes.
     * Shows progress dialog during execution and result dialog on completion.
     * 
     * Requirements: 2.2, 3.4, 3.5
     */
    private fun handleBatchExecutionState(state: BatchExecutionState) {
        when (state) {
            is BatchExecutionState.Idle -> {
                // Dismiss progress dialog if showing
                progressDialog?.dismiss()
                progressDialog = null
            }
            
            is BatchExecutionState.Executing -> {
                // Show or update progress dialog (Requirement 2.2)
                if (progressDialog == null) {
                    progressDialog = BatchProgressDialog.show(
                        fragmentManager = childFragmentManager,
                        action = state.action,
                        totalCount = state.totalCount,
                        onCancelled = {
                            // Cancel is not fully implemented - just dismiss
                            viewModel.resetBatchExecutionState()
                        }
                    )
                } else {
                    progressDialog?.updateProgress(state.currentIndex, state.currentPackageName)
                }
            }
            
            is BatchExecutionState.Completed -> {
                // Dismiss progress dialog
                progressDialog?.dismiss()
                progressDialog = null
                
                // Show result dialog (Requirement 3.4)
                BatchResultDialog.show(
                    fragmentManager = childFragmentManager,
                    result = state.result,
                    onDismissed = {
                        // Refresh app list after completion (Requirement 3.4)
                        viewModel.refreshApps()
                        
                        // Clear selection after success (Requirement 3.5)
                        adapter.deselectAll()
                        viewModel.onSelectionChanged(0)
                        
                        // Reset batch execution state
                        viewModel.resetBatchExecutionState()
                    }
                )
            }
            
            is BatchExecutionState.Error -> {
                // Dismiss progress dialog
                progressDialog?.dismiss()
                progressDialog = null
                
                // Show error dialog
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_batch_title)
                    .setMessage(state.message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.resetBatchExecutionState()
                    }
                    .show()
            }
        }
    }

    /**
     * Show dialog when mode is required for actions.
     */
    private fun showModeRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_mode_required_title)
            .setMessage(R.string.error_mode_required_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.registerPackageReceiver()
    }

    override fun onPause() {
        super.onPause()
        viewModel.unregisterPackageReceiver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
