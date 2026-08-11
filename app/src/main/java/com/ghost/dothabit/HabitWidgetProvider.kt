package com.ghost.dothabit

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import java.util.Calendar

class HabitWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_TODAY = "com.ghost.dothabit.ACTION_TOGGLE_TODAY"
        const val EXTRA_WIDGET_ID = "widget_id"

        fun updateAll(context: Context, mgr: AppWidgetManager, ids: IntArray) {
            for (id in ids) {
                updateOne(context, mgr, id)
            }
        }

        private fun updateOne(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val habitId = PrefsHelper.getWidgetHabitId(context, widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_dot_habit)
            views.setTextViewText(R.id.widgetHabitName, PrefsHelper.getHabitName(context, habitId))

            val today = Calendar.getInstance()
            val doneToday = PrefsHelper.isDone(context, today, habitId)
            val level = PrefsHelper.getChallengeLevel(context, habitId)
            val streak = PrefsHelper.currentStreak(context, habitId)
            val progressText = "${level.title}: ${streak.coerceAtMost(level.targetDays)}/${level.targetDays} days"
            views.setTextViewText(
                R.id.widgetTodayStatus,
                when {
                    streak >= level.targetDays -> "Unlocked: ${PrefsHelper.getReward(context, habitId)}"
                    doneToday -> "$progressText - keep going"
                    else -> "$progressText - tap today"
                }
            )

            views.setImageViewBitmap(R.id.widgetDotImage, renderDotGrid(context, habitId))

            val toggleIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TODAY
                putExtra(EXTRA_WIDGET_ID, widgetId)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                widgetId + 10000,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widgetDotImage, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.widgetTodayStatus, togglePendingIntent)
            views.setOnClickPendingIntent(R.id.widgetHabitName, configPendingIntent)

            mgr.updateAppWidget(widgetId, views)
        }

        private fun renderDotGrid(context: Context, habitId: String): Bitmap {
            val cols = 7
            val cellSize = 60
            val cal = Calendar.getInstance()
            val totalDays = PrefsHelper.daysInMonth(cal)
            val today = cal.get(Calendar.DAY_OF_MONTH)
            val rows = ((totalDays - 1) / cols) + 1

            val bitmap = Bitmap.createBitmap(cols * cellSize, rows * cellSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5A6A") }
            val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#44D27A") }
            val future = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#384052") }
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
                        if (PrefsHelper.isDone(context, dayCal, habitId)) green else red
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
            val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            PrefsHelper.toggleToday(context, PrefsHelper.getWidgetHabitId(context, widgetId))
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
            updateAll(context, mgr, ids)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { PrefsHelper.removeWidgetHabit(context, it) }
    }
}
