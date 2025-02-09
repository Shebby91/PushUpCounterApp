package com.example.app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DeleteConfirmationDialog(date: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag löschen",color = MaterialTheme.colorScheme.onTertiary) },
        text = { Text("Möchtest du den Eintrag für $date wirklich löschen?",color = MaterialTheme.colorScheme.onTertiary) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Löschen")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun EditDialog(date: String, oldValue: Int, onSave: (Int) -> Unit) {
    var newCount by remember { mutableStateOf(oldValue.toString()) }

    AlertDialog(
        onDismissRequest = { onSave(oldValue) },
        title = { Text("Push-Ups bearbeiten",color = MaterialTheme.colorScheme.onTertiary) },
        text = {
            Column {
                Text("Neuer Wert für $date:" ,color = MaterialTheme.colorScheme.onTertiary)
                OutlinedTextField(
                    value = newCount,
                    onValueChange = { newCount = it.filter { char -> char.isDigit() } },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(newCount.toIntOrNull() ?: oldValue)
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onSave(oldValue) }) {
                Text("Abbrechen")
            }
        }
    )
}

/*fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}*/