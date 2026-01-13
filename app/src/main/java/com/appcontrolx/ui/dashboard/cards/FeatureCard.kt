package com.appcontrolx.ui.dashboard.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.appcontrolx.R
import com.google.android.material.card.MaterialCardView

/**
 * Card component for quick access to app features.
 * 
 * Features:
 * - Icon display
 * - Feature label
 * - Click handling for navigation
 * - Consistent Material 3 styling
 * 
 * Requirements: 0.3.1-0.3.5 - Feature cards for quick access navigation
 */
class FeatureCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val ivIcon: ImageView
    private val tvLabel: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_feature, this, true)
        
        // Setup card appearance
        cardElevation = resources.getDimension(R.dimen.card_elevation_small)
        radius = resources.getDimension(R.dimen.card_corner_radius)
        setCardBackgroundColor(context.getColor(R.color.surface))
        strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
        strokeColor = context.getColor(R.color.outline)
        
        ivIcon = findViewById(R.id.ivFeatureIcon)
        tvLabel = findViewById(R.id.tvFeatureLabel)
        
        // Handle custom attributes if provided
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.FeatureCard)
            try {
                val iconRes = typedArray.getResourceId(R.styleable.FeatureCard_featureIcon, 0)
                val labelRes = typedArray.getResourceId(R.styleable.FeatureCard_featureLabel, 0)
                val iconTint = typedArray.getResourceId(R.styleable.FeatureCard_featureIconTint, R.color.primary)
                
                if (iconRes != 0) {
                    ivIcon.setImageResource(iconRes)
                }
                if (labelRes != 0) {
                    tvLabel.setText(labelRes)
                }
                ivIcon.setColorFilter(context.getColor(iconTint))
            } finally {
                typedArray.recycle()
            }
        }
    }

    /**
     * Set the feature icon.
     */
    fun setIcon(@DrawableRes iconRes: Int) {
        ivIcon.setImageResource(iconRes)
    }

    /**
     * Set the feature icon tint color.
     */
    fun setIconTint(@ColorRes colorRes: Int) {
        ivIcon.setColorFilter(context.getColor(colorRes))
    }

    /**
     * Set the feature label.
     */
    fun setLabel(@StringRes labelRes: Int) {
        tvLabel.setText(labelRes)
    }

    /**
     * Set the feature label text directly.
     */
    fun setLabel(label: String) {
        tvLabel.text = label
    }

    /**
     * Configure the feature card with icon, label, and tint.
     */
    fun configure(
        @DrawableRes iconRes: Int,
        @StringRes labelRes: Int,
        @ColorRes tintRes: Int = R.color.primary
    ) {
        setIcon(iconRes)
        setLabel(labelRes)
        setIconTint(tintRes)
    }
}
