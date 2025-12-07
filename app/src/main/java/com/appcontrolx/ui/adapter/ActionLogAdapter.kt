package com.appcontrolx.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appcontrolx.R
import com.appcontrolx.databinding.ItemActionLogBinding
import com.appcontrolx.rollback.ActionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionLogAdapter(
    private val onRollbackClick: (ActionLog) -> Unit
) : ListAdapter<ActionLog, ActionLogAdapter.ViewHolder>(DiffCallback()) {
    
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActionLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemActionLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(log: ActionLog) {
            val context = binding.root.context
            
            // Action name
            binding.tvAction.text = log.action.replace("_", " ")
            
            // Package count
            binding.tvPackages.text = context.getString(R.string.log_packages_count, log.packages.size)
            
            // Timestamp
            binding.tvTimestamp.text = dateFormat.format(Date(log.timestamp))
            
            // Status
            if (log.success) {
                binding.tvStatus.text = context.getString(R.string.log_status_success)
                binding.tvStatus.setTextColor(context.getColor(R.color.status_positive))
            } else {
                binding.tvStatus.text = log.message ?: context.getString(R.string.log_status_failed)
                binding.tvStatus.setTextColor(context.getColor(R.color.status_negative))
            }
            
            // Rollback button - only show for RESTRICT_BACKGROUND and FREEZE
            val canRollback = log.action in listOf("RESTRICT_BACKGROUND", "FREEZE")
            binding.btnRollback.visibility = if (canRollback) View.VISIBLE else View.GONE
            binding.btnRollback.setOnClickListener {
                onRollbackClick(log)
            }
            
            // Package list (collapsed by default)
            binding.tvPackageList.text = log.packages.joinToString("\n")
            binding.tvPackageList.visibility = View.GONE
            
            binding.root.setOnClickListener {
                binding.tvPackageList.visibility = 
                    if (binding.tvPackageList.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ActionLog>() {
        override fun areItemsTheSame(oldItem: ActionLog, newItem: ActionLog): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
        
        override fun areContentsTheSame(oldItem: ActionLog, newItem: ActionLog): Boolean {
            return oldItem == newItem
        }
    }
}
