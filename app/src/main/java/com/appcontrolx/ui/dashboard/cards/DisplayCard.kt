package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.DisplayInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying screen resolution and refresh rate.
 * 
 * Features:
 * - Screen resolution (width x height)
 * - Refresh rate in Hz
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

    init {
        LayoutInflater.from(context).inflate(R.layout.card_display, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvResolution = findViewById(R.id.tvDisplayResolution)
        tvRefresh = findViewById(R.id.tvDisplayRefresh)
    }

    /**
     * Update the card with display information.
     */
    fun update(display: DisplayInfo) {
        tvResolution.text = display.resolution
        tvRefresh.text = context.getString(R.string.dashboard_hz_format, display.refreshRate)
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvResolution.text = "--"
            tvRefresh.text = ""
        }
    }
}
