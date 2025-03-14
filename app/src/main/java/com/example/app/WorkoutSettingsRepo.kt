package com.example.app

import android.content.Context


object WorkoutSettingsRepository {
    private const val PREFS_NAME = "workout_settings"
    private const val KEY_PUSHUP_GOAL = "pushup_daily_goal"
    private const val KEY_PLANK_MINUTES = "plank_target_minutes"
    private const val KEY_PLANK_SECONDS = "plank_target_seconds"
    private const val KEY_PUSHUP_SETS = "pushup_sets"
    private const val KEY_PLANK_SETS = "plank_sets"

    // Speichern der Anzahl der Sätze
    fun savePushUpGoal(context: Context, goal: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PUSHUP_GOAL, goal)
            .putInt(KEY_PUSHUP_SETS, sets)
            .apply()
    }

    // Abrufen der Push-Up-Ziele (inklusive der Sätze)
    fun getPushUpGoal(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reps = prefs.getInt(KEY_PUSHUP_GOAL, 30) // Standard: 30 Wiederholungen
        val sets = prefs.getInt(KEY_PUSHUP_SETS, 3)  // Standard: 3 Sätze
        return Pair(reps, sets)
    }

    fun savePlankTargetTime(context: Context, minutes: Int, seconds: Int, sets: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PLANK_MINUTES, minutes)
            .putInt(KEY_PLANK_SECONDS, seconds)
            .putInt(KEY_PLANK_SETS, sets)
            .apply()
    }

    // Abrufen der Plank-Ziele (Minuten, Sekunden, Sätze)
    fun getPlankTargetTime(context: Context): Triple<Int, Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val minutes = prefs.getInt(KEY_PLANK_MINUTES, 0)  // Standard: 0 Minuten
        val seconds = prefs.getInt(KEY_PLANK_SECONDS, 30) // Standard: 30 Sekunden
        val sets = prefs.getInt(KEY_PLANK_SETS, 3)        // Standard: 3 Sätze
        return Triple(minutes, seconds, sets)
    }
}