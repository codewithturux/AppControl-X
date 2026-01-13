package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.StorageInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying internal storage usage with circular progress.
 * 
 * Features:
 * - Circular progress indicator showing percentage
 * - Used storage display
 * - Total storage display
 * - DevCheck-style design
 * 
 * Requirements: 0.1.5 - Storage card showing used, total, and percentage for internal storage
 */
class StorageCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val progressStorage: ProgressBar
    private val tvPercent: TextView
    private val tvUsed: TextView
    private val tvTotal: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_storage, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        progressStorage = findViewById(R.id.progressStorage)
        tvPercent = findViewById(R.id.tvStoragePercent)
        tvUsed = findViewById(R.id.tvStorageUsed)
        tvTotal = findViewById(R.id.tvStorageTotal)
    }

    /**
     * Update the card with storage information.
     */
    fun update(storage: StorageInfo) {
        val percent = storage.usedPercent.toInt()
        
        // Update circular progress
        progressStorage.progress = percent
        
        // Update percentage text in center
        tvPercent.text = context.getString(R.string.dashboard_percent_format, percent)
        
        // Update used and total text
        tvUsed.text = context.getString(R.string.dashboard_used_format, formatSize(storage.usedBytes))
        tvTotal.text = context.getString(R.string.dashboard_total_format, formatSize(storage.totalBytes))
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            progressStorage.progress = 0
            tvPercent.text = "--"
            tvUsed.text = ""
            tvTotal.text = ""
        }
    }

    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) {
            String.format("%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }
}
