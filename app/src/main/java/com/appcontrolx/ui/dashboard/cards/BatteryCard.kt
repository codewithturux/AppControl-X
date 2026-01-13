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
 * - Temperature display
 * - Charging/Discharging status text
 * - Color-coded icon based on battery level
 * - DevCheck-style design
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
    private val tvTemp: TextView
    private val tvStatus: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_battery, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        ivIcon = findViewById(R.id.ivBatteryIcon)
        tvPercent = findViewById(R.id.tvBatteryPercent)
        tvTemp = findViewById(R.id.tvBatteryTemp)
        tvStatus = findViewById(R.id.tvBatteryStatus)
    }

    /**
     * Update the card with battery information.
     */
    fun update(battery: BatteryInfo) {
        // Update percentage
        tvPercent.text = context.getString(R.string.dashboard_percent_format, battery.percent)
        
        // Update temperature
        tvTemp.text = context.getString(R.string.dashboard_temp_format, battery.temperature)
        
        // Update charging/discharging status
        if (battery.isCharging) {
            tvStatus.text = context.getString(R.string.dashboard_charging)
            tvStatus.setTextColor(context.getColor(R.color.status_positive))
        } else {
            tvStatus.text = context.getString(R.string.dashboard_discharging)
            tvStatus.setTextColor(context.getColor(R.color.on_surface_secondary))
        }
        
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
            tvTemp.text = ""
            tvStatus.text = ""
        }
    }
}
