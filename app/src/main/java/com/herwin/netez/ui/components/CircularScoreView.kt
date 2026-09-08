package com.herwin.netez.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.herwin.netez.R
import kotlin.math.min

class CircularScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var score: Int = 0
    private var status: String = "Good"
    
    private val strokeWidth = 8f * resources.displayMetrics.density
    
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.2).toInt()
        style = Paint.Style.STROKE
        this.strokeWidth = this@CircularScoreView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        this.strokeWidth = this@CircularScoreView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f * resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    
    private val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (255 * 0.7).toInt()
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    
    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    
    private val rect = RectF()

    fun setScore(value: Int, statusText: String) {
        score = value.coerceIn(0, 100)
        status = statusText
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (size - strokeWidth) / 2f
        
        rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        
        // Draw track
        canvas.drawCircle(centerX, centerY, radius, trackPaint)
        
        // Draw indicator
        val sweepAngle = (score / 100f) * 360f
        canvas.drawArc(rect, -90f, sweepAngle, false, indicatorPaint)
        
        // Draw text
        canvas.drawText(score.toString(), centerX, centerY - 2f * resources.displayMetrics.density, scorePaint)
        canvas.drawText("/100", centerX, centerY + 10f * resources.displayMetrics.density, totalPaint)
        canvas.drawText(status, centerX, centerY + 25f * resources.displayMetrics.density, statusPaint)
    }
}
