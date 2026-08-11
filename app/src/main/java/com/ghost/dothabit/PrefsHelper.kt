package com.ghost.dothabit

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * All data lives in SharedPreferences so both the Activity and the Widget
 * (same process) can read/write it instantly without a database.
 *
 * Key format for each day: "day_yyyy-MM-dd" -> Boolean
 *   true  = manually marked DONE   -> GREEN dot
 *   missing/false = default        -> RED dot (this is the "automatic" mark)
 */
object PrefsHelper {

    private const val PREFS = "dot_habit_prefs"
    private const val KEY_HABIT_NAME = "habit_name"
    private const val DAY_PREFIX = "day_"

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getHabitName(context: Context): String =
        prefs(context).getString(KEY_HABIT_NAME, "My Habit") ?: "My Habit"

    fun setHabitName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_HABIT_NAME, name).apply()
    }

    fun keyForDate(cal: Calendar): String = DAY_PREFIX + fmt.format(cal.time)

    fun isDone(context: Context, cal: Calendar): Boolean =
        prefs(context).getBoolean(keyForDate(cal), false)

    fun setDone(context: Context, cal: Calendar, done: Boolean) {
        prefs(context).edit().putBoolean(keyForDate(cal), done).apply()
    }

    fun toggleToday(context: Context) {
        val today = Calendar.getInstance()
        val current = isDone(context, today)
        setDone(context, today, !current)
    }

    /** Days in the currently displayed month (30 or 31, or 28/29 for Feb). */
    fun daysInMonth(cal: Calendar): Int =
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}
