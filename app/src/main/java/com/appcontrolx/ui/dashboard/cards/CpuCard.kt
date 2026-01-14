package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appcontrolx.R
import com.appcontrolx.data.model.CpuInfo
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Enhanced CPU card component with real-time graph and per-core frequencies.
 * 
 * Features:
 * - Real-time line graph showing CPU usage over time
 * - Large percentage display with progress bar
 * - Temperature display (if available)
 * - Core count display
 * - Per-core frequency grid (2 columns)
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
 */
class CpuCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val cpuGraph: CpuGraphView
    private val tvUsage: TextView
    private val progressBar: LinearProgressIndicator
    private val tvTemp: TextView
    private val tvCores: TextView
    private val rvCoreFrequencies: RecyclerView
    
    private val coreFrequencyAdapter = CoreFrequencyAdapter()
    
    // Graph data points (last 60 seconds)
    private val graphData = mutableListOf<Float>()

    init {
        // Inflate the card layout
        LayoutInflater.from(context).inflate(R.layout.card_cpu, this, true)
        
        // Setup card appearance
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius_large)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        // Find views
        cpuGraph = findViewById(R.id.cpuGraph)
        tvUsage = findViewById(R.id.tvCpuUsage)
        progressBar = findViewById(R.id.progressCpu)
        tvTemp = findViewById(R.id.tvCpuTemp)
        tvCores = findViewById(R.id.tvCpuCores)
        rvCoreFrequencies = findViewById(R.id.rvCoreFrequencies)
        
        // Setup core frequency RecyclerView with 2 columns
        rvCoreFrequencies.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = coreFrequencyAdapter
            isNestedScrollingEnabled = false
        }
    }

    /**
     * Update the card with CPU information.
     * This updates the graph, usage display, temperature, core count, and frequencies.
     */
    fun update(cpu: CpuInfo) {
        val usagePercent = cpu.usagePercent.toInt().coerceIn(0, 100)
        
        // Update usage display
        tvUsage.text = context.getString(R.string.dashboard_percent_format, usagePercent)
        progressBar.progress = usagePercent
        
        // Add data point to graph
        addGraphDataPoint(cpu.usagePercent)
        
        // Update temperature
        cpu.temperature?.let { temp ->
            tvTemp.text = context.getString(R.string.dashboard_temp_format, temp)
            tvTemp.visibility = VISIBLE
        } ?: run {
            tvTemp.text = context.getString(R.string.status_not_available)
            tvTemp.visibility = VISIBLE
        }
        
        // Update core count
        tvCores.text = context.getString(R.string.dashboard_cpu_cores, cpu.cores)
        
        // Update core frequencies
        updateCoreFrequencies(cpu.coreFrequencies)
    }
    
    /**
     * Update the core frequency grid with new values.
     */
    fun updateCoreFrequencies(frequencies: List<Long>) {
        coreFrequencyAdapter.updateFrequencies(frequencies)
    }
    
    /**
     * Add a data point to the graph with animation.
     */
    private fun addGraphDataPoint(value: Float) {
        graphData.add(value.coerceIn(0f, 100f))
        
        // Trim to max data points (60)
        while (graphData.size > CpuGraphView.MAX_DATA_POINTS) {
            graphData.removeAt(0)
        }
        
        // Update graph view
        cpuGraph.setData(graphData)
    }
    
    /**
     * Clear all graph data.
     */
    fun clearGraphData() {
        graphData.clear()
        cpuGraph.clearData()
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
            coreFrequencyAdapter.updateFrequencies(emptyList())
            cpuGraph.clearData()
        }
    }
    
    /**
     * Adapter for displaying core frequencies in a grid.
     */
    private inner class CoreFrequencyAdapter : RecyclerView.Adapter<CoreFrequencyAdapter.ViewHolder>() {
        
        private var frequencies: List<Long> = emptyList()
        
        fun updateFrequencies(newFrequencies: List<Long>) {
            frequencies = newFrequencies
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_core_frequency, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val frequency = frequencies.getOrNull(position) ?: 0L
            holder.bind(position, frequency)
        }
        
        override fun getItemCount(): Int = frequencies.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvCoreLabel: TextView = itemView.findViewById(R.id.tvCoreLabel)
            private val tvCoreFrequency: TextView = itemView.findViewById(R.id.tvCoreFrequency)
            
            fun bind(coreIndex: Int, frequencyMhz: Long) {
                tvCoreLabel.text = context.getString(R.string.dashboard_core_label, coreIndex)
                tvCoreFrequency.text = context.getString(R.string.dashboard_core_frequency_mhz, frequencyMhz)
            }
        }
    }
}
