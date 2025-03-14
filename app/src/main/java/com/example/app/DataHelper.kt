package com.example.app

enum class WorkoutType {
    PUSH_UP,
    PLANK,
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
    val count: Int? = null,          // Wiederholungen für Push-Ups
    val sets: Int? = null,           // Anzahl der Sätze
    val durationMillis: Int? = null  // Nur für Planks
)