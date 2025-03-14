package com.example.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.compose.foundation.Image
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DarkTheme {
                AppNavigation()
                //PushUpCounterScreen()
            }
        }
    }
}

class PushUpSensorListener(context: Context, private val onPushUpDetected: () -> Unit) :
    SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var previousZ = 0f
    private var pushUpInProgress = false
    private var pushUpStartTime = 0L
    private var lastPushUpTime = 0L

    private val minPushUpDuration = 400  // Angepasst: Realistischere Zeit
    private val minZdiff = 1.5f
    private val minTotalMovement = 2.5f  // Angepasst: Weniger Distanz notwendig

    private var zValues = mutableListOf<Float>()

    private var calibratedZ = 0f  // Neue Variable: Speichert den Kalibrierungswert
    private var isCalibrated = false  // Neue Variable: Zeigt an, ob kalibriert wurde

    // Vibrator für Vibrationseffekt
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    fun calibrate() {
        isCalibrated = false  // Vorherige Kalibrierung zurücksetzen
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val currentZ = it.values[2]
            val timestamp = System.currentTimeMillis()

            if (!isCalibrated) {
                calibratedZ = currentZ
                isCalibrated = true
                return
            }

            if (zValues.size > 5) zValues.removeAt(0)
            zValues.add(currentZ)
            val smoothedZ = zValues.average().toFloat()

            val deltaZ = previousZ - smoothedZ

            if (!pushUpInProgress && deltaZ > minZdiff) {
                pushUpInProgress = true
                pushUpStartTime = timestamp
            }

            if (pushUpInProgress && smoothedZ - previousZ > minZdiff) {
                val pushUpDuration = timestamp - pushUpStartTime
                val totalMovement = abs(previousZ - smoothedZ)

                if (pushUpDuration > minPushUpDuration && totalMovement > minTotalMovement) {
                    pushUpInProgress = false
                    lastPushUpTime = timestamp
                    onPushUpDetected()
                    vibratePhone()
                }
            }

            previousZ = smoothedZ
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun vibratePhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Neue Vibrations-API für Android 8+ (Oreo)
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

}

@SuppressLint("StaticFieldLeak")
class WorkoutTimerViewModel(private val context: Context, val workoutType: WorkoutType) : ViewModel() {
    var minutes by mutableIntStateOf(0)
    var seconds by mutableIntStateOf(30) // Standardzeit 30 Sekunden
    var goalSets by mutableIntStateOf(3)
    var remainingTime by mutableIntStateOf(0)
    var isRunning by mutableStateOf(false)
    var history by mutableStateOf<List<WorkoutRecord>>(WorkoutHistoryRepository.loadHistory(context))
    var progress by mutableFloatStateOf(0f)
    var remainingTimeText by mutableStateOf("")
    private var timer: CountDownTimer? = null

    init {
        loadTargetTime()
    }

    fun startTimer() {
        if (isRunning) return
        val totalMillis = (minutes * 60 + seconds) * 1000L
        remainingTime = totalMillis.toInt()
        isRunning = true
        progress = 0f
        remainingTimeText = formatTime(remainingTime)

        timer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished.toInt()
                remainingTimeText = formatTime(remainingTime)
                progress = (1 - millisUntilFinished.toFloat() / totalMillis)
            }

