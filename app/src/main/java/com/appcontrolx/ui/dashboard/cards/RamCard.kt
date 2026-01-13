package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.RamInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying RAM usage information with circular progress.
 * 
 * Features:
 * - Circular progress indicator showing percentage
 * - Used memory display
 * - Total memory display
 * - DevCheck-style design
 * 
 * Requirements: 0.1.4 - RAM card showing used, free, and total memory with percentage
 */
class RamCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val progressRam: ProgressBar
    private val tvPercent: TextView
    private val tvUsed: TextView
    private val tvTotal: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_ram, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        progressRam = findViewById(R.id.progressRam)
        tvPercent = findViewById(R.id.tvRamPercent)
        tvUsed = findViewById(R.id.tvRamUsed)
        tvTotal = findViewById(R.id.tvRamTotal)
    }

    /**
     * Update the card with RAM information.
     */
    fun update(ram: RamInfo) {
        val percent = ram.usedPercent.toInt()
        
        // Update circular progress
        progressRam.progress = percent
        
        // Update percentage text in center
        tvPercent.text = context.getString(R.string.dashboard_percent_format, percent)
        
        // Update used and total text
        tvUsed.text = context.getString(R.string.dashboard_used_format, formatSize(ram.usedBytes))
        tvTotal.text = context.getString(R.string.dashboard_total_format, formatSize(ram.totalBytes))
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            progressRam.progress = 0
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
