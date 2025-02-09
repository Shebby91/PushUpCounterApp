package com.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun WorkoutHistoryItem(
    record: WorkoutRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Formatierung: Datum und je nach Typ entweder die Anzahl oder die Zeit
    val detailsText = when (record.type) {
        WorkoutType.PUSH_UP -> "${record.count ?: 0} Push‑Ups"
        WorkoutType.PLANK -> {
            val millis = record.durationMillis ?: 0
            val minutes = millis / 60000
            val seconds = (millis % 60000) / 1000
            "$minutes m $seconds s Plank"
        }
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.date, style = MaterialTheme.typography.bodyLarge)
                Text(text = detailsText, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = Color.Blue)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red)
            }
        }
    }
}