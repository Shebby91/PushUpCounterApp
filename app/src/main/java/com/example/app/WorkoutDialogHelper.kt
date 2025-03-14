package com.example.app
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun DeletePushUpDialog(
    record: WorkoutRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag löschen",color = MaterialTheme.colorScheme.onTertiary) },
        text = { Text("Möchtest du diesen Eintrag wirklich löschen?",color = MaterialTheme.colorScheme.onTertiary) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Löschen")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun DeletePlankDialog(
    record: WorkoutRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag löschen",color = MaterialTheme.colorScheme.onTertiary) },
        text = { Text("Möchtest du diesen Eintrag wirklich löschen?",color = MaterialTheme.colorScheme.onTertiary) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Löschen")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun EditPushUpDialog(
    record: WorkoutRecord,
    onDismiss: () -> Unit,
    onConfirm: (newCount: Int) -> Unit
) {
    var newCountText by remember { mutableStateOf(record.count?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag bearbeiten",color = MaterialTheme.colorScheme.onTertiary) },
        text = {
            OutlinedTextField(
                value = newCountText,
                onValueChange = { newCountText = it },
                label = { Text("Anzahl Push-Ups",color = MaterialTheme.colorScheme.onTertiary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = {
                val newCount = newCountText.toIntOrNull() ?: record.count ?: 0
                onConfirm(newCount)
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun EditPlankDialog(
    record: WorkoutRecord,
    onDismiss: () -> Unit,
    onConfirm: (newMinutes: Int, newSeconds: Int) -> Unit
) {
    // Berechnung der Ausgangswerte aus durationMillis
    val initialMinutes = (record.durationMillis ?: 0) / 60000
    val initialSeconds = ((record.durationMillis ?: 0) % 60000) / 1000
    var minutesText by remember { mutableStateOf(initialMinutes.toString()) }
    var secondsText by remember { mutableStateOf(initialSeconds.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag bearbeiten",color = MaterialTheme.colorScheme.onTertiary) },
        text = {
            Column {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = { Text("Minuten",color = MaterialTheme.colorScheme.onTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it },
                    label = { Text("Sekunden",color = MaterialTheme.colorScheme.onTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newMinutes = minutesText.toIntOrNull() ?: initialMinutes
                val newSeconds = secondsText.toIntOrNull() ?: initialSeconds
                onConfirm(newMinutes, newSeconds)
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
/*fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}*/