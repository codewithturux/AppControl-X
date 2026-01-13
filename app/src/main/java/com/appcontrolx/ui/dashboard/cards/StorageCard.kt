package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.StorageInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying internal storage usage.
 * 
 * Features:
 * - Used storage display
 * - Total storage display
 * - Usage percentage
 * 
 * Requirements: 0.1.5 - Storage card showing used, total, and percentage for internal storage
 */
class StorageCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvUsed: TextView
    private val tvDetail: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_storage, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvUsed = findViewById(R.id.tvStorageUsed)
        tvDetail = findViewById(R.id.tvStorageDetail)
    }

    /**
     * Update the card with storage information.
     */
    fun update(storage: StorageInfo) {
        val percent = storage.usedPercent.toInt()
        
        tvUsed.text = formatSize(storage.usedBytes)
        tvDetail.text = "of ${formatSize(storage.totalBytes)} • $percent%"
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
            String.format("%.0f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }
}
