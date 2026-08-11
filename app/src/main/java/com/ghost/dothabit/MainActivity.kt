package com.ghost.dothabit

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
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
    private lateinit var challengeText: TextView
    private lateinit var rewardText: TextView
    private lateinit var challengeProgress: ProgressBar
    private lateinit var challengeButton: Button
    private lateinit var challengePanel: View
    private lateinit var coachText: TextView
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
        challengeText = findViewById(R.id.challengeText)
        rewardText = findViewById(R.id.rewardText)
        challengeProgress = findViewById(R.id.challengeProgress)
        challengeButton = findViewById(R.id.challengeButton)
        challengePanel = findViewById(R.id.challengePanel)
        coachText = findViewById(R.id.coachText)

        habitNameText.setOnClickListener { showHabitDialog() }
        addHabitButton.setOnClickListener { showHabitDialog(isNew = true) }
        editHabitButton.setOnClickListener { showHabitDialog() }
        deleteHabitButton.setOnClickListener { showDeleteDialog() }
        challengeButton.setOnClickListener { showChallengeDialog() }

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
            celebrateProgress()
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
        val streak = PrefsHelper.currentStreak(this, selectedHabit.id)
        val target = selectedHabit.level.targetDays
        val progress = ((streak.coerceAtMost(target) * 100f) / target).toInt()
        val daysLeft = (target - streak).coerceAtLeast(0)
        todayText.text = if (isDone) "Today is done" else "Today is waiting"
        streakText.text = "$streak day streak"
        challengeText.text = "${selectedHabit.level.title} challenge"
        rewardText.text = if (streak >= target) {
            "Reward unlocked: ${selectedHabit.reward}"
        } else {
            "Reward: ${selectedHabit.reward} after $daysLeft more days"
        }
        coachText.text = when {
            streak >= target -> "You earned it. Claim the reward, then raise the target."
            isDone -> "Locked in for today. Tomorrow keeps the chain alive."
            streak == 0 -> "Start the streak today. One tap begins the challenge."
            daysLeft == 1 -> "One more clean day unlocks the reward."
            else -> "$daysLeft days left. Keep the chain unbroken."
        }
        challengeProgress.progress = progress
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
                    if (isNew) {
                        PrefsHelper.addHabit(this, name)
                    } else {
                        PrefsHelper.setHabitName(this, name, currentId)
                    }
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

    private fun showChallengeDialog() {
        val habitId = PrefsHelper.getSelectedHabitId(this)
        val rewardInput = EditText(this).apply {
            hint = "Reward you want"
            setText(PrefsHelper.getReward(this@MainActivity, habitId))
            setSingleLine(false)
            minLines = 1
        }
        val levels = PrefsHelper.ChallengeLevel.values().toList()
        val levelGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(0, 18, 0, 8)
        }
        levels.forEachIndexed { index, level ->
            levelGroup.addView(RadioButton(this).apply {
                id = 7000 + index
                text = "${level.title} challenge - ${level.targetDays} days"
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#1A1D24"))
                setPadding(0, 8, 0, 8)
            })
        }
        val selectedLevelIndex = levels.indexOf(PrefsHelper.getChallengeLevel(this, habitId)).coerceAtLeast(0)
        levelGroup.check(7000 + selectedLevelIndex)

        val autoButton = Button(this).apply {
            text = "Auto choose from reward"
            setOnClickListener {
                val auto = PrefsHelper.autoLevelForReward(rewardInput.text.toString())
                levelGroup.check(7000 + levels.indexOf(auto))
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 8, 0)
            addView(rewardInput)
            addView(levelGroup)
            addView(autoButton)
        }

        AlertDialog.Builder(this)
            .setTitle("Challenge reward")
            .setView(content)
            .setPositiveButton("Save") { _, _ ->
                val levelIndex = (levelGroup.checkedRadioButtonId - 7000).coerceIn(0, levels.lastIndex)
                val level = levels[levelIndex]
                PrefsHelper.saveChallenge(this, habitId, level, rewardInput.text.toString().trim())
                refreshUi()
                refreshWidgets()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun celebrateProgress() {
        dotGrid.animate().cancel()
        challengePanel.animate().cancel()
        dotGrid.scaleX = 0.98f
        dotGrid.scaleY = 0.98f
        challengePanel.alpha = 0.78f
        challengePanel.translationY = 10f
        dotGrid.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
        challengePanel.animate().alpha(1f).translationY(0f).setDuration(260).start()
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, HabitWidgetProvider::class.java))
        HabitWidgetProvider.updateAll(this, mgr, ids)
    }
}
