package com.ghost.dothabit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Calendar

/**
 * Draws one dot per day of the current month, 7 columns per row.
 * Day 1 = first dot, day 2 = next dot, etc. -- exactly like a
 * GitHub-style contribution graph but for a single habit.
 *
 * RED  = not marked done yet (the automatic default)
 * GREEN = manually marked done
 * Grey  = future day (hasn't happened yet, nothing to mark)
 */
class DotGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cols = 7
    private var dotRadius = 0f
    private var spacing = 0f

    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }
    private val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43A047") }
    private val futurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3A3A3A") }
    private val todayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    var habitId: String = PrefsHelper.DEFAULT_HABIT_ID
        set(value) {
            field = value
            invalidate()
        }

    var onDayTapped: ((dayOfMonth: Int) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        spacing = width / (cols * 2.2f)
        dotRadius = spacing * 0.38f
        val rows = 6
        val height = (rows * spacing * 1.9f).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val totalDays = PrefsHelper.daysInMonth(cal)

        val cellW = width / cols.toFloat()
        val cellH = cellW

        for (day in 1..totalDays) {
            val index = day - 1
            val row = index / cols
            val col = index % cols
            val cx = cellW * col + cellW / 2f
            val cy = cellH * row + cellH / 2f

            val paint = when {
                day > today -> futurePaint
                else -> {
                    val dayCal = Calendar.getInstance()
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                        if (PrefsHelper.isDone(context, dayCal, habitId)) greenPaint else redPaint
                }
            }
            canvas.drawCircle(cx, cy, dotRadius, paint)
            if (day == today) {
                canvas.drawCircle(cx, cy, dotRadius + 5f, todayRingPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cellW = width / cols.toFloat()
            val cellH = cellW
            val col = (event.x / cellW).toInt().coerceIn(0, cols - 1)
            val row = (event.y / cellH).toInt().coerceAtLeast(0)
            val day = row * cols + col + 1
            val cal = Calendar.getInstance()
            val totalDays = PrefsHelper.daysInMonth(cal)
            val today = cal.get(Calendar.DAY_OF_MONTH)
            // Only allow marking today or past days in this month -- not the future.
            if (day in 1..totalDays && day <= today) {
                onDayTapped?.invoke(day)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
