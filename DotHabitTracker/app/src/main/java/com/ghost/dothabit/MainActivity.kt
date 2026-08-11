package com.ghost.dothabit

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dotGrid: DotGridView
    private lateinit var habitNameText: TextView
    private lateinit var monthText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        habitNameText = findViewById(R.id.habitNameText)
        monthText = findViewById(R.id.monthText)
        dotGrid = findViewById(R.id.dotGrid)

        habitNameText.setOnClickListener { showRenameDialog() }

        dotGrid.onDayTapped = { day ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, day)
            val newState = !PrefsHelper.isDone(this, cal)
            PrefsHelper.setDone(this, cal, newState)
            dotGrid.invalidate()
            refreshWidgets()
        }

        // Schedule the midnight rollover so a new (red) dot appears automatically each day.
        MidnightReceiver.scheduleNextMidnight(this)

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        habitNameText.text = PrefsHelper.getHabitName(this)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthText.text = sdf.format(Calendar.getInstance().time)
        dotGrid.invalidate()
    }

    private fun showRenameDialog() {
        val input = EditText(this)
        input.setText(PrefsHelper.getHabitName(this))
        AlertDialog.Builder(this)
            .setTitle("Name your habit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    PrefsHelper.setHabitName(this, name)
                    refreshUi()
                    refreshWidgets()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, HabitWidgetProvider::class.java))
        HabitWidgetProvider.updateAll(this, mgr, ids)
    }
}
