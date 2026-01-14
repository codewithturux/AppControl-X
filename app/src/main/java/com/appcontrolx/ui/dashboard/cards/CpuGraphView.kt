package com.appcontrolx.ui.dashboard.cards

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.appcontrolx.R

/**
 * Custom view that displays a real-time line graph for CPU usage.
 * 
 * Features:
 * - Line graph drawing with smooth Path
 * - Fill gradient below the line
 * - Support for dynamic data points (max 60)
 * - Smooth animation on data update
 * 
 * Requirements: 1.1, 1.4 - Real-time CPU usage graph with smooth animations
 */
class CpuGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val MAX_DATA_POINTS = 60
        private const val ANIMATION_DURATION = 300L
    }

    // Data points storage (0-100 range)
    private val dataPoints = mutableListOf<Float>()
    
    // Animation support
    private var animatedValue: Float = 0f
    private var animator: ValueAnimator? = null

    // Paint objects
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Paths for drawing
    private val linePath = Path()
    private val fillPath = Path()

    // Customizable attributes
    private var lineColor: Int = ContextCompat.getColor(context, R.color.md_theme_primary)
    private var fillColor: Int = ContextCompat.getColor(context, R.color.md_theme_primary)
    private var strokeWidth: Float = 4f * resources.displayMetrics.density

    init {
        // Parse custom attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CpuGraphView)
            try {
                lineColor = typedArray.getColor(
                    R.styleable.CpuGraphView_graphLineColor,
                    ContextCompat.getColor(context, R.color.md_theme_primary)
                )
                fillColor = typedArray.getColor(
                    R.styleable.CpuGraphView_graphFillColor,
                    ContextCompat.getColor(context, R.color.md_theme_primary)
                )
                strokeWidth = typedArray.getDimension(
                    R.styleable.CpuGraphView_graphStrokeWidth,
                    4f * resources.displayMetrics.density
                )
            } finally {
                typedArray.recycle()
            }
        }

        // Apply paint settings
        linePaint.color = lineColor
        linePaint.strokeWidth = strokeWidth
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateFillGradient()
    }

    /**
     * Updates the fill gradient based on current view dimensions.
     */
    private fun updateFillGradient() {
        if (height > 0) {
            fillPaint.shader = LinearGradient(
                0f, 0f,
                0f, height.toFloat(),
                intArrayOf(
                    adjustAlpha(fillColor, 0.4f),
                    adjustAlpha(fillColor, 0.1f),
                    adjustAlpha(fillColor, 0.0f)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    /**
     * Adjusts the alpha of a color.
     */
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (255 * factor).toInt()
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(alpha, red, green, blue)
    }

    /**
     * Sets the data points for the graph.
     * Values should be in the range 0-100.
     * 
     * @param points List of CPU usage values (0-100)
     */
    fun setData(points: List<Float>) {
        dataPoints.clear()
        dataPoints.addAll(points.map { it.coerceIn(0f, 100f) })
        
        // Trim to max data points
        while (dataPoints.size > MAX_DATA_POINTS) {
            dataPoints.removeAt(0)
        }
        
        invalidate()
    }

    /**
     * Adds a single data point to the graph with animation.
     * Value should be in the range 0-100.
     * 
     * @param value CPU usage value (0-100)
     */
    fun addDataPoint(value: Float) {
        val clampedValue = value.coerceIn(0f, 100f)
        
        // Cancel any running animation
        animator?.cancel()
        
        // Animate the new value
        val startValue = if (dataPoints.isNotEmpty()) dataPoints.last() else 0f
        animator = ValueAnimator.ofFloat(startValue, clampedValue).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedValue = animation.animatedValue as Float
                
                // Update the last data point during animation
                if (dataPoints.isNotEmpty()) {
                    dataPoints[dataPoints.lastIndex] = animatedValue
                }
                invalidate()
            }
        }
        
        // Add the new data point
        dataPoints.add(clampedValue)
        
        // Trim to max data points
        while (dataPoints.size > MAX_DATA_POINTS) {
            dataPoints.removeAt(0)
        }
        
        animator?.start()
    }

    /**
     * Clears all data points from the graph.
     */
    fun clearData() {
        animator?.cancel()
        dataPoints.clear()
        invalidate()
    }

    /**
     * Gets the current number of data points.
     */
    fun getDataPointCount(): Int = dataPoints.size

    /**
     * Gets a copy of the current data points.
     */
    fun getDataPoints(): List<Float> = dataPoints.toList()


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (dataPoints.isEmpty() || width == 0 || height == 0) {
            // Draw a flat line at the bottom when no data
            drawEmptyState(canvas)
            return
        }

        // Calculate drawing area with padding
        val padding = strokeWidth / 2
        val drawWidth = width - padding * 2
        val drawHeight = height - padding * 2

        // Build paths
        buildPaths(padding, drawWidth, drawHeight)

        // Draw fill gradient first (below the line)
        canvas.drawPath(fillPath, fillPaint)

        // Draw the line on top
        canvas.drawPath(linePath, linePaint)
    }

    /**
     * Draws an empty state when there's no data.
     */
    private fun drawEmptyState(canvas: Canvas) {
        val y = height - strokeWidth / 2
        linePath.reset()
        linePath.moveTo(0f, y)
        linePath.lineTo(width.toFloat(), y)
        canvas.drawPath(linePath, linePaint.apply { alpha = 50 })
        linePaint.alpha = 255
    }

    /**
     * Builds the line and fill paths from data points.
     */
    private fun buildPaths(padding: Float, drawWidth: Float, drawHeight: Float) {
        linePath.reset()
        fillPath.reset()

        val pointCount = dataPoints.size
        if (pointCount == 0) return

        // Calculate x spacing between points
        val xStep = if (pointCount > 1) drawWidth / (pointCount - 1) else drawWidth

        // Start fill path at bottom-left
        fillPath.moveTo(padding, height - padding)

        dataPoints.forEachIndexed { index, value ->
            val x = padding + (index * xStep)
            // Invert Y because canvas Y increases downward
            val y = padding + drawHeight * (1 - value / 100f)

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                // Use smooth curves for better visual
                val prevX = padding + ((index - 1) * xStep)
                val prevValue = dataPoints[index - 1]
                val prevY = padding + drawHeight * (1 - prevValue / 100f)
                
                // Control points for smooth curve
                val controlX1 = prevX + xStep / 2
                val controlX2 = x - xStep / 2
                
                linePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        // Close fill path
        val lastX = padding + ((pointCount - 1) * xStep)
        fillPath.lineTo(lastX, height - padding)
        fillPath.close()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    /**
     * Sets the line color programmatically.
     */
    fun setLineColor(color: Int) {
        lineColor = color
        linePaint.color = color
        invalidate()
    }

    /**
     * Sets the fill color programmatically.
     */
    fun setFillColor(color: Int) {
        fillColor = color
        updateFillGradient()
        invalidate()
    }

    /**
     * Sets the stroke width programmatically.
     */
    fun setStrokeWidth(width: Float) {
        strokeWidth = width
        linePaint.strokeWidth = width
        invalidate()
    }
}
