package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Custom view that renders a scrolling ECG waveform with glow effect.
 */
class EcgView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>()
    private val maxPoints = 200

    // Main ECG line
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF99")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Glow effect behind the line
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D00FF99") // 30% opacity green
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Grid lines
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A00FF99") // very faint green
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    // Filled area under the curve
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun addDataPoints(points: List<Float>) {
        dataPoints.addAll(points)
        // Keep only the latest points
        while (dataPoints.size > maxPoints) {
            dataPoints.removeAt(0)
        }
        invalidate()
    }

    fun clearData() {
        dataPoints.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2f

        // Draw background grid
        drawGrid(canvas, w, h)

        if (dataPoints.size < 2) return

        val stepX = w / maxPoints
        val amplitude = h * 0.4f // Use 40% of height for amplitude

        // Build the line path
        linePath.reset()
        fillPath.reset()

        val startX = w - (dataPoints.size * stepX)

        for (i in dataPoints.indices) {
            val x = startX + (i * stepX)
            val y = midY - (dataPoints[i] * amplitude)

            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path
        val lastX = startX + ((dataPoints.size - 1) * stepX)
        fillPath.lineTo(lastX, h)
        fillPath.lineTo(startX, h)
        fillPath.close()

        // Gradient fill under the curve
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.parseColor("#1A00FF99"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // Draw glow
        canvas.drawPath(linePath, glowPaint)

        // Draw main line
        canvas.drawPath(linePath, linePaint)
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float) {
        // Horizontal grid lines
        val hLines = 5
        for (i in 0..hLines) {
            val y = (h / hLines) * i
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Vertical grid lines
        val vLines = 10
        for (i in 0..vLines) {
            val x = (w / vLines) * i
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }
    }

    /**
     * Set the line color based on status (for DANGER mode, turn red)
     */
    fun setStatusColor(color: Int) {
        linePaint.color = color
        glowPaint.color = Color.argb(77, Color.red(color), Color.green(color), Color.blue(color))
        gridPaint.color = Color.argb(26, Color.red(color), Color.green(color), Color.blue(color))
        invalidate()
    }
}
