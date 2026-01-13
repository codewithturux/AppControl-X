package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.CpuInfo
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Wide card component displaying CPU usage, temperature, and core count.
 * 
 * Features:
 * - Large percentage display
 * - Progress bar showing usage
 * - Temperature display (if available)
 * - Core count
 * 
 * Requirements: 0.1.1 - CPU card showing current load percentage and temperature
 */
class CpuCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvUsage: TextView
    private val progressBar: LinearProgressIndicator
    private val tvTemp: TextView
    private val tvCores: TextView

    init {
        // Inflate the card layout
        LayoutInflater.from(context).inflate(R.layout.card_cpu, this, true)
        
        // Setup card appearance
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius_large)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        // Find views
        tvUsage = findViewById(R.id.tvCpuUsage)
        progressBar = findViewById(R.id.progressCpu)
        tvTemp = findViewById(R.id.tvCpuTemp)
        tvCores = findViewById(R.id.tvCpuCores)
    }

    /**
     * Update the card with CPU information.
     */
    fun update(cpu: CpuInfo) {
        val usagePercent = cpu.usagePercent.toInt().coerceIn(0, 100)
        
        tvUsage.text = context.getString(R.string.dashboard_percent_format, usagePercent)
        progressBar.progress = usagePercent
        
        cpu.temperature?.let { temp ->
            tvTemp.text = context.getString(R.string.dashboard_temp_format, temp)
            tvTemp.visibility = VISIBLE
        } ?: run {
            tvTemp.visibility = GONE
        }
        
        tvCores.text = context.getString(R.string.dashboard_cpu_cores, cpu.cores)
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvUsage.text = "--"
            progressBar.progress = 0
            tvTemp.visibility = GONE
            tvCores.text = ""
        }
    }
}
