package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.NetworkInfo
import com.appcontrolx.data.model.NetworkType
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying network connection status.
 * 
 * Features:
 * - Network type (WiFi/Mobile/Ethernet/None)
 * - Connection status indicator
 * - WiFi SSID or cellular info
 * - Color-coded icon based on connection status
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

    init {
        LayoutInflater.from(context).inflate(R.layout.card_network, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        ivIcon = findViewById(R.id.ivNetworkIcon)
        tvType = findViewById(R.id.tvNetworkType)
        tvDetail = findViewById(R.id.tvNetworkDetail)
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
        }
    }
}
