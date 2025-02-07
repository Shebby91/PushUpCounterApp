package com.example.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.AppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                PushUpCounterScreen()
            }
        }
    }
}

@Composable
fun PushUpCounterScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("push_up_prefs", Context.MODE_PRIVATE)

    // Ziel speichern
    var dailyGoal by remember { mutableIntStateOf(sharedPreferences.getInt("daily_goal", 50)) }
    var count by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(loadHistory(sharedPreferences)) }
    var editEntry by remember { mutableStateOf<Pair<String, Int>?>(null) } // Für Bearbeiten
    var deleteEntry by remember { mutableStateOf<String?>(null) } // Für Bestätigung beim Löschen
    var showGoalAchievedDialog by remember { mutableStateOf(false) } // Flag für Ziel erreicht Dialog
    // Fortschritt berechnen

    val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
    val currentDayCount = history[currentDate] ?: 0
    val goalReachedToday = currentDayCount == dailyGoal - 1 // Überprüfen, ob Ziel erreicht wurde

    // Dialog-Status
    var showDialog by remember { mutableStateOf(false) }

    // Funktionen zum Speichern
    fun saveGoalToPreferences(value: Int) {
        sharedPreferences.edit().putInt("daily_goal", value).apply()
        // Dialog anzeigen
        showDialog = true
    }

    fun saveHistoryToPreferences() {
        val gson = Gson()
        val json = gson.toJson(history)
        sharedPreferences.edit().putString("push_up_history", json).apply()
    }

    fun addPushUpRecord() {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        history = history.toMutableMap().apply {
            put(date, (this[date] ?: 0) + 1)
        }
        saveHistoryToPreferences()
    }

    fun confirmDeleteEntry(date: String) {
        deleteEntry = date // Setzt das zu löschende Datum
    }

    fun performDeleteEntry(date: String) {
        history = history.toMutableMap().apply {
            remove(date)
        }
        saveHistoryToPreferences()
        deleteEntry = null // Dialog schließen
    }

    fun updateEntry(date: String, newCount: Int) {
        history = history.toMutableMap().apply {
            if (newCount > 0) put(date, newCount) else remove(date)
        }
        saveHistoryToPreferences()
    }

    // Testweise 10 Datumseinträge hinzufügen
    fun addTestEntries() {
        val testHistory = mutableMapOf<String, Int>()
        for (i in 1..6) {
            val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date().apply { time = time - (i * 86400000L) }) // Tägliche Rückdaten
            testHistory[date] = (1..dailyGoal).random() // Zufällige Anzahl von Push-Ups für jedes Datum
        }
        history = testHistory
        saveHistoryToPreferences()
    }

    // Bei der ersten Initialisierung die Test-Daten hinzufügen
    LaunchedEffect(Unit) {
        addTestEntries()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Ziel-Eingabe
        OutlinedTextField(
            value = dailyGoal.toString(),
            onValueChange = { dailyGoal = it.toIntOrNull() ?: dailyGoal },
            label = { Text("Tägliches Ziel (Push-Ups)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Ziel speichern Button
        Button(
            onClick = {
                saveGoalToPreferences(dailyGoal)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Ziel speichern")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Push-Up Verlauf", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(history.toList().sortedByDescending { it.first }) { (date, amount) ->
                val isGoalMet = amount >= dailyGoal
                val progress = (amount.toFloat() / dailyGoal)
                // Kachel für jedes Datum
                ListItem(date, amount, progress, isGoalMet,{ editEntry = date to amount }, { confirmDeleteEntry(date) }, dailyGoal)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Push-Ups: $count", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                count++
                addPushUpRecord()
                if (goalReachedToday) {
                    showGoalAchievedDialog = true // Ziel erreicht, Dialog anzeigen
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Push-Up hinzufügen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                count = 0
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Zurücksetzen")
        }
    }

    // Einfache AlertDialog-Box anzeigen
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Speichern erfolgreich") },
            text = { Text("Das tägliche Ziel wurde erfolgreich gespeichert.") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog für das Erreichen des Ziels
    if (showGoalAchievedDialog) {
        AlertDialog(
            onDismissRequest = { showGoalAchievedDialog = false },
            title = { Text("Ziel erreicht!") },
            text = { Text("Herzlichen Glückwunsch! Du hast dein tägliches Ziel von $dailyGoal Push-Ups erreicht.") },
            confirmButton = {
                Button(onClick = { showGoalAchievedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Bearbeitungsdialog anzeigen
    editEntry?.let { (date, oldValue) ->
        EditDialog(date, oldValue) { newCount ->
            updateEntry(date, newCount)
            editEntry = null
        }
    }

    // Bestätigungsdialog für Löschen anzeigen
    deleteEntry?.let { date ->
        DeleteConfirmationDialog(
            date = date,
            onConfirm = { performDeleteEntry(date) },
            onDismiss = { deleteEntry = null }
        )
    }
}

@Composable
fun ListItem(date: String, amount: Int, progress: Float, isSuccess: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, dailyGoal: Int) {
    // Berechne die Überschüsse (falls das Ziel überschritten wurde)
    val overGoalList = mutableListOf<Int>()
    var remainingAmount = amount

    while (remainingAmount > dailyGoal) {
        overGoalList.add(dailyGoal)
        remainingAmount -= dailyGoal
    }

    if (remainingAmount > 0) {
        overGoalList.add(remainingAmount) // Falls noch Restbetrag übrig bleibt
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)) // Hellgraue Farbe
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 16.dp,16.dp,4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f) // Datum & Anzahl links
            ) {
                Text(text = date, style = MaterialTheme.typography.bodyLarge)
                Text(text = "$amount von $dailyGoal Push-Ups", style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                horizontalArrangement = Arrangement.End // Buttons nach rechts schieben
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = Color.Blue)
                }
            }
        }
        // Fortschrittsanzeigen für das tägliche Ziel
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp,0.dp,16.dp,16.dp)
            ,
            verticalAlignment = Alignment.CenterVertically
        ){
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (isSuccess) Color.Green else Color.Gray,
            )
        }
        // Zweite ProgressBar für den Überschuss
        overGoalList.forEachIndexed { index, overAmount ->
            val progressOverGoal = overAmount.toFloat() / dailyGoal
            if (index > 0){
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp,0.dp,16.dp,16.dp)
                    ,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    LinearProgressIndicator(
                        progress = { progressOverGoal },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (isSuccess) Color.Green else Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
fun EditDialog(date: String, oldValue: Int, onSave: (Int) -> Unit) {
    var newCount by remember { mutableStateOf(oldValue.toString()) }

    AlertDialog(
        onDismissRequest = { onSave(oldValue) },
        title = { Text("Push-Ups bearbeiten") },
        text = {
            Column {
                Text("Neuer Wert für $date:")
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

@Composable
fun DeleteConfirmationDialog(date: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag löschen") },
        text = { Text("Möchtest du den Eintrag für $date wirklich löschen?") },
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

fun loadHistory(sharedPreferences: android.content.SharedPreferences): Map<String, Int> {
    val gson = Gson()
    val json = sharedPreferences.getString("push_up_history", null)
    val type = object : TypeToken<Map<String, Int>>() {}.type
    return gson.fromJson(json, type) ?: emptyMap()
}