            override fun onFinish() {
                isRunning = false
                saveWorkoutRecord()
            }
        }.start()
    }

    fun stopTimer() {
        timer?.cancel()
        isRunning = false
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(timeInMillis: Int): String {
        val minutes = timeInMillis / 1000 / 60
        val seconds = (timeInMillis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun saveWorkoutRecord() {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val totalMilliseconds = (minutes * 60 + seconds) * 1000

        val record = WorkoutRecord(
            date = date,
            type = workoutType,
            durationMillis = totalMilliseconds,
            sets = 1
        )

        WorkoutHistoryRepository.addOrUpdateRecord(context, record)
        history = WorkoutHistoryRepository.loadHistory(context)
    }

    fun saveTargetTime() {
        WorkoutSettingsRepository.saveTargetTime(context, workoutType, minutes, seconds, goalSets)
    }

    private fun loadTargetTime() {
        val (min, sec, sets) = WorkoutSettingsRepository.getTargetTime(context, workoutType)
        minutes = min
        seconds = sec
        goalSets = sets
    }

    fun updateRecord(updatedRecord: WorkoutRecord, originalRecord: WorkoutRecord) {
        val currentHistory = history.toMutableList()
        val index = currentHistory.indexOf(originalRecord)
        if (index != -1) {
            currentHistory[index] = updatedRecord
            WorkoutHistoryRepository.saveHistory(context, currentHistory)
            history = currentHistory
        }
    }

    fun deleteRecord(record: WorkoutRecord) {
        val currentHistory = history.toMutableList()
        currentHistory.remove(record)
        WorkoutHistoryRepository.saveHistory(context, currentHistory)
        history = currentHistory
    }

}



@Composable
fun DarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFC107),
            secondary = Color(0xFF4527A0),
            background = Color(0xFF212529),
            surface = Color(0xFFF8F9FA),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.Black,
            onBackground = Color(0xFFB0A8B9),
            onSurface = Color(0xFFB0A8B9),
            tertiary = Color(0xFF198754)
        ),
        typography = Typography(),
        content = content
    )
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController, startDestination = "start") {
        composable("start") { StartScreen(navController) }
        composable("pushups") { CounterScreen(WorkoutType.PUSH_UP) }
        composable("squat") { CounterScreen(WorkoutType.SQUAT) }
        composable("lunge") { CounterScreen(WorkoutType.LUNGE) }
        composable("rowing") { CounterScreen(WorkoutType.ROWING) }
        composable("crunches") { CounterScreen(WorkoutType.CRUNCHES) }
        composable("shoulderpress") { CounterScreen(WorkoutType.SHOULDER_PRESS) }
        composable("burpees") { CounterScreen(WorkoutType.BURPEES) }
        composable("legraises") { CounterScreen(WorkoutType.LEG_RAISES) }
        composable("trizepsdips") { CounterScreen(WorkoutType.TRIZEPS_DIPS) }
        composable("planks") { WorkoutTimerScreen(WorkoutTimerViewModel(context,WorkoutType.PLANK)) }
        composable("climber") { WorkoutTimerScreen(WorkoutTimerViewModel(context,WorkoutType.MOUNTAIN_CLIMBER)) }
        //composable("overview") {  }
    }
}

@Composable
fun StartScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App-Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(250.dp)
            )
            //Text(text = "Power-App", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            // Erste Reihe mit zwei Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("pushups") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Push-Ups", color = MaterialTheme.colorScheme.onPrimary)
                }

                Button(
                    onClick = { navController.navigate("planks") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Planks", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            // Zweite Reihe mit zwei Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("squat") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Squats", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { navController.navigate("climber") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("MountainClimber", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            // Dritte Reihe mit zwei Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("rowing") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Rowing", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { navController.navigate("burpees") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Burpees", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            //Vierte Reihe mit zwei Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("lunge") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Lunges", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { navController.navigate("legraises") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Leg Raises", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            //Fünfte Reihe mit zwei Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("shoulderpress") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Shoulder Press", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { navController.navigate("trizepsdips") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Trizeps-Dips", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            //Fünfte Reihe mit zwei Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("crunches") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Crunches", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { /*navController.navigate("overview")*/ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Daily Goals", color = MaterialTheme.colorScheme.onSecondary)
                }
            }

        }
    }
}

