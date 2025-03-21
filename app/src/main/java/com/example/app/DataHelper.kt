package com.example.app

enum class WorkoutType {
    PUSH_UP,
    PLANK,
    MOUNTAIN_CLIMBER,
    SQUAT,
    LUNGE,
    ROWING,
    CRUNCHES,
    SHOULDER_PRESS,
    BURPEES,
    LEG_RAISES,
    TRIZEPS_DIPS
}

data class WorkoutRecord(
    val date: String,
    val type: WorkoutType,
    val count: Int? = null,
    val sets: Int? = null,
    val durationMillis: Int? = null,

    // Die Zielwerte werden jetzt fest mit dem Eintrag gespeichert
    val goalReps: Int? = null,       // Ziel-Wiederholungen pro Satz
    val goalSets: Int? = null,       // Ziel-Anzahl der Sätze
)