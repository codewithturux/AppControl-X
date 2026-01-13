package com.appcontrolx.ui.applist

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcontrolx.R
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.AppStatus
import com.appcontrolx.databinding.ItemAppBinding

/**
 * RecyclerView adapter for displaying app list items.
 * 
 * Features:
 * - App icon, name, package name display (Requirement 8.1)
 * - Status badges: frozen, running, stopped, restricted (Requirement 8.2)
 * - Selection mode for batch operations via card selection state
 * - Long press to start selection mode (Requirement 8.6)
 * - Info button to show app details
 * 
 * Requirements: 8.1, 8.2
 */
class AppListAdapter(
    private val onSelectionChanged: (Int) -> Unit,
    private val onInfoClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {
    
    private val selectedPackages = mutableSetOf<String>()
    private var isSelectionMode = false
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    /**
     * Get list of currently selected apps.
     */
    fun getSelectedApps(): List<AppInfo> = currentList.filter { selectedPackages.contains(it.packageName) }
    
    /**
     * Get count of selected apps.
     */
    fun getSelectedCount(): Int = selectedPackages.size
    
    /**
     * Check if in selection mode.
     */
    fun isInSelectionMode(): Boolean = isSelectionMode
    
    /**
     * Select all visible apps.
     */
    fun selectAll() {
        isSelectionMode = true
        currentList.forEach { selectedPackages.add(it.packageName) }
        notifyDataSetChanged()
        onSelectionChanged(selectedPackages.size)
    }
    
    /**
     * Deselect all apps and exit selection mode.
     */
    fun deselectAll() {
        selectedPackages.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }
    
    /**
     * Check if all visible apps are selected.
     */
    fun isAllSelected(): Boolean = selectedPackages.size == currentList.size && currentList.isNotEmpty()
    
    /**
     * Check if a specific app is selected.
     */
    fun isSelected(packageName: String): Boolean = selectedPackages.contains(packageName)
    
    // Cache for O(1) lookup of app names by package name
    private val appNameCache = mutableMapOf<String, String>()
    
    override fun submitList(list: List<AppInfo>?) {
        // Build cache when list is submitted for quick lookups
        appNameCache.clear()
        list?.forEach { appNameCache[it.packageName] = it.appName }
        super.submitList(list)
    }
    
    /**
     * Get app name by package name from cache.
     * Useful for displaying app names in batch operations.
     */
    fun getAppName(packageName: String): String? {
        return appNameCache[packageName]
    }
    
    /**
     * Toggle selection state for a package.
     */
    private fun toggleSelection(packageName: String) {
        if (selectedPackages.contains(packageName)) {
            selectedPackages.remove(packageName)
            if (selectedPackages.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedPackages.add(packageName)
            isSelectionMode = true
        }
        onSelectionChanged(selectedPackages.size)
    }
    
    /**
     * Start selection mode with the given package selected.
     */
    private fun startSelection(packageName: String) {
        isSelectionMode = true
        selectedPackages.add(packageName)
        notifyDataSetChanged()
        onSelectionChanged(selectedPackages.size)
    }

    /**
     * ViewHolder for app list items.
     * 
     * Displays:
     * - App icon (Requirement 8.1)
     * - App name (Requirement 8.1)
     * - Package name (Requirement 8.1)
     * - Status badges: frozen, running, stopped, restricted (Requirement 8.2)
     * - Selection state via card appearance
     * - Info button for app details
     */
    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(app: AppInfo) {
            binding.apply {
                val context = root.context
                
                // Requirement 8.1: Display app icon, name, and package name
                ivAppIcon.setImageDrawable(app.icon)
                ivAppIcon.contentDescription = context.getString(R.string.app_icon_desc)
                tvAppName.text = app.appName
                tvPackageName.text = app.packageName
                
                val isSelected = selectedPackages.contains(app.packageName)
                
                // Visual selection state - change card appearance to indicate selection
                cardApp.isChecked = isSelected
                cardApp.strokeWidth = if (isSelected) 
                    context.resources.getDimensionPixelSize(R.dimen.stroke_selected) 
                    else context.resources.getDimensionPixelSize(R.dimen.stroke_normal)
                cardApp.strokeColor = if (isSelected)
                    context.getColor(R.color.primary)
                    else context.getColor(R.color.outline)
                cardApp.setCardBackgroundColor(
                    if (isSelected) context.getColor(R.color.selected_bg)
                    else context.getColor(R.color.surface)
                )
                
                // Disabled/Frozen state - dim the item for visual feedback
                root.alpha = if (app.isEnabled) 1f else 0.6f
                
                // Requirement 8.2: Show status badges for frozen, running, stopped, restricted
                bindStatusBadges(app)
                
                // Set accessibility content description for the entire item
                cardApp.contentDescription = buildAccessibilityDescription(app, isSelected, context)
                
                // Tap: if in selection mode -> toggle selection, else -> show app info
                cardApp.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(app.packageName)
                        notifyItemChanged(adapterPosition)
                    } else {
                        onInfoClick(app)
                    }
                }
                
                // Long press: start selection mode (Requirement 8.6)
                cardApp.setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (!isSelectionMode) {
                        startSelection(app.packageName)
                    } else {
                        toggleSelection(app.packageName)
                        notifyItemChanged(adapterPosition)
                    }
                    true
                }
                
                // Info button always shows app details
                btnInfo.setOnClickListener {
                    onInfoClick(app)
                }
            }
        }
        
        /**
         * Bind status badges based on app state.
         * 
         * Badge priority and display logic:
         * - FROZEN: App is disabled, shown with blue badge
         * - RUNNING: App is active (not frozen and not stopped), shown with green badge
         * - STOPPED: App is stopped but not frozen, shown with gray badge
         * - RESTRICTED: Background restricted (can coexist with running), shown with orange badge
         * 
         * Requirement 8.2
         */
        private fun ItemAppBinding.bindStatusBadges(app: AppInfo) {
            statusContainer.visibility = View.VISIBLE
            
            // Determine running state: not frozen and not stopped means running
            val isRunning = !app.isFrozen && !app.isStopped
            
            // Frozen badge - highest priority, shown when app is disabled
            tvStatusFrozen.visibility = if (app.isFrozen) View.VISIBLE else View.GONE
            
            // Running badge - shown when app is active (not frozen, not stopped)
            tvStatusRunning.visibility = if (isRunning) View.VISIBLE else View.GONE
            
            // Stopped badge - shown when app is stopped but not frozen
            tvStatusStopped.visibility = if (app.isStopped && !app.isFrozen) View.VISIBLE else View.GONE
            
            // Restricted badge - shown when background is restricted (can coexist with running)
            // Not shown when frozen since frozen apps can't run in background anyway
            tvStatusRestricted.visibility = if (app.isBackgroundRestricted && !app.isFrozen) View.VISIBLE else View.GONE
        }
        
        /**
         * Build accessibility content description for the app item.
         */
        private fun buildAccessibilityDescription(
            app: AppInfo, 
            isSelected: Boolean, 
            context: android.content.Context
        ): String {
            val statusDesc = when (app.status) {
                AppStatus.FROZEN -> context.getString(R.string.a11y_app_frozen)
                AppStatus.RUNNING -> context.getString(R.string.a11y_app_running)
                AppStatus.STOPPED -> context.getString(R.string.a11y_app_stopped)
                AppStatus.RESTRICTED -> context.getString(R.string.a11y_bg_restricted)
                AppStatus.NORMAL -> ""
            }
            
            val selectionDesc = if (isSelected) {
                context.getString(R.string.a11y_app_selected, app.appName)
            } else {
                app.appName
            }
            
            return if (statusDesc.isNotEmpty()) {
                "$selectionDesc. $statusDesc"
            } else {
                selectionDesc
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates.
     * Compares items by package name and content by relevant state fields.
     */
    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName && 
                   oldItem.isEnabled == newItem.isEnabled &&
                   oldItem.isRunning == newItem.isRunning &&
                   oldItem.isStopped == newItem.isStopped &&
                   oldItem.isBackgroundRestricted == newItem.isBackgroundRestricted &&
                   oldItem.appName == newItem.appName &&
                   oldItem.versionName == newItem.versionName
        }
    }
}
