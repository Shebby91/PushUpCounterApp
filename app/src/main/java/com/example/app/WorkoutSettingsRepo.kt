package com.example.app

import android.content.Context


object WorkoutSettingsRepository {
    private const val PREFS_NAME = "workout_settings"
    // Schlüssel für PLANK
    private const val KEY_PLANK_MINUTES = "plank_target_minutes"
    private const val KEY_PLANK_SECONDS = "plank_target_seconds"
    private const val KEY_PLANK_SETS = "plank_sets"

    // Für MOUNTAIN_CLIMBER definieren wir eigene Schlüssel
    private const val KEY_MC_MINUTES = "mountain_climber_target_minutes"
    private const val KEY_MC_SECONDS = "mountain_climber_target_seconds"
    private const val KEY_MC_SETS = "mountain_climber_sets"

    // Schlüssel für Push-Ups
    private const val KEY_PUSHUP_GOAL = "pushup_daily_goal"
    private const val KEY_PUSHUP_SETS = "pushup_sets"

    // Schlüssel für Squats
    private const val KEY_SQUAT_GOAL = "squat_daily_goal"
    private const val KEY_SQUAT_SETS = "squat_sets"

    // Schlüssel für Lunges
    private const val KEY_LUNGE_GOAL = "lunge_daily_goal"
    private const val KEY_LUNGE_SETS = "lunge_sets"

    // Schlüssel für Rowing
    private const val KEY_ROWING_GOAL = "rowing_daily_goal"
    private const val KEY_ROWING_SETS = "rowing_sets"

    // Schlüssel für Rowing
    private const val KEY_CRUNCHES_GOAL = "crunches_daily_goal"
    private const val KEY_CRUNCHES_SETS = "crunches_sets"


    private const val KEY_SHOULDER_PRESS_GOAL = "shoulder_press_daily_goal"
    private const val KEY_SHOULDER_PRESS_SETS = "shoulder_press_sets"

    private const val KEY_BURPEES_GOAL = "burpees_daily_goal"
    private const val KEY_BURPEES_SETS = "burpees_sets"

    private const val KEY_LEG_RAISES_GOAL = "leg_raises_daily_goal"
    private const val KEY_LEG_RAISES_SETS = "leg_raises_sets"

    private const val KEY_TRIZEPS_DIPS_GOAL = "trizeps_dips_daily_goal"
    private const val KEY_TRIZEPS_DIPS_SETS = "trizeps_dips_sets"

