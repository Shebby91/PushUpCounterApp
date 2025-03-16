package com.example.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
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
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DarkTheme {
                AppNavigation()
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
    var history by mutableStateOf(WorkoutHistoryRepository.loadHistory(context))
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
        composable("overview") { DailyOverviewScreen() }
        composable("stats") { TotalWorkoutOverviewScreen(context) }
        composable("achievements") { TotalWorkoutOverviewScreen(context) }
        composable("dataTransfer") { DataTransferScreen(navController) }
    }
}
@Composable
fun StartScreen(navController: NavController) {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader().use { reader ->
                    val json = reader?.readText() ?: ""
                    importWorkoutHistory(context, json)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fehler beim Import: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        // Festes Logo oben
        Image(
            painter = painterResource(id = R.drawable.logo_cropped),
            contentDescription = "App Logo",
            modifier = Modifier.size(90.dp)
        )
        Text(
            text = "Welcome back, Sebastian!",
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )


        // Scrollbarer Bereich mit Übungen
        Box(modifier = Modifier
            .weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val exercises = listOf(
                    "Kniebeuge" to "squat",
                    "Burpees" to "burpees",
                    "Ausfallschritte" to "lunge",
                    "Mountain-Climber" to "climber",
                    "Push-Ups" to "pushups",
                    "Planks" to "planks",
                    "Schulterpresse" to "shoulderpress",
                    "Beinheben" to "legraises",
                    "Rudern" to "rowing",
                    "Triceps-Dips" to "trizepsdips",
                    "Crunches" to "crunches",
                )
                val goals = listOf(
                    "Tägliche Ziele" to "overview",
                    "Statistik" to "stats",
                    "Errungenschaften" to "achievements"
                )
                items(exercises) { (title, route) ->
                    ExerciseTile(navController = navController, label = title, route = route, )
                }
                items(goals) { (title, route) ->
                    GoalTile(navController = navController, label = title, route = route, )
                }
                item {
                    ActionTile(label = "Workout importieren", type = "import", onClick = { importLauncher.launch(arrayOf("application/json")) })
                }
                item {
                    ActionTile(label = "Workout exportieren", type = "export", onClick = { exportWorkoutHistory(context) })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "©2025 Sebastian Grauthoff - App Version 1.0",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ExerciseTile(navController: NavController, label: String, route: String) {
    val imageId = when (route) {
        "squat" -> R.drawable.squats
        "burpees" -> R.drawable.burpees
        "lunge" -> R.drawable.lunges
        "climber" -> R.drawable.mountainclimber
        "pushups" -> R.drawable.pushups
        "planks" -> R.drawable.planks
        "shoulderpress" -> R.drawable.shoulderpress
        "legraises" -> R.drawable.legraises
        "rowing" -> R.drawable.rowing
        "trizepsdips" -> R.drawable.trizepsdips
        "crunches" -> R.drawable.crunches
        "overview" -> R.drawable.logo
        else -> R.drawable.logo
    }

    Column(
        modifier = Modifier
            .shadow(10.dp, shape = RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .clickable { navController.navigate(route) }
            .padding(12.dp)
            .aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActionTile(label: String, type: String, onClick: () -> Unit) {
    val (imageId, borderColor) = when (type) {
        "export" -> R.drawable.export to MaterialTheme.colorScheme.onBackground
        "import" -> R.drawable.sync to MaterialTheme.colorScheme.onBackground
        else -> R.drawable.logo to Color.Gray
    }

    Column(
        modifier = Modifier
            .shadow(10.dp, shape = RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GoalTile(navController: NavController, label: String, route: String) {
    val imageId = when (route) {
        "stats" -> R.drawable.stats
        "overview" -> R.drawable.goals
        "achievements" -> R.drawable.achievments
        else -> R.drawable.logo
    }


    Column(
        modifier = Modifier
            .shadow(10.dp, shape = RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .clickable { navController.navigate(route) }
            .padding(12.dp)
            .aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = null,
            modifier = Modifier.size(125.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WorkoutTimerScreen(viewModel: WorkoutTimerViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    val filteredHistory = viewModel.history.filter { it.type == viewModel.workoutType }.reversed()
    var editRecord by remember { mutableStateOf<WorkoutRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<WorkoutRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            modifier = Modifier.shadow(10.dp, shape = RoundedCornerShape(8.dp)).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = "Ziel speichern")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (viewModel.workoutType.name) {
                WorkoutType.PLANK.toString() -> "Plank Verlauf"
                WorkoutType.MOUNTAIN_CLIMBER.toString() -> "Mountain-Climber Verlauf"
                else -> {""}
            },
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
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
        Spacer(modifier = Modifier.height(8.dp))
        if (viewModel.isRunning) {
            Text(
                text = viewModel.remainingTimeText,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
        } else {
            Text(
                text = "00:00",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = { viewModel.startTimer() },
            enabled = !viewModel.isRunning,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        )
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
        Spacer(modifier = Modifier.height(16.dp))
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
                val (minutes, seconds) = WorkoutSettingsRepository.getTargetTime(context,
                    WorkoutType.PLANK
                )
                reps = minutes.toString()
                sets = seconds.toString()
            }
            WorkoutType.MOUNTAIN_CLIMBER -> {
                // Hier kannst du die Darstellung für Planks anpassen
                val (minutes, seconds) = WorkoutSettingsRepository.getTargetTime(context,
                    WorkoutType.MOUNTAIN_CLIMBER
                )
                reps = minutes.toString()
                sets = seconds.toString()
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
   val filteredHistory = history.filter { it.type == workoutType }.reversed()
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
           modifier = Modifier.shadow(10.dp, shape = RoundedCornerShape(8.dp)).fillMaxWidth(),
           colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
       ) {
           Text(text = "Ziel speichern", color = MaterialTheme.colorScheme.onPrimary)
       }
       Spacer(modifier = Modifier.height(16.dp))
       Text(
           text = when (workoutType) {
               WorkoutType.PUSH_UP -> "Push‑Up Verlauf"
               WorkoutType.SQUAT -> "Kniebeugen Verlauf"
               WorkoutType.LUNGE -> "Ausfallschritte Verlauf"
               WorkoutType.ROWING -> "Rudern Verlauf"
               WorkoutType.CRUNCHES -> "Crunches Verlauf"
               WorkoutType.SHOULDER_PRESS -> "Schulterpresse Verlauf"
               WorkoutType.BURPEES -> "Burpee Verlauf"
               WorkoutType.LEG_RAISES -> "Beinheben Verlauf"
               WorkoutType.TRIZEPS_DIPS -> "Trizeps-Dips Verlauf"
               WorkoutType.PLANK -> "Plank Verlauf"
               WorkoutType.MOUNTAIN_CLIMBER -> "Mountain-Climber Verlauf"
           },
           color = MaterialTheme.colorScheme.surface,
           style = MaterialTheme.typography.headlineSmall
       )
       Spacer(modifier = Modifier.height(8.dp))
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
       Spacer(modifier = Modifier.height(8.dp))
       Text(
           text = "$count",
           color = MaterialTheme.colorScheme.onPrimary,
           style = MaterialTheme.typography.headlineMedium
       )

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
       Spacer(modifier = Modifier.height(8.dp))
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
   if (showCalibratedDialog) {
       vibratePhone(context,100)
       WorkoutAlert(title = "Erfolgreich kalibriert!", message = "Kalibrierung wurde erfolgreich zurückgesetzt.", onDismiss = { showCalibratedDialog = false })
   }
   editRecord?.let { record ->
        EditCounterWorkoutDialog(
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
        DeleteCounterWorkoutDialog(
            record = record,
            onDismiss = { deleteRecord = null },
            onConfirm = {
                deleteRecord(record)
                deleteRecord = null
            }
        )
   }
}

@Composable
fun DailyOverviewScreen() {
    val context = LocalContext.current
    // Alle gespeicherten Records laden
    val allRecords = WorkoutHistoryRepository.loadHistory(context)
    // Gruppiere die Records nach Datum (das Datumsformat muss dabei konsistent sein)
    val recordsByDate = allRecords.groupBy { it.date }.toSortedMap(Comparator.reverseOrder())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f),verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            // Festes Logo oben
            Image(
                painter = painterResource(id = R.drawable.goals_cropped),
                contentDescription = "App Logo",
                modifier = Modifier.size(90.dp)
            )
            Text(
                text = "Tägliche Ziele",
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                recordsByDate.forEach { (date, recordsForDate) ->
                    item {
                        Card(
                            modifier = Modifier
                                .shadow(10.dp, shape = RoundedCornerShape(8.dp))
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = date, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                // Gruppiere für den jeweiligen Tag die Records nach Workout-Typ
                                val recordsByType = recordsForDate.groupBy { it.type }
                                recordsByType.forEach { (type, recordsOfType) ->
                                    // Berechnung der erreichten und der Ziel-Sätze je nach Typ:
                                    val (targetSets, achievedSets) = when (type) {
                                        // Wiederholungsbasierte Workouts
                                        WorkoutType.PUSH_UP -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getPushUpGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            // Errechnete Sätze = (gesamte Wiederholungen / Wiederholungen pro Satz), begrenzt auf das Ziel
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.SQUAT -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getSquatGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.LUNGE -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getLungeGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.ROWING -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getRowingGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.CRUNCHES -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getCrunchesGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.SHOULDER_PRESS -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getShoulderPressGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.BURPEES -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getBurpeesGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.LEG_RAISES -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getLegRaisesGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }

                                        WorkoutType.TRIZEPS_DIPS -> {
                                            val (goalReps, goalSets) = WorkoutSettingsRepository.getTrizepsDipsGoal(
                                                context
                                            )
                                            val totalReps = recordsOfType.sumOf { it.count ?: 0 }
                                            val achieved =
                                                (totalReps / goalReps).coerceAtMost(goalSets)
                                            Pair(goalSets, achieved)
                                        }
                                        // Timer-basierte Workouts (PLANK und MOUNTAIN_CLIMBER)
                                        WorkoutType.PLANK, WorkoutType.MOUNTAIN_CLIMBER -> {
                                            val (_, _, target) = WorkoutSettingsRepository.getTargetTime(
                                                context,
                                                type
                                            )
                                            val totalSets = recordsOfType.sumOf { it.sets ?: 0 }
                                            Pair(target, totalSets)
                                        }
                                    }

                                    val goalReached = achievedSets >= targetSets

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween // Gleichmäßige Verteilung
                                    ) {
                                        Icon(
                                            modifier = Modifier
                                                .padding(end = 8.dp),
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star",
                                            tint = if (goalReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                                        )
                                        Text(
                                            text = when (type) {
                                                WorkoutType.PUSH_UP -> "Push‑Ups"
                                                WorkoutType.SQUAT -> "Kniebeugen"
                                                WorkoutType.LUNGE -> "Ausfallschritte"
                                                WorkoutType.ROWING -> "Rudern"
                                                WorkoutType.CRUNCHES -> "Crunches"
                                                WorkoutType.SHOULDER_PRESS -> "Schulterpresse"
                                                WorkoutType.BURPEES -> "Burpees"
                                                WorkoutType.LEG_RAISES -> "Beinheben"
                                                WorkoutType.TRIZEPS_DIPS -> "Trizeps-Dips"
                                                WorkoutType.PLANK -> "Planks"
                                                WorkoutType.MOUNTAIN_CLIMBER -> "Mountain-Climber"
                                            },
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Text(
                                            text = if (goalReached) "Ziel erreicht" else "Ziel nicht erreicht",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (goalReached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.End // Rechts ausrichten für bessere Lesbarkeit
                                        )

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "©2025 Sebastian Grauthoff - App Version 1.0",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun TotalWorkoutOverviewScreen(context: Context) {
    val allRecords = WorkoutHistoryRepository.loadHistory(context)
    val recordsByType = allRecords.groupBy { it.type }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(modifier = Modifier.weight(1f),verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            // Festes Logo oben
            Image(
                painter = painterResource(id = R.drawable.stats_cropped),
                contentDescription = "App Logo",
                modifier = Modifier.size(90.dp)
            )
            Text(
                text = "Statistik",
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                recordsByType.forEach { (type, records) ->
                    item {
                        Card(
                            modifier = Modifier
                                .shadow(10.dp, shape = RoundedCornerShape(8.dp))
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val totalValue = when (type) {
                                    // Wiederholungsbasierte Workouts: Gesamtzahl aller Wiederholungen berechnen
                                    WorkoutType.PUSH_UP,
                                    WorkoutType.SQUAT,
                                    WorkoutType.LUNGE,
                                    WorkoutType.ROWING,
                                    WorkoutType.CRUNCHES,
                                    WorkoutType.SHOULDER_PRESS,
                                    WorkoutType.BURPEES,
                                    WorkoutType.LEG_RAISES,
                                    WorkoutType.TRIZEPS_DIPS -> {
                                        records.sumOf { it.count ?: 0 }.toString()
                                    }

                                    // Zeitbasierte Workouts: Gesamtzeit in hh:mm:ss umwandeln
                                    WorkoutType.PLANK,
                                    WorkoutType.MOUNTAIN_CLIMBER -> {
                                        val totalMillis = records.sumOf { (it.durationMillis?.toLong() ?: 0L) * (it.sets ?: 1) }
                                        formatTime(totalMillis)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = getWorkoutName(type),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = totalValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "©2025 Sebastian Grauthoff - App Version 1.0",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
fun formatTime(milliseconds: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun getWorkoutName(type: WorkoutType): String {
    return when (type) {
        WorkoutType.PUSH_UP -> "Push‑Ups"
        WorkoutType.SQUAT -> "Kniebeugen"
        WorkoutType.LUNGE -> "Ausfallschritte"
        WorkoutType.ROWING -> "Rudern"
        WorkoutType.CRUNCHES -> "Crunches"
        WorkoutType.SHOULDER_PRESS -> "Schulterpresse"
        WorkoutType.BURPEES -> "Burpees"
        WorkoutType.LEG_RAISES -> "Beinheben"
        WorkoutType.TRIZEPS_DIPS -> "Trizeps-Dips"
        WorkoutType.PLANK -> "Planks"
        WorkoutType.MOUNTAIN_CLIMBER -> "Mountain-Climber"
    }
}


fun exportWorkoutHistory(context: Context) {
    val history = WorkoutHistoryRepository.loadHistory(context)
    val json = Gson().toJson(history)

    val fileName = "workout_history_${System.currentTimeMillis()}.json"
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsDir, fileName)

    try {
        file.writeText(json)
        Toast.makeText(context, "Export erfolgreich!\nGespeichert unter: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun importWorkoutHistory(context: Context, json: String) {
    try {
        val type = object : TypeToken<List<WorkoutRecord>>() {}.type
        val importedHistory: List<WorkoutRecord> = Gson().fromJson(json, type)

        WorkoutHistoryRepository.saveHistory(context, importedHistory)
        Toast.makeText(context, "Import erfolgreich! ${importedHistory.size} Einträge geladen.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Import fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DataTransferScreen(navController: NavController) {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader().use { reader ->
                    val json = reader?.readText() ?: ""
                    importWorkoutHistory(context, json)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fehler beim Import: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { exportWorkoutHistory(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Workout-History exportieren")
        }

        Button(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Workout-History importieren")
        }
    }
}