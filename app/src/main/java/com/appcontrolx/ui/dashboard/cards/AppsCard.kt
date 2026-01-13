package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.appcontrolx.R
import com.appcontrolx.data.model.AppCounts
import com.google.android.material.card.MaterialCardView

/**
 * Wide card component displaying installed app counts.
 * 
 * Features:
 * - User apps count
 * - System apps count
 * - Horizontal layout
 * 
 * Requirements: 0.1.8 - Apps card showing total user apps and system apps count
 */
class AppsCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvCount: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_apps, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvCount = findViewById(R.id.tvAppsCount)
    }

    /**
     * Update the card with app counts.
     */
    fun update(counts: AppCounts) {
        tvCount.text = context.getString(
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
            tvCount.text = "-- user • -- system"
        }
    }
}
