package com.appcontrolx.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcontrolx.R
import com.appcontrolx.data.model.ActionLog
import com.appcontrolx.databinding.ItemActionLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying action history in a RecyclerView.
 * Shows rollback button only for the first (most recent) item when rollback is available.
 * 
 * Requirements: 7.3, 7.4, 7.6 - Display actions in reverse chronological order with rollback for last action
 */
class ActionHistoryAdapter(
    private val onRollbackClick: ((ActionLog) -> Unit)? = null
) : ListAdapter<ActionLog, ActionHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    /** Whether rollback is available (snapshot exists) */
    var rollbackAvailable: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                // Notify first item changed to update rollback button visibility
                if (itemCount > 0) {
                    notifyItemChanged(0)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActionLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: ItemActionLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: ActionLog, position: Int) {
            val context = binding.root.context

            // Action name - format enum to readable text
            binding.tvAction.text = formatActionName(log.action.name)

            // Package count
            binding.tvPackages.text = context.getString(R.string.log_packages_count, log.packages.size)

            // Timestamp
            binding.tvTimestamp.text = dateFormat.format(Date(log.timestamp))

            // Status
            if (log.success) {
                binding.tvStatus.text = context.getString(R.string.log_status_success)
                binding.tvStatus.setTextColor(context.getColor(R.color.status_positive))
                binding.tvStatus.contentDescription = context.getString(R.string.a11y_action_success)
            } else {
                binding.tvStatus.text = log.errorMessage ?: context.getString(R.string.log_status_failed)
                binding.tvStatus.setTextColor(context.getColor(R.color.status_negative))
                binding.tvStatus.contentDescription = context.getString(R.string.a11y_action_failed)
            }

            // Show rollback button only for the first item (most recent action) 
            // when rollback is available and the action was successful
            // Requirements: 7.3, 7.6 - Rollback only available for most recent batch action with saved snapshot
            val showRollback = position == 0 && rollbackAvailable && log.success && onRollbackClick != null
            binding.btnRollback.visibility = if (showRollback) View.VISIBLE else View.GONE
            
            if (showRollback) {
                binding.btnRollback.setOnClickListener {
                    onRollbackClick?.invoke(log)
                }
            }

            // Package list (collapsed by default, expandable on click)
            binding.tvPackageList.text = log.packages.joinToString("\n")
            binding.tvPackageList.visibility = View.GONE

            // Toggle package list visibility on click
            binding.root.setOnClickListener {
                binding.tvPackageList.visibility =
                    if (binding.tvPackageList.visibility == View.GONE) View.VISIBLE else View.GONE
            }
            
            // Set accessibility content description for the entire item
            val statusDesc = if (log.success) {
                context.getString(R.string.a11y_action_success)
            } else {
                context.getString(R.string.a11y_action_failed)
            }
            binding.root.contentDescription = "${formatActionName(log.action.name)}, " +
                "${context.getString(R.string.log_packages_count, log.packages.size)}, " +
                "${dateFormat.format(Date(log.timestamp))}, $statusDesc"
        }

        /**
         * Format action enum name to readable text.
         * e.g., RESTRICT_BACKGROUND -> Restrict Background
         */
        private fun formatActionName(name: String): String {
            return name.replace("_", " ")
                .lowercase()
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ActionLog>() {
        override fun areItemsTheSame(oldItem: ActionLog, newItem: ActionLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActionLog, newItem: ActionLog): Boolean {
            return oldItem == newItem
        }
    }
}
