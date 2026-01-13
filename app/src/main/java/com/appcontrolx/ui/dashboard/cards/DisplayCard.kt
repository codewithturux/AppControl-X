package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.DisplayInfo
import com.appcontrolx.data.model.GpuInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying screen resolution, refresh rate, and GPU model.
 * 
 * Features:
 * - Screen resolution (width x height)
 * - Refresh rate in Hz
 * - GPU model name
 * - DevCheck-style design
 * 
 * Requirements: 0.1.6 - Display card showing resolution and refresh rate (Hz)
 */
class DisplayCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvResolution: TextView
    private val tvRefresh: TextView
    private val tvGpuModel: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_display, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvResolution = findViewById(R.id.tvDisplayResolution)
        tvRefresh = findViewById(R.id.tvDisplayRefresh)
        tvGpuModel = findViewById(R.id.tvGpuModel)
    }

    /**
     * Update the card with display information.
     */
    fun update(display: DisplayInfo) {
        tvResolution.text = display.resolution
        tvRefresh.text = context.getString(R.string.dashboard_hz_format, display.refreshRate)
    }

    /**
     * Update the card with GPU information.
     */
    fun updateGpu(gpu: GpuInfo?) {
        if (gpu != null) {
            tvGpuModel.text = gpu.model
            tvGpuModel.visibility = View.VISIBLE
        } else {
            tvGpuModel.visibility = View.GONE
        }
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvResolution.text = "--"
            tvRefresh.text = ""
            tvGpuModel.visibility = View.GONE
        }
    }
}
