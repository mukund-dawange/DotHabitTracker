package com.ghost.dothabit

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var habits: List<PrefsHelper.Habit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        habits = PrefsHelper.getHabits(this)
        val list = findViewById<ListView>(R.id.widgetHabitList)
        list.adapter = ArrayAdapter(this, R.layout.habit_choice_item, habits.map { it.name })
        list.choiceMode = ListView.CHOICE_MODE_SINGLE
        list.setItemChecked(0, true)
        list.setOnItemClickListener { _, _, position, _ -> saveChoice(position) }
        findViewById<Button>(R.id.widgetSaveButton).setOnClickListener {
            saveChoice(list.checkedItemPosition.coerceAtLeast(0))
        }
    }

    private fun saveChoice(position: Int) {
        val habit = habits.getOrNull(position) ?: return
        PrefsHelper.setWidgetHabitId(this, widgetId, habit.id)
        val mgr = AppWidgetManager.getInstance(this)
        HabitWidgetProvider.updateAll(this, mgr, intArrayOf(widgetId))

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