    // Speichern und Abrufen für Push-Ups
    fun savePushUpGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PUSHUP_GOAL, goal)
            .putInt(KEY_PUSHUP_SETS, sets)
            .apply()
    }

    fun getPushUpGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_PUSHUP_GOAL, 15) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_PUSHUP_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    // Speichern des Zielwertes – je nach WorkoutType
    fun saveTargetTime(context: Context, workoutType: WorkoutType, minutes: Int, seconds: Int, sets: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (workoutType) {
            WorkoutType.PLANK -> {
                prefs.edit()
                    .putInt(KEY_PLANK_MINUTES, minutes)
                    .putInt(KEY_PLANK_SECONDS, seconds)
                    .putInt(KEY_PLANK_SETS, sets)
                    .apply()
            }
            WorkoutType.MOUNTAIN_CLIMBER -> {
                prefs.edit()
                    .putInt(KEY_MC_MINUTES, minutes)
                    .putInt(KEY_MC_SECONDS, seconds)
                    .putInt(KEY_MC_SETS, sets)
                    .apply()
            }
            else -> {
                val keyMinutes = "${workoutType.name.lowercase()}_target_minutes"
                val keySeconds = "${workoutType.name.lowercase()}_target_seconds"
                val keySets = "${workoutType.name.lowercase()}_sets"
                prefs.edit()
                    .putInt(keyMinutes, minutes)
                    .putInt(keySeconds, seconds)
                    .putInt(keySets, sets)
                    .apply()
            }
        }
    }

    // Abrufen des Zielwertes – Rückgabe als Triple (Minuten, Sekunden, Sätze)
    fun getTargetTime(context: Context, workoutType: WorkoutType): Triple<Int, Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (workoutType) {
            WorkoutType.PLANK -> {
                val minutes = prefs.getInt(KEY_PLANK_MINUTES, 0)
                val seconds = prefs.getInt(KEY_PLANK_SECONDS, 45)
                val sets = prefs.getInt(KEY_PLANK_SETS, 3)
                Triple(minutes, seconds, sets)
            }
            WorkoutType.MOUNTAIN_CLIMBER -> {
                val minutes = prefs.getInt(KEY_MC_MINUTES, 0)
                val seconds = prefs.getInt(KEY_MC_SECONDS, 20)
                val sets = prefs.getInt(KEY_MC_SETS, 3)
                Triple(minutes, seconds, sets)
            }
            else -> {
                val keyMinutes = "${workoutType.name.lowercase()}_target_minutes"
                val keySeconds = "${workoutType.name.lowercase()}_target_seconds"
                val keySets = "${workoutType.name.lowercase()}_sets"
                val minutes = prefs.getInt(keyMinutes, 0)
                val seconds = prefs.getInt(keySeconds, 30)
                val sets = prefs.getInt(keySets, 3)
                Triple(minutes, seconds, sets)
            }
        }
    }

    // Speichern und Abrufen für Squats
    fun saveSquatGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SQUAT_GOAL, goal)
            .putInt(KEY_SQUAT_SETS, sets)
            .apply()
    }

    fun getSquatGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_SQUAT_GOAL, 12) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_SQUAT_SETS, 4)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    // Speichern und Abrufen für Squats
    fun saveLungeGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LUNGE_GOAL, goal)
            .putInt(KEY_LUNGE_SETS, sets)
            .apply()
    }

    fun getLungeGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_LUNGE_GOAL, 10) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_LUNGE_SETS, 6)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }
    // Speichern und Abrufen für Squats
    fun saveRowingGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ROWING_GOAL, goal)
            .putInt(KEY_ROWING_SETS, sets)
            .apply()
    }

    fun getRowingGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_ROWING_GOAL, 12) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_ROWING_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    // Speichern und Abrufen für Squats
    fun saveCrunchesGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CRUNCHES_GOAL, goal)
            .putInt(KEY_CRUNCHES_SETS, sets)
            .apply()
    }

    fun getCrunchesGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_CRUNCHES_GOAL, 15) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_CRUNCHES_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    // Speichern und Abrufen für Squats
    fun saveShoulderPressGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SHOULDER_PRESS_GOAL, goal)
            .putInt(KEY_SHOULDER_PRESS_SETS, sets)
            .apply()
    }

    fun getShoulderPressGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_SHOULDER_PRESS_GOAL, 12) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_SHOULDER_PRESS_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    fun saveBurpeesGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SHOULDER_PRESS_GOAL, goal)
            .putInt(KEY_SHOULDER_PRESS_SETS, sets)
            .apply()
    }

    fun getBurpeesGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_BURPEES_GOAL, 8) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_BURPEES_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    fun saveLegRaisesGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LEG_RAISES_GOAL, goal)
            .putInt(KEY_LEG_RAISES_SETS, sets)
            .apply()
    }

    fun getLegRaisesGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_LEG_RAISES_GOAL, 10) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_LEG_RAISES_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    fun saveTrizepsDipsGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TRIZEPS_DIPS_GOAL, goal)
            .putInt(KEY_TRIZEPS_DIPS_SETS, sets)
            .apply()
    }

    fun getTrizepsDipsGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_TRIZEPS_DIPS_GOAL, 12) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_TRIZEPS_DIPS_SETS, 3)    // Standard: 3 Sätze
        return Pair(reps, sets)
    }
}