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
 * - Total apps count (prominent)
 * - User apps count
 * - System apps count
 * - DevCheck-style design
 * 
 * Requirements: 0.1.8 - Apps card showing total user apps and system apps count
 */
class AppsCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val tvTotal: TextView
    private val tvUser: TextView
    private val tvSystem: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_apps, this, true)
        
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        
        tvTotal = findViewById(R.id.tvAppsTotal)
        tvUser = findViewById(R.id.tvAppsUser)
        tvSystem = findViewById(R.id.tvAppsSystem)
    }

    /**
     * Update the card with app counts.
     */
    fun update(counts: AppCounts) {
        tvTotal.text = context.getString(R.string.dashboard_apps_total, counts.total)
        tvUser.text = context.getString(R.string.dashboard_apps_user, counts.userApps)
        tvSystem.text = context.getString(R.string.dashboard_apps_system, counts.systemApps)
    }

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean) {
        if (loading) {
            tvTotal.text = "-- Total"
            tvUser.text = "-- User"
            tvSystem.text = "-- System"
        }
    }
}
