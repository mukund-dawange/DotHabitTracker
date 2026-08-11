package com.ghost.dothabit

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Fires shortly after midnight every day so the dot graph "moves on"
 * to the new day automatically (new day = red until marked). Also
 * reschedules itself after a phone reboot.
 */
class MidnightReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_MIDNIGHT_TICK = "com.ghost.dothabit.ACTION_MIDNIGHT_TICK"

        fun scheduleNextMidnight(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MidnightReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_TICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val next = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.timeInMillis,
                pendingIntent
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Refresh all widgets so the new day's (red) dot shows up.
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
        HabitWidgetProvider.updateAll(context, mgr, ids)

        // Line up tomorrow's tick too (works for both BOOT_COMPLETED and the daily tick).
        scheduleNextMidnight(context)
    }
}
