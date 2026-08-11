package com.ghost.dothabit

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dotGrid: DotGridView
    private lateinit var habitNameText: TextView
    private lateinit var monthText: TextView
    private lateinit var habitSpinner: Spinner
    private lateinit var addHabitButton: Button
    private lateinit var editHabitButton: Button
    private lateinit var deleteHabitButton: Button
    private lateinit var streakText: TextView
    private lateinit var todayText: TextView
    private var habits: List<PrefsHelper.Habit> = emptyList()
    private var isRefreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        habitNameText = findViewById(R.id.habitNameText)
        monthText = findViewById(R.id.monthText)
        dotGrid = findViewById(R.id.dotGrid)
        habitSpinner = findViewById(R.id.habitSpinner)
        addHabitButton = findViewById(R.id.addHabitButton)
        editHabitButton = findViewById(R.id.editHabitButton)
        deleteHabitButton = findViewById(R.id.deleteHabitButton)
        streakText = findViewById(R.id.streakText)
        todayText = findViewById(R.id.todayText)

        habitNameText.setOnClickListener { showHabitDialog() }
        addHabitButton.setOnClickListener { showHabitDialog(isNew = true) }
        editHabitButton.setOnClickListener { showHabitDialog() }
        deleteHabitButton.setOnClickListener { showDeleteDialog() }

        habitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isRefreshing) return
                val picked = habits.getOrNull(position) ?: return
                PrefsHelper.setSelectedHabitId(this@MainActivity, picked.id)
                refreshUi()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        dotGrid.onDayTapped = { day ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, day)
            val habitId = PrefsHelper.getSelectedHabitId(this)
            PrefsHelper.setDone(this, cal, !PrefsHelper.isDone(this, cal, habitId), habitId)
            refreshUi()
            refreshWidgets()
        }

        MidnightReceiver.scheduleNextMidnight(this)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        habits = PrefsHelper.getHabits(this)
        val selectedId = PrefsHelper.getSelectedHabitId(this)
        val selectedIndex = habits.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        val selectedHabit = habits[selectedIndex]

        isRefreshing = true
        habitSpinner.adapter = ArrayAdapter(this, R.layout.habit_spinner_item, habits.map { it.name }).apply {
            setDropDownViewResource(R.layout.habit_choice_item)
        }
        habitSpinner.setSelection(selectedIndex, false)
        isRefreshing = false

        dotGrid.habitId = selectedHabit.id
        habitNameText.text = selectedHabit.name
        monthText.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        val today = Calendar.getInstance()
        val isDone = PrefsHelper.isDone(this, today, selectedHabit.id)
        todayText.text = if (isDone) "Today is done" else "Today is waiting"
        streakText.text = "${currentStreak(selectedHabit.id)} day streak"
        deleteHabitButton.isEnabled = habits.size > 1
        dotGrid.invalidate()
    }

    private fun showHabitDialog(isNew: Boolean = false) {
        val input = EditText(this)
        val currentId = PrefsHelper.getSelectedHabitId(this)
        if (!isNew) input.setText(PrefsHelper.getHabitName(this, currentId))
        AlertDialog.Builder(this)
            .setTitle(if (isNew) "Add habit" else "Edit habit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (isNew) PrefsHelper.addHabit(this, name) else PrefsHelper.setHabitName(this, name, currentId)
                    refreshUi()
                    refreshWidgets()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog() {
        val id = PrefsHelper.getSelectedHabitId(this)
        AlertDialog.Builder(this)
            .setTitle("Delete habit?")
            .setMessage("This removes the habit and its dots from this phone.")
            .setPositiveButton("Delete") { _, _ ->
                PrefsHelper.deleteHabit(this, id)
                refreshUi()
                refreshWidgets()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun currentStreak(habitId: String): Int {
        val cal = Calendar.getInstance()
        var count = 0
        while (PrefsHelper.isDone(this, cal, habitId)) {
            count++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return count
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, HabitWidgetProvider::class.java))
        HabitWidgetProvider.updateAll(this, mgr, ids)
    }
}
