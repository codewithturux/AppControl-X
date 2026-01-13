package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.RamInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying RAM usage information.
 * 
 * Features:
 * - Used memory display
 * - Total memory display
 * - Usage percentage
 * 
 * Requirements: 0.1.4 - RAM card showing used, free, and total memory with percentage
 */
class RamCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvUsed: TextView
    private val tvDetail: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_ram, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvUsed = findViewById(R.id.tvRamUsed)
        tvDetail = findViewById(R.id.tvRamDetail)
    }

    /**
     * Update the card with RAM information.
     */
    fun update(ram: RamInfo) {
        val percent = ram.usedPercent.toInt()
        
        tvUsed.text = formatSize(ram.usedBytes)
        tvDetail.text = "of ${formatSize(ram.totalBytes)} • $percent%"
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvUsed.text = "--"
            tvDetail.text = ""
        }
    }

    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1) {
            String.format("%.1f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }
}
