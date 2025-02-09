package com.example.app

enum class WorkoutType {
    PUSH_UP,
    PLANK
}

data class WorkoutRecord(
    val date: String,
    val type: WorkoutType,
    val count: Int? = null,          // Nur für Push-Ups
    val durationMillis: Int? = null  // Nur für Planks
)