package com.ghost.dothabit

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PrefsHelper {

    private const val PREFS = "dot_habit_prefs"
    private const val KEY_HABIT_NAME = "habit_name"
    private const val KEY_HABIT_IDS = "habit_ids"
    private const val KEY_SELECTED_HABIT_ID = "selected_habit_id"
    private const val DAY_PREFIX = "day_"
    private const val HABIT_PREFIX = "habit_"
    private const val WIDGET_HABIT_PREFIX = "widget_habit_"
    const val DEFAULT_HABIT_ID = "default"

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    enum class ChallengeLevel(val title: String, val targetDays: Int) {
        EASY("Easy", 3),
        MID("Mid", 7),
        HARD("Hard", 21)
    }

    data class Habit(
        val id: String,
        val name: String,
        val level: ChallengeLevel,
        val reward: String
    )

    fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getHabits(context: Context): List<Habit> {
        migrateLegacyHabit(context)
        return habitIds(context).map { id ->
            Habit(id, getHabitName(context, id), getChallengeLevel(context, id), getReward(context, id))
        }.ifEmpty {
            listOf(Habit(DEFAULT_HABIT_ID, "My Habit", ChallengeLevel.EASY, "Small treat"))
        }
    }

    fun getSelectedHabitId(context: Context): String {
        migrateLegacyHabit(context)
        val selected = prefs(context).getString(KEY_SELECTED_HABIT_ID, DEFAULT_HABIT_ID) ?: DEFAULT_HABIT_ID
        return if (habitIds(context).contains(selected)) selected else DEFAULT_HABIT_ID
    }

    fun setSelectedHabitId(context: Context, habitId: String) {
        prefs(context).edit().putString(KEY_SELECTED_HABIT_ID, habitId).apply()
    }

    fun getHabitName(context: Context, habitId: String = getSelectedHabitId(context)): String {
        migrateLegacyHabit(context)
        return prefs(context).getString(nameKey(habitId), null)
            ?: prefs(context).getString(KEY_HABIT_NAME, "My Habit")
            ?: "My Habit"
    }

    fun setHabitName(context: Context, name: String, habitId: String = getSelectedHabitId(context)) {
        prefs(context).edit().putString(nameKey(habitId), name).apply()
    }

    fun addHabit(
        context: Context,
        name: String,
        level: ChallengeLevel = ChallengeLevel.EASY,
        reward: String = "Small treat"
    ): String {
        val id = "custom_${System.currentTimeMillis()}"
        val ids = habitIds(context).toMutableList()
        ids.add(id)
        prefs(context).edit()
            .putString(KEY_HABIT_IDS, ids.joinToString(","))
            .putString(nameKey(id), name)
            .putString(levelKey(id), level.name)
            .putString(rewardKey(id), reward)
            .putString(KEY_SELECTED_HABIT_ID, id)
            .apply()
        return id
    }

    fun saveChallenge(context: Context, habitId: String, level: ChallengeLevel, reward: String) {
        prefs(context).edit()
            .putString(levelKey(habitId), level.name)
            .putString(rewardKey(habitId), reward.ifBlank { "Small treat" })
            .apply()
    }

    fun getChallengeLevel(context: Context, habitId: String = getSelectedHabitId(context)): ChallengeLevel {
        val raw = prefs(context).getString(levelKey(habitId), ChallengeLevel.EASY.name)
        return ChallengeLevel.values().firstOrNull { it.name == raw } ?: ChallengeLevel.EASY
    }

    fun getReward(context: Context, habitId: String = getSelectedHabitId(context)): String =
        prefs(context).getString(rewardKey(habitId), "Small treat") ?: "Small treat"

    fun autoLevelForReward(reward: String): ChallengeLevel {
        val text = reward.lowercase(Locale.US)
        val hardWords = listOf("phone", "trip", "shoes", "watch", "gaming", "expensive", "big", "500", "1000")
        val midWords = listOf("movie", "food", "pizza", "coffee", "book", "shirt", "snack", "200")
        return when {
            hardWords.any { text.contains(it) } || reward.length > 28 -> ChallengeLevel.HARD
            midWords.any { text.contains(it) } || reward.length > 12 -> ChallengeLevel.MID
            else -> ChallengeLevel.EASY
        }
    }

    fun deleteHabit(context: Context, habitId: String) {
        val ids = habitIds(context).filterNot { it == habitId }
        if (ids.isEmpty()) return
        val edit = prefs(context).edit()
            .putString(KEY_HABIT_IDS, ids.joinToString(","))
            .remove(nameKey(habitId))
            .remove(levelKey(habitId))
            .remove(rewardKey(habitId))
        prefs(context).all.keys
            .filter { it.startsWith("$HABIT_PREFIX$habitId$DAY_PREFIX") }
            .forEach { edit.remove(it) }
        if (getSelectedHabitId(context) == habitId) {
            edit.putString(KEY_SELECTED_HABIT_ID, ids.first())
        }
        edit.apply()
    }

    fun getWidgetHabitId(context: Context, widgetId: Int): String {
        val habitId = prefs(context).getString(WIDGET_HABIT_PREFIX + widgetId, null)
        return if (habitId != null && habitIds(context).contains(habitId)) habitId else getSelectedHabitId(context)
    }

    fun setWidgetHabitId(context: Context, widgetId: Int, habitId: String) {
        prefs(context).edit().putString(WIDGET_HABIT_PREFIX + widgetId, habitId).apply()
    }

    fun removeWidgetHabit(context: Context, widgetId: Int) {
        prefs(context).edit().remove(WIDGET_HABIT_PREFIX + widgetId).apply()
    }

    fun keyForDate(cal: Calendar, habitId: String = DEFAULT_HABIT_ID): String =
        "$HABIT_PREFIX$habitId$DAY_PREFIX${fmt.format(cal.time)}"

    fun isDone(context: Context, cal: Calendar, habitId: String = getSelectedHabitId(context)): Boolean =
        prefs(context).getBoolean(keyForDate(cal, habitId), false)

    fun setDone(context: Context, cal: Calendar, done: Boolean, habitId: String = getSelectedHabitId(context)) {
        prefs(context).edit().putBoolean(keyForDate(cal, habitId), done).apply()
    }

    fun toggleToday(context: Context, habitId: String = getSelectedHabitId(context)) {
        val today = Calendar.getInstance()
        val current = isDone(context, today, habitId)
        setDone(context, today, !current, habitId)
    }

    fun daysInMonth(cal: Calendar): Int =
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun currentStreak(context: Context, habitId: String): Int {
        val cal = Calendar.getInstance()
        var count = 0
        while (isDone(context, cal, habitId)) {
            count++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return count
    }

    private fun habitIds(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_HABIT_IDS, null)
        return raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf(DEFAULT_HABIT_ID)
    }

    private fun nameKey(habitId: String) = "${HABIT_PREFIX}${habitId}_name"
    private fun levelKey(habitId: String) = "${HABIT_PREFIX}${habitId}_level"
    private fun rewardKey(habitId: String) = "${HABIT_PREFIX}${habitId}_reward"

    private fun migrateLegacyHabit(context: Context) {
        val pref = prefs(context)
        if (pref.contains(KEY_HABIT_IDS)) return
        val legacyName = pref.getString(KEY_HABIT_NAME, "My Habit") ?: "My Habit"
        val edit = pref.edit()
            .putString(KEY_HABIT_IDS, DEFAULT_HABIT_ID)
            .putString(KEY_SELECTED_HABIT_ID, DEFAULT_HABIT_ID)
            .putString(nameKey(DEFAULT_HABIT_ID), legacyName)
            .putString(levelKey(DEFAULT_HABIT_ID), ChallengeLevel.EASY.name)
            .putString(rewardKey(DEFAULT_HABIT_ID), "Small treat")

        pref.all.keys
            .filter { it.startsWith(DAY_PREFIX) }
            .forEach { oldKey ->
                edit.putBoolean(
                    "$HABIT_PREFIX$DEFAULT_HABIT_ID$oldKey",
                    pref.getBoolean(oldKey, false)
                )
            }
        edit.apply()
    }
}
