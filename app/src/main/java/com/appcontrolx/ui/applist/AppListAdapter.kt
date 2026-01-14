package com.appcontrolx.ui.applist

import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcontrolx.R
import com.appcontrolx.data.model.AppInfo
import com.appcontrolx.data.model.AppRunningState
import com.appcontrolx.data.model.AppStatus
import com.appcontrolx.databinding.ItemAppBinding

/**
 * RecyclerView adapter for displaying app list items.
 * 
 * Features:
 * - App icon, name, package name, version, size display (Requirement 5.1)
 * - Running state badges: RUNNING, AWAKENED, STOPPED (Requirements 4.7, 4.8, 4.9)
 * - Background restriction badge (Requirement 5.2)
 * - Protected lock icon for system apps (Requirement 5.3)
 * - Selection mode for batch operations via card selection state
 * - Long press to start selection mode
 * - Info button to show app details
 * 
 * Requirements: 4.7, 4.8, 4.9, 5.1, 5.2, 5.3
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
    
    fun getSelectedApps(): List<AppInfo> = currentList.filter { selectedPackages.contains(it.packageName) }
    
    fun getSelectedCount(): Int = selectedPackages.size
    
    fun isInSelectionMode(): Boolean = isSelectionMode
    
    fun selectAll() {
        isSelectionMode = true
        currentList.forEach { selectedPackages.add(it.packageName) }
        notifyDataSetChanged()
        onSelectionChanged(selectedPackages.size)
    }
    
    fun deselectAll() {
        selectedPackages.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }
    
    fun isAllSelected(): Boolean = selectedPackages.size == currentList.size && currentList.isNotEmpty()
    
    fun isSelected(packageName: String): Boolean = selectedPackages.contains(packageName)
    
    private val appNameCache = mutableMapOf<String, String>()
    
    override fun submitList(list: List<AppInfo>?) {
        appNameCache.clear()
        list?.forEach { appNameCache[it.packageName] = it.appName }
        super.submitList(list)
    }
    
    fun getAppName(packageName: String): String? = appNameCache[packageName]
    
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
    
    private fun startSelection(packageName: String) {
        isSelectionMode = true
        selectedPackages.add(packageName)
        notifyDataSetChanged()
        onSelectionChanged(selectedPackages.size)
    }

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(app: AppInfo) {
            binding.apply {
                val context = root.context
                
                ivAppIcon.setImageDrawable(app.icon)
                ivAppIcon.contentDescription = context.getString(R.string.app_icon_desc)
                tvAppName.text = app.appName
                tvPackageName.text = app.packageName

                
                // Display version (Requirement 5.1)
                tvVersion.text = if (app.versionName.isNotEmpty()) {
                    "v${app.versionName}"
                } else {
                    "v${app.versionCode}"
                }
                
                // Display size (Requirement 5.1)
                tvSize.text = Formatter.formatShortFileSize(context, app.size)
                
                val isSelected = selectedPackages.contains(app.packageName)
                
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
                
                root.alpha = if (app.isEnabled) 1f else 0.6f
                
                bindProtectedIcon(app)
                bindStatusBadges(app)
                
                cardApp.contentDescription = buildAccessibilityDescription(app, isSelected, context)
                
                cardApp.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(app.packageName)
                        notifyItemChanged(adapterPosition)
                    } else {
                        onInfoClick(app)
                    }
                }
                
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
                
                btnInfo.setOnClickListener {
                    onInfoClick(app)
                }
            }
        }
        
        private fun ItemAppBinding.bindProtectedIcon(app: AppInfo) {
            val isProtected = app.isSystemApp && isProtectedPackage(app.packageName)
            ivProtectedIcon.visibility = if (isProtected) View.VISIBLE else View.GONE
        }
        
        private fun isProtectedPackage(packageName: String): Boolean {
            val protectedPrefixes = listOf(
                "com.android.systemui",
                "com.android.settings",
                "com.android.phone",
                "com.android.launcher",
                "com.android.providers",
                "com.google.android.gms",
                "com.google.android.gsf",
                "android"
            )
            return protectedPrefixes.any { packageName.startsWith(it) || packageName == it }
        }

        
        /**
         * Bind status badges based on app running state and restrictions.
         * Requirements: 4.7, 4.8, 4.9, 5.2
         */
        private fun ItemAppBinding.bindStatusBadges(app: AppInfo) {
            statusContainer.visibility = View.VISIBLE
            
            tvStatusRunning.visibility = View.GONE
            tvStatusAwakened.visibility = View.GONE
            tvStatusStopped.visibility = View.GONE
            tvStatusFrozen.visibility = View.GONE
            tvStatusRestricted.visibility = View.GONE
            
            if (app.isFrozen) {
                tvStatusFrozen.visibility = View.VISIBLE
            } else {
                when (app.runningState) {
                    AppRunningState.RUNNING -> {
                        tvStatusRunning.visibility = View.VISIBLE
                    }
                    AppRunningState.AWAKENED -> {
                        tvStatusAwakened.visibility = View.VISIBLE
                    }
                    AppRunningState.STOPPED -> {
                        tvStatusStopped.visibility = View.VISIBLE
                    }
                    AppRunningState.UNKNOWN -> {
                        when {
                            app.isRunning -> tvStatusRunning.visibility = View.VISIBLE
                            app.isStopped -> tvStatusStopped.visibility = View.VISIBLE
                        }
                    }
                }
            }
            
            if (app.isBackgroundRestricted && !app.isFrozen) {
                tvStatusRestricted.visibility = View.VISIBLE
            }
        }
        
        private fun buildAccessibilityDescription(
            app: AppInfo, 
            isSelected: Boolean, 
            context: android.content.Context
        ): String {
            val statusDesc = when (app.runningState) {
                AppRunningState.RUNNING -> context.getString(R.string.a11y_app_running)
                AppRunningState.AWAKENED -> context.getString(R.string.a11y_app_awakened)
                AppRunningState.STOPPED -> context.getString(R.string.a11y_app_stopped)
                AppRunningState.UNKNOWN -> when (app.status) {
                    AppStatus.FROZEN -> context.getString(R.string.a11y_app_frozen)
                    AppStatus.RUNNING -> context.getString(R.string.a11y_app_running)
                    AppStatus.STOPPED -> context.getString(R.string.a11y_app_stopped)
                    AppStatus.RESTRICTED -> context.getString(R.string.a11y_bg_restricted)
                    AppStatus.NORMAL -> ""
                }
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
                   oldItem.versionName == newItem.versionName &&
                   oldItem.runningState == newItem.runningState &&
                   oldItem.size == newItem.size
        }
    }
}
