package com.ghost.dothabit

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import java.util.Calendar

/**
 * Home-screen widget: shows the same red/green dot grid as the app,
 * for the current month, and lets you tap the widget to mark TODAY
 * as done/not-done without opening the app.
 */
class HabitWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TODAY = "com.ghost.dothabit.ACTION_TOGGLE_TODAY"

        fun updateAll(context: Context, mgr: AppWidgetManager, ids: IntArray) {
            for (id in ids) {
                updateOne(context, mgr, id)
            }
        }

        private fun updateOne(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_dot_habit)
            views.setTextViewText(R.id.widgetHabitName, PrefsHelper.getHabitName(context))

            val today = Calendar.getInstance()
            val doneToday = PrefsHelper.isDone(context, today)
            views.setTextViewText(
                R.id.widgetTodayStatus,
                if (doneToday) "Today: done ✅ (tap to undo)" else "Today: not done — tap to mark"
            )

            val bitmap = renderDotGrid(context)
            views.setImageViewBitmap(R.id.widgetDotImage, bitmap)

            // Tapping anywhere on the widget toggles today's status.
            val intent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TODAY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetDotImage, pendingIntent)
            views.setOnClickPendingIntent(R.id.widgetTodayStatus, pendingIntent)

            mgr.updateAppWidget(widgetId, views)
        }

        private fun renderDotGrid(context: Context): Bitmap {
            val cols = 7
            val cellSize = 60
            val cal = Calendar.getInstance()
            val totalDays = PrefsHelper.daysInMonth(cal)
            val today = cal.get(Calendar.DAY_OF_MONTH)
            val rows = ((totalDays - 1) / cols) + 1

            val width = cols * cellSize
            val height = rows * cellSize
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }
            val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43A047") }
            val future = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3A3A3A") }
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

            val radius = cellSize * 0.35f
            for (day in 1..totalDays) {
                val idx = day - 1
                val row = idx / cols
                val col = idx % cols
                val cx = col * cellSize + cellSize / 2f
                val cy = row * cellSize + cellSize / 2f

                val paint = when {
                    day > today -> future
                    else -> {
                        val dayCal = Calendar.getInstance()
                        dayCal.set(Calendar.DAY_OF_MONTH, day)
                        if (PrefsHelper.isDone(context, dayCal)) green else red
                    }
                }
                canvas.drawCircle(cx, cy, radius, paint)
                if (day == today) canvas.drawCircle(cx, cy, radius + 4f, ring)
            }
            return bitmap
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TODAY) {
            PrefsHelper.toggleToday(context)
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(context, HabitWidgetProvider::class.java)
            )
            updateAll(context, mgr, ids)
        }
    }
}
