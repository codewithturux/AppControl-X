package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.AppCounts
import com.google.android.material.card.MaterialCardView

/**
 * Card component displaying installed app counts with user/system breakdown.
 * 
 * Features:
 * - Total apps count (prominent, large text)
 * - User | System breakdown in single line
 * - Click ripple effect
 * - Click listener to navigate to Apps tab
 * 
 * Requirements: 4.1, 4.2, 4.4 - Apps card showing total count prominently,
 * breakdown display, and navigation to Apps tab on click
 */
class AppsCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvTotal: TextView
    private val tvBreakdown: TextView
    
    /**
     * Callback for when the card is clicked.
     * Used to navigate to the Apps tab.
     */
    var onCardClickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.card_apps, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvTotal = findViewById(R.id.tvAppsTotal)
        tvBreakdown = findViewById(R.id.tvAppsBreakdown)
        
        // Setup click listener for navigation to Apps tab
        setOnClickListener {
            onCardClickListener?.invoke()
        }
    }

    /**
     * Update the card with app counts.
     * Displays total count prominently and breakdown in "X User | Y System" format.
     */
    fun update(counts: AppCounts) {
        // Display total count prominently (just the number)
        tvTotal.text = counts.total.toString()
        
        // Display breakdown in "X User | Y System" format
        tvBreakdown.text = context.getString(
            R.string.dashboard_apps_count,
            counts.userApps,
            counts.systemApps
        )
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvTotal.text = "--"
            tvBreakdown.text = "-- User | -- System"
        }
    }
}
