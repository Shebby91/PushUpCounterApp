package com.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutHistoryItem(
    record: WorkoutRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    // Unterschiedliche Detailtexte je nach Typ
    val detailsText = when (record.type) {
        WorkoutType.PUSH_UP -> "${record.count ?: 0} Push‑Ups"
        WorkoutType.SQUAT -> "${record.count ?: 0} Kniebeugen"
        WorkoutType.LUNGE -> "${record.count ?: 0} Schritte"
        WorkoutType.ROWING -> "${record.count ?: 0} Rudern"
        WorkoutType.CRUNCHES -> "${record.count ?: 0} Bauchpresse"
        WorkoutType.SHOULDER_PRESS-> "${record.count ?: 0} Schulterpresse"
        WorkoutType.BURPEES-> "${record.count ?: 0} Burpees"
        WorkoutType.LEG_RAISES-> "${record.count ?: 0} Beinheben"
        WorkoutType.TRIZEPS_DIPS-> "${record.count ?: 0} Dips"
        WorkoutType.PLANK -> {
            val (targetMinutes, targetSeconds, _) = WorkoutSettingsRepository.getPlankTargetTime(context)
            "$targetMinutes m $targetSeconds s Plank"
        }
    }

    // Errechne Ziel und erreichte Sätze abhängig vom Typ
    val (totalSets, completedSets) = when (record.type) {
        WorkoutType.PUSH_UP -> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getPushUpGoal(context)
            val completedPushUps = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedPushUps / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.SQUAT -> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getSquatGoal(context)
            val completedSquats = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedSquats / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.LUNGE -> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getLungeGoal(context)
            val completedLunges = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedLunges / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.ROWING-> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getRowingGoal(context)
            val completedRowing = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedRowing / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.CRUNCHES-> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getCrunchesGoal(context)
            val completedCrunches = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedCrunches / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.SHOULDER_PRESS-> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getShoulderPressGoal(context)
            val completedShoulderPress = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedShoulderPress / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.BURPEES-> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getBurpeesGoal(context)
            val completedBurpees = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedBurpees / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.LEG_RAISES-> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getLegRaisesGoal(context)
            val completedLegRaises = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedLegRaises / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.TRIZEPS_DIPS-> {
            val (goalReps, goalSets) = WorkoutSettingsRepository.getTrizepsDipsGoal(context)
            val completedDips = record.count ?: 0
            // Errechnete Sätze (begrenzen auf das Ziel)
            val setsDone = (completedDips / goalReps).coerceAtMost(goalSets)
            Pair(goalSets, setsDone)
        }
        WorkoutType.PLANK -> {
            // Für Planks holen wir das Ziel aus dem Triple
            val (_, _, targetSets) = WorkoutSettingsRepository.getPlankTargetTime(context)
            // Hier nehmen wir an, dass record.sets die Anzahl der abgeschlossenen Plank-Sätze enthält
            val setsDone = record.sets ?: 0
            Pair(targetSets, setsDone)
        }
        else -> Pair(0, 0)
    }

    // Erstelle Checkmarks: Für jedes Ziel-Set ein Icon, das grün wird, wenn der Satz erledigt wurde, sonst grau
    val checkmarks = List(totalSets) { index -> index < completedSets }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Erste Spalte: Datum und Details
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.date, style = MaterialTheme.typography.bodyLarge)
                Text(text = detailsText, style = MaterialTheme.typography.bodyMedium)
            }

            // Zweite Spalte: Checkmarks und Text
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    checkmarks.forEach { isCompleted ->
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Häkchen",
                            tint = if (isCompleted) MaterialTheme.colorScheme.tertiary else Color.Gray
                        )
                    }
                }
                Text(text = "Sätze: $completedSets / $totalSets")
            }

            // Dritte Spalte: Optionen (Edit / Delete)
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = Color.Blue)
                    }
                }
            }
        }
    }
}