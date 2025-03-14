package com.example.app

enum class WorkoutType {
    PUSH_UP,
    PLANK
}

data class WorkoutRecord(
    val date: String,
    val type: WorkoutType,
    val count: Int? = null,          // Wiederholungen für Push-Ups
    val sets: Int? = null,           // Anzahl der Sätze
    val durationMillis: Int? = null  // Nur für Planks
)