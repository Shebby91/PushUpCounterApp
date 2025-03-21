package com.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
        WorkoutType.SQUAT -> "${record.count ?: 0} Kniebeug."
        WorkoutType.LUNGE -> "${record.count ?: 0} Schritte"
        WorkoutType.ROWING -> "${record.count ?: 0} Rudern"
        WorkoutType.CRUNCHES -> "${record.count ?: 0} Crunches"
        WorkoutType.SHOULDER_PRESS-> "${record.count ?: 0} Schulterpr."
        WorkoutType.BURPEES-> "${record.count ?: 0} Burpees"
        WorkoutType.LEG_RAISES-> "${record.count ?: 0} Beinheben"
        WorkoutType.TRIZEPS_DIPS-> "${record.count ?: 0} Dips"
        WorkoutType.PLANK -> "${record.durationMillis?.toLong()?.let { formatTime(it) }?: 0}"
        WorkoutType.MOUNTAIN_CLIMBER -> "Dauer: ${record.durationMillis?.div(1000) ?: 0} Sek."
    }

    // Errechne Ziel und erreichte Sätze abhängig vom Typ
    val totalSets = record.goalSets ?: 0
    val goalReps = record.goalReps ?: 0
    val completedSets = when (record.type) {
        WorkoutType.PLANK, WorkoutType.MOUNTAIN_CLIMBER -> record.sets ?: 0
        else -> (record.count ?: 0) / goalReps
    }

    // Erstelle Checkmarks: Für jedes Ziel-Set ein Icon, das grün wird, wenn der Satz erledigt wurde, sonst grau
    val checkmarks = if (totalSets > 0) List(totalSets) { index -> index < completedSets } else emptyList()

    Card(
        modifier = Modifier
            .shadow(4.dp, shape = RoundedCornerShape(14.dp))
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Erste Spalte: Datum und Details
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = record.date, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = detailsText, color = MaterialTheme.colorScheme.surface)
            }

            // Zweite Spalte: Checkmarks (falls vorhanden)
            if (checkmarks.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        checkmarks.forEach { isCompleted ->
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Häkchen",
                                tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                            )
                        }
                    }

                    Text(text = "Sätze: $completedSets / $totalSets", color = MaterialTheme.colorScheme.surface)
                }
            }

            // Dritte Spalte: Optionen (Edit / Delete)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.background)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = MaterialTheme.colorScheme.background)
                    }
                }
            }
        }
    }
}