package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.DeviceInfo
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.TimeUnit

/**
 * Card component displaying device hardware and software information.
 * 
 * Features:
 * - Device brand and model
 * - SoC/processor name
 * - Android version with codename
 * - System uptime
 * - Deep sleep time with percentage (Root mode only)
 * - Kernel version
 * - Build number
 * - DevCheck-style design
 * 
 * Requirements: 0.2.1-0.2.7 - Device Info card with all device details
 */
class DeviceInfoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvDeviceName: TextView
    private val layoutSoc: LinearLayout
    private val tvSoc: TextView
    private val tvAndroidVersion: TextView
    private val tvUptime: TextView
    private val layoutDeepSleep: LinearLayout
    private val tvDeepSleep: TextView
    private val tvKernel: TextView
    private val tvBuild: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_device_info, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius_large)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvDeviceName = findViewById(R.id.tvDeviceName)
        layoutSoc = findViewById(R.id.layoutSoc)
        tvSoc = findViewById(R.id.tvSoc)
        tvAndroidVersion = findViewById(R.id.tvAndroidVersion)
        tvUptime = findViewById(R.id.tvUptime)
        layoutDeepSleep = findViewById(R.id.layoutDeepSleep)
        tvDeepSleep = findViewById(R.id.tvDeepSleep)
        tvKernel = findViewById(R.id.tvKernel)
        tvBuild = findViewById(R.id.tvBuild)
    }

    /**
     * Update the card with device information.
     */
    fun update(device: DeviceInfo) {
        // Device name
        tvDeviceName.text = "${device.brand} ${device.model}"
        
        // SoC/Processor name
        device.socName?.let { soc ->
            tvSoc.text = soc
            layoutSoc.visibility = View.VISIBLE
        } ?: run {
            layoutSoc.visibility = View.GONE
        }
        
        // Android version with codename
        val androidText = if (device.androidCodename != null) {
            "Android ${device.androidVersion} (${device.androidCodename})"
        } else {
            "Android ${device.androidVersion} (API ${device.apiLevel})"
        }
        tvAndroidVersion.text = androidText
        
        // Uptime
        tvUptime.text = formatDuration(device.uptimeMs)
        
        // Deep sleep with percentage (only shown in Root mode)
        device.deepSleepMs?.let { deepSleep ->
            layoutDeepSleep.visibility = View.VISIBLE
            val deepSleepPercent = if (device.uptimeMs > 0) {
                ((deepSleep.toFloat() / device.uptimeMs) * 100).toInt()
            } else {
                0
            }
            tvDeepSleep.text = "${formatDurationDetailed(deepSleep)} ($deepSleepPercent%)"
        } ?: run {
            layoutDeepSleep.visibility = View.GONE
        }
        
        // Kernel version
        tvKernel.text = device.kernelVersion
        
        // Build number
        tvBuild.text = device.buildNumber
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvDeviceName.text = "--"
            layoutSoc.visibility = View.GONE
            tvAndroidVersion.text = "--"
            tvUptime.text = "--"
            layoutDeepSleep.visibility = View.GONE
            tvKernel.text = "--"
            tvBuild.text = "--"
        }
    }

    private fun formatDuration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m")
        }.trim()
    }
    
    private fun formatDurationDetailed(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
}
