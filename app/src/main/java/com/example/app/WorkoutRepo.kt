package com.example.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WorkoutHistoryRepository {
    private const val PREFS_NAME = "workout_prefs"
    private const val KEY_HISTORY = "workout_history"

    fun loadHistory(context: Context): List<WorkoutRecord> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_HISTORY, null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<List<WorkoutRecord>>() {}.type)
        } else {
            emptyList()
        }
    }

    fun saveHistory(context: Context, history: List<WorkoutRecord>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(history)
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply()
    }

    // Methode, um einen bestehenden Eintrag zu aktualisieren
    fun updateRecord(context: Context, updatedRecord: WorkoutRecord) {
        val currentHistory = loadHistory(context).toMutableList()
        val index = currentHistory.indexOfFirst { it.date == updatedRecord.date && it.type == updatedRecord.type && it.durationMillis == updatedRecord.durationMillis }

        if (index != -1) {
            // Eintrag gefunden, aktualisiere die Sets
            currentHistory[index] = updatedRecord
            saveHistory(context, currentHistory)
        }
    }

    // Methode, um ein neues Workout hinzuzufügen oder ein bestehendes zu aktualisieren
    fun addOrUpdateRecord(context: Context, newRecord: WorkoutRecord) {
        val currentHistory = loadHistory(context).toMutableList()
        val existingRecord = currentHistory.find { it.date == newRecord.date && it.type == newRecord.type && it.durationMillis == newRecord.durationMillis }

        if (existingRecord != null) {
            // Wenn der Eintrag existiert, erhöhe die Sets
            val updatedRecord = existingRecord.copy(sets = existingRecord.sets?.plus(1))
            updateRecord(context, updatedRecord)
        } else {
            // Wenn der Eintrag nicht existiert, füge ihn hinzu
            currentHistory.add(newRecord)
            saveHistory(context, currentHistory)
        }
    }
}