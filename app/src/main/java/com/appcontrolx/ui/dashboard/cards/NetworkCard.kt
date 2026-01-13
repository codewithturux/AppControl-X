package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.NetworkInfo
import com.appcontrolx.data.model.NetworkType
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying network connection status with signal details.
 * 
 * Features:
 * - Network type (WiFi/Mobile/Ethernet/None)
 * - Connection status indicator
 * - WiFi SSID or cellular info
 * - Signal strength percentage
 * - Signal dBm value
 * - Color-coded icon based on connection status
 * - DevCheck-style design
 * 
 * Requirements: 0.1.3 - Network card showing WiFi/Mobile data status and signal strength
 */
class NetworkCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val ivIcon: ImageView
    private val tvType: TextView
    private val tvDetail: TextView
    private val tvSignalStrength: TextView
    private val tvSignalDbm: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_network, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        ivIcon = findViewById(R.id.ivNetworkIcon)
        tvType = findViewById(R.id.tvNetworkType)
        tvDetail = findViewById(R.id.tvNetworkDetail)
        tvSignalStrength = findViewById(R.id.tvSignalStrength)
        tvSignalDbm = findViewById(R.id.tvSignalDbm)
    }

    /**
     * Update the card with network information.
     */
    fun update(network: NetworkInfo) {
        val typeText = when (network.type) {
            NetworkType.WIFI -> "WiFi"
            NetworkType.MOBILE -> "Mobile"
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.NONE -> context.getString(R.string.dashboard_network_disconnected)
        }
        tvType.text = typeText
        
        val detailText = when {
            network.type == NetworkType.WIFI && network.ssid != null -> network.ssid
            network.type == NetworkType.MOBILE -> "Cellular"
            network.isConnected -> context.getString(R.string.status_available)
            else -> context.getString(R.string.status_not_available)
        }
        tvDetail.text = detailText
        
        // Show signal strength percentage if available
        network.signalPercent?.let { percent ->
            tvSignalStrength.text = context.getString(R.string.dashboard_signal_strength, percent)
            tvSignalStrength.visibility = View.VISIBLE
        } ?: run {
            tvSignalStrength.visibility = View.GONE
        }
        
        // Show signal dBm if available
        network.signalDbm?.let { dbm ->
            tvSignalDbm.text = context.getString(R.string.dashboard_signal_dbm, dbm)
            tvSignalDbm.visibility = View.VISIBLE
        } ?: run {
            tvSignalDbm.visibility = View.GONE
        }
        
        // Update icon color based on connection status
        val iconTint = if (network.isConnected) R.color.status_positive else R.color.status_neutral
        ivIcon.setColorFilter(context.getColor(iconTint))
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvType.text = "--"
            tvDetail.text = ""
            tvSignalStrength.visibility = View.GONE
            tvSignalDbm.visibility = View.GONE
        }
    }
}
