package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.GpuInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying GPU model information.
 * 
 * Features:
 * - GPU model name
 * - Vendor name (if available)
 * - Requires root to retrieve GPU info
 * 
 * Requirements: 0.1.7 - GPU card showing GPU model name (if available via root)
 */
class GpuCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvModel: TextView
    private val tvVendor: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_gpu, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvModel = findViewById(R.id.tvGpuModel)
        tvVendor = findViewById(R.id.tvGpuVendor)
    }

    /**
     * Update the card with GPU information.
     * @param gpu GPU info, or null if not available
     */
    fun update(gpu: GpuInfo?) {
        if (gpu != null) {
            tvModel.text = gpu.model
            gpu.vendor?.let {
                tvVendor.text = it
                tvVendor.visibility = View.VISIBLE
            } ?: run {
                tvVendor.visibility = View.GONE
            }
        } else {
            tvModel.text = context.getString(R.string.dashboard_gpu_unavailable)
            tvVendor.visibility = View.GONE
        }
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvModel.text = "--"
            tvVendor.visibility = View.GONE
        }
    }
}
