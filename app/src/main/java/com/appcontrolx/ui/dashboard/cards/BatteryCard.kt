package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.BatteryInfo
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying battery percentage, charging status, and temperature.
 * 
 * Features:
 * - Battery percentage display
 * - Charging indicator
 * - Temperature display
 * - Color-coded icon based on battery level
 * 
 * Requirements: 0.1.2 - Battery card showing percentage, charging status, and temperature
 */
class BatteryCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val ivIcon: ImageView
    private val tvPercent: TextView
    private val tvDetail: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_battery, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        ivIcon = findViewById(R.id.ivBatteryIcon)
        tvPercent = findViewById(R.id.tvBatteryPercent)
        tvDetail = findViewById(R.id.tvBatteryDetail)
    }

    /**
     * Update the card with battery information.
     */
    fun update(battery: BatteryInfo) {
        tvPercent.text = context.getString(R.string.dashboard_percent_format, battery.percent)
        
        val statusText = buildString {
            append(context.getString(R.string.dashboard_temp_format, battery.temperature))
            if (battery.isCharging) {
                append(" • ")
                append(context.getString(R.string.dashboard_charging))
            }
        }
        tvDetail.text = statusText
        
        // Update icon color based on battery level
        val iconTint = when {
            battery.percent <= 20 -> R.color.status_negative
            battery.isCharging -> R.color.status_positive
            else -> R.color.status_positive
        }
        ivIcon.setColorFilter(context.getColor(iconTint))
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvPercent.text = "--"
            tvDetail.text = ""
        }
    }
}
