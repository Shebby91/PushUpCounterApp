package com.example.app
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
fun DeleteCounterWorkoutDialog(
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
fun EditCounterWorkoutDialog(
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
                label = { Text("Anzahl",color = MaterialTheme.colorScheme.onTertiary) },
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
fun EditWorkoutDialog(
    record: WorkoutRecord,
    onDismiss: () -> Unit,
    onConfirm: (newMinutes: Int, newSeconds: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag bearbeiten") },
        text = { Text("Hier kannst du die Werte bearbeiten (nicht vollständig implementiert).") },
        confirmButton = {
            Button(onClick = { onConfirm(0, 0) }) {
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
fun DeleteWorkoutDialog(
    record: WorkoutRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag löschen") },
        text = { Text("Möchtest du diesen Eintrag wirklich löschen?") },
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
/*fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}*/