@Composable
fun WorkoutTimerScreen(viewModel: WorkoutTimerViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    val filteredHistory = viewModel.history.filter { it.type == viewModel.workoutType }
    var editRecord by remember { mutableStateOf<WorkoutRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<WorkoutRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Text(text = "${viewModel.workoutType.name} Timer", style = MaterialTheme.typography.headlineLarge)

        Row {
            TextField(
                value = viewModel.minutes.toString(),
                onValueChange = { viewModel.minutes = it.toIntOrNull() ?: 0 },
                label = { Text("Minuten") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = viewModel.seconds.toString(),
                onValueChange = { viewModel.seconds = it.toIntOrNull() ?: 30 },
                label = { Text("Sekunden") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = viewModel.goalSets.toString(),
                onValueChange = { viewModel.goalSets = it.toIntOrNull() ?: 3 },
                label = { Text("Sätze") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.saveTargetTime()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = "Ziel speichern")
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filteredHistory) { record ->
                WorkoutHistoryItem(record = record, onEdit = { editRecord = record }, onDelete = { deleteRecord = record })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = viewModel.remainingTimeText, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.startTimer() },
            enabled = !viewModel.isRunning,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) /*{
            Text(text = "${viewModel.workoutType.name} starten")
        }*/
        {
            Text(
                text = when (viewModel.workoutType.name) {
                    WorkoutType.PLANK.toString() -> "Plank starten"
                    WorkoutType.MOUNTAIN_CLIMBER.toString() -> "Mountain-Climber starten"
                    else -> {"Wiederholung hinzufügen"}
                },
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.stopTimer() },
            enabled = viewModel.isRunning,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(text = "Stop")
        }
    }

    if (showDialog) {
        WorkoutAlert(
            title = "Speichern erfolgreich",
            message = "Das tägliche Ziel wurde erfolgreich gespeichert",
            onDismiss = { showDialog = false }
        )
    }

    editRecord?.let { record ->
        EditWorkoutDialog(record = record, onDismiss = { editRecord = null }) { newMinutes, newSeconds ->
            val newDuration = (newMinutes * 60 + newSeconds) * 1000
            val updated = record.copy(durationMillis = newDuration)
            viewModel.updateRecord(updated, record)
            editRecord = null
        }
    }

    deleteRecord?.let { record ->
        DeleteWorkoutDialog(record = record, onDismiss = { deleteRecord = null }) {
            viewModel.deleteRecord(record)
            deleteRecord = null
        }
    }
}

@Composable
fun CounterScreen(workoutType: WorkoutType) {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(WorkoutHistoryRepository.loadHistory(context)) }
    var editRecord by remember { mutableStateOf<WorkoutRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<WorkoutRecord?>(null) }
    var showCalibratedDialog by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("30") }
    var sets by remember { mutableStateOf("3") }

    // Abrufen der Push-Up-Ziele aus SharedPreferences
    LaunchedEffect(context) {
        when (workoutType) {
            WorkoutType.PUSH_UP -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getPushUpGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.SQUAT -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getSquatGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.LUNGE -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getLungeGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.ROWING -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getRowingGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.CRUNCHES -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getCrunchesGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.SHOULDER_PRESS -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getShoulderPressGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.BURPEES -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getBurpeesGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.LEG_RAISES -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getLegRaisesGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.TRIZEPS_DIPS -> {
                val (savedReps, savedSets) = WorkoutSettingsRepository.getTrizepsDipsGoal(context)
                reps = savedReps.toString()
                sets = savedSets.toString()
            }
            WorkoutType.PLANK -> {
                // Hier kannst du die Darstellung für Planks anpassen
                val (minutes, seconds, savedSets) = WorkoutSettingsRepository.getTargetTime(context,
                    WorkoutType.PLANK
                )
                reps = minutes.toString()  // Beispiel: Minuten als 'Reps'
                sets = seconds.toString()  // Beispiel: Sekunden als 'Sätze'
            }
            WorkoutType.MOUNTAIN_CLIMBER -> {
                // Hier kannst du die Darstellung für Planks anpassen
                val (minutes, seconds, savedSets) = WorkoutSettingsRepository.getTargetTime(context,
                    WorkoutType.MOUNTAIN_CLIMBER
                )
                reps = minutes.toString()  // Beispiel: Minuten als 'Reps'
                sets = seconds.toString()  // Beispiel: Sekunden als 'Sätze'
            }
        }
    }
    // Dialog-Status
    var showDialog by remember { mutableStateOf(false) }

    // Aktualisieren eines Eintrags in der History
    fun updateRecord(updatedRecord: WorkoutRecord, originalRecord: WorkoutRecord) {
        val currentHistory = WorkoutHistoryRepository.loadHistory(context).toMutableList()
        val index = currentHistory.indexOf(originalRecord)
        if (index != -1) {
            currentHistory[index] = updatedRecord
            WorkoutHistoryRepository.saveHistory(context, currentHistory)
            history = currentHistory
        }
    }

    // Löschen eines Eintrags aus der History
    fun deleteRecord(record: WorkoutRecord) {
        val currentHistory = WorkoutHistoryRepository.loadHistory(context).toMutableList()
        currentHistory.remove(record)
        WorkoutHistoryRepository.saveHistory(context, currentHistory)
        history = currentHistory
    }

    // Speichern des Ziels (Vereinheitlicht über WorkoutSettingsRepository)


    fun addWorkoutRecord() {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val currentHistory = WorkoutHistoryRepository.loadHistory(context).toMutableList()
        // Für wiederholungsbasierte Workouts (Push‑Ups, Kniebeugen)
        val existingRecord = currentHistory.find { it.date == date && it.type == workoutType }
        if (existingRecord != null) {
            val updatedRecord = existingRecord.copy(count = (existingRecord.count ?: 0) + 1)
            currentHistory.remove(existingRecord)
            currentHistory.add(updatedRecord)
        } else {
            val newRecord = WorkoutRecord(date = date, type = workoutType, count = 1)
            currentHistory.add(newRecord)
        }
        WorkoutHistoryRepository.saveHistory(context, currentHistory)
        history = currentHistory
    }

   val sensorListener = remember { PushUpSensorListener(context) { count++; addWorkoutRecord() } }

   LaunchedEffect(Unit) {
       sensorListener.register()
   }

   DisposableEffect(Unit) {
       onDispose { sensorListener.unregister() }
   }

    val filteredHistory = history.filter { it.type == workoutType }

   Column(
       modifier = Modifier
           .fillMaxSize()
           .background(MaterialTheme.colorScheme.background)
           .padding(horizontal = 16.dp, vertical = 64.dp),
       horizontalAlignment = Alignment.CenterHorizontally
   ) {

       Row(
           horizontalArrangement = Arrangement.spacedBy(16.dp), // Abstand zwischen den Textfeldern
           verticalAlignment = Alignment.CenterVertically // Vertikale Ausrichtung
       ) {
           TextField(
               value = reps,
               onValueChange = { reps = it },
               label = { Text("Wiederholungen") },
               keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
               modifier = Modifier.weight(1f) // TextField nimmt gleichmäßig Platz ein
           )

           TextField(
               value = sets,
               onValueChange = { sets = it },
               label = { Text("Sätze") },
               keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
               modifier = Modifier.weight(1f) // TextField nimmt gleichmäßig Platz ein
           )
       }

       Spacer(modifier = Modifier.height(24.dp))

       // Ziel speichern Button
       Button(
           onClick = {
               val repsInt = reps.toIntOrNull() ?: 30
               val setsInt = sets.toIntOrNull() ?: 3
               when (workoutType) {
                   WorkoutType.PUSH_UP -> WorkoutSettingsRepository.savePushUpGoal(context, repsInt, setsInt)
                   WorkoutType.SQUAT -> WorkoutSettingsRepository.saveSquatGoal(context, repsInt, setsInt)
                   WorkoutType.LUNGE -> WorkoutSettingsRepository.saveLungeGoal(context, repsInt, setsInt)
                   WorkoutType.ROWING -> WorkoutSettingsRepository.saveRowingGoal(context, repsInt, setsInt)
                   WorkoutType.CRUNCHES -> WorkoutSettingsRepository.saveCrunchesGoal(context, repsInt, setsInt)
                   WorkoutType.SHOULDER_PRESS -> WorkoutSettingsRepository.saveShoulderPressGoal(context, repsInt, setsInt)
                   WorkoutType.BURPEES -> WorkoutSettingsRepository.saveBurpeesGoal(context, repsInt, setsInt)
                   WorkoutType.LEG_RAISES -> WorkoutSettingsRepository.saveLegRaisesGoal(context, repsInt, setsInt)
                   WorkoutType.TRIZEPS_DIPS -> WorkoutSettingsRepository.saveTrizepsDipsGoal(context, repsInt, setsInt)
                   WorkoutType.PLANK -> WorkoutSettingsRepository.saveTargetTime(context, WorkoutType.PLANK,repsInt, setsInt, setsInt)
                   WorkoutType.MOUNTAIN_CLIMBER -> WorkoutSettingsRepository.saveTargetTime(context, WorkoutType.PLANK,repsInt, setsInt, setsInt) // Anpassen, falls nötig
               }
               showDialog = true
           },
           modifier = Modifier.fillMaxWidth(),
           colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
       ) {
           Text(text = "Ziel speichern", color = MaterialTheme.colorScheme.onPrimary)
       }

       Spacer(modifier = Modifier.height(24.dp))

       /*Text(
           text = "Push-Up Verlauf",
           color = MaterialTheme.colorScheme.onPrimary,
           style = MaterialTheme.typography.titleLarge
       )*/
       LazyColumn(
           modifier = Modifier
               .weight(1f)
               .fillMaxWidth(),
           contentPadding = PaddingValues(vertical = 8.dp)
       ) {
           items(filteredHistory) { record ->
               WorkoutHistoryItem(
                   record = record,
                   onEdit = { editRecord = record },
                   onDelete = { deleteRecord = record }
               )
           }
       }

       Spacer(modifier = Modifier.height(24.dp))

       Text(
           text = "$count",
           color = MaterialTheme.colorScheme.onPrimary,
           style = MaterialTheme.typography.headlineLarge
       )

       Spacer(modifier = Modifier.height(16.dp))




       Row(
           horizontalArrangement = Arrangement.spacedBy(16.dp), // Abstand zwischen den Textfeldern
           verticalAlignment = Alignment.CenterVertically // Vertikale Ausrichtung
       ) {
           Button(
               onClick = {
                   count++
                   addWorkoutRecord()
               },
               modifier = Modifier
                   .fillMaxWidth(), // Volle Breite für den oberen Button
               colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
           ) {
               Text(
                   text = when (workoutType) {
                       WorkoutType.PUSH_UP -> "Push‑Up hinzufügen"
                       WorkoutType.SQUAT -> "Kniebeuge hinzufügen"
                       WorkoutType.LUNGE -> "Ausfallschritt hinzufügen"
                       WorkoutType.ROWING -> "Rudern hinzufügen"
                       WorkoutType.CRUNCHES -> "Bauchpresse hinzufügen"
                       WorkoutType.SHOULDER_PRESS -> "Schulterpresse hinzufügen"
                       WorkoutType.BURPEES -> "Burpee hinzufügen"
                       WorkoutType.LEG_RAISES -> "Beinheben hinzufügen"
                       WorkoutType.TRIZEPS_DIPS -> "Dips hinzufügen"
                       WorkoutType.PLANK -> "Plank hinzufügen"
                       WorkoutType.MOUNTAIN_CLIMBER -> "Mountain-Climber hinzufügen"
                   },
                   color = MaterialTheme.colorScheme.onPrimary
               )
           }
       }

       Spacer(modifier = Modifier.height(16.dp))

       Row(
           horizontalArrangement = Arrangement.spacedBy(16.dp), // Abstand zwischen den Buttons
           verticalAlignment = Alignment.CenterVertically // Vertikale Ausrichtung
       ) {
           OutlinedButton(
               onClick = {
                   sensorListener.calibrate()
                   showCalibratedDialog = true
               },
               modifier = Modifier
                   .weight(1f) // 50% der Breite, weil beide Buttons das gleiche Gewicht haben
                   .height(50.dp),
           ) {
               Text(text = "Kalibrieren", color = MaterialTheme.colorScheme.onSecondary)
           }

           Button(
               onClick = {
                   count = 0
                   vibratePhone(context, 100)
               },
               modifier = Modifier
                   .weight(1f) // 50% der Breite
                   .height(50.dp),
               colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
           ) {
               Text(text = "Zurücksetzen", color = MaterialTheme.colorScheme.onSecondary)
           }
       }
   }

   // Einfache AlertDialog-Box anzeigen
   if (showDialog) {
       vibratePhone(context,100)
       WorkoutAlert(title = "Speichern erfolgreich", message = "Das tägliche Ziel wurde erfolgreich gespeichert", onDismiss = { showDialog = false })
   }

   // Dialog für das Erreichen des Ziels
  /* if (showGoalAchievedDialog) {
       vibratePhone(context,400)
       WorkoutAlert(title = "Ziel erreicht!", message = "Herzlichen Glückwunsch! Du hast dein tägliches Ziel von $dailyGoal Push-Ups erreicht.", onDismiss = { showGoalAchievedDialog = false })
   }
*/
   // Dialog für das Erreichen des Ziels
   if (showCalibratedDialog) {
       vibratePhone(context,100)
       WorkoutAlert(title = "Erfolgreich kalibriert!", message = "Kalibrierung wurde erfolgreich zurückgesetzt.", onDismiss = { showCalibratedDialog = false })
   }

    editRecord?.let { record ->
        EditPushUpDialog(
            record = record,
            onDismiss = { editRecord = null },
            onConfirm = { newCount ->
                val updated = record.copy(count = newCount)
                updateRecord(updated, record)
                editRecord = null
            }
        )
    }

    deleteRecord?.let { record ->
        DeletePushUpDialog(
            record = record,
            onDismiss = { deleteRecord = null },
            onConfirm = {
                deleteRecord(record)
                deleteRecord = null
            }
        )
    }
}