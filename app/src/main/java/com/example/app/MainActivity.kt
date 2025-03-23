package com.example.app
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.Image
import android.os.CountDownTimer
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        vibratePhone(context, 100)
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
        vibratePhone(context, 100)
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
            sets = 1,
            goalSets = goalSets
        )
        WorkoutHistoryRepository.addOrUpdateRecord(context, record)
        history = WorkoutHistoryRepository.loadHistory(context)
        vibratePhone(context, 100)
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
        composable("achievements") { AchievementsScreen(context) }
        composable("dataTransfer") { DataTransferScreen() }
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
                    "Gesamtstatistik" to "stats",
                    "Errungenschaften" to "achievements"
                )
                items(exercises) { (title, route) ->
                    ExerciseTile(navController = navController, label = title, route = route )
                }
                items(goals) { (title, route) ->
                    GoalTile(navController = navController, label = title, route = route )
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

    val view = LocalView.current

    LaunchedEffect(Unit) {
        view.keepScreenOn = true
    }

    DisposableEffect(Unit) {
        onDispose { view.keepScreenOn = false }
    }

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
    val view = LocalView.current
    var count by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(WorkoutHistoryRepository.loadHistory(context)) }
    var editRecord by remember { mutableStateOf<WorkoutRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<WorkoutRecord?>(null) }
    var reps by remember { mutableStateOf("30") }
    var sets by remember { mutableStateOf("3") }

    val numberMap = mapOf(
        "eins" to 1,
        "zwei" to 2,
        "drei" to 3,
        "vier" to 4,
        "fünf" to 5,
        "sechs" to 6,
        "sieben" to 7,
        "acht" to 8,
        "neun" to 9,
        "zehn" to 10,
        "elf" to 11,
        "zwölf" to 12,
        "dreizehn" to 13,
        "vierzehn" to 14,
        "fünfzehn" to 15,
        "sechzehn" to 16,
        "siebzehn" to 17,
        "achtzehn" to 18,
        "neunzehn" to 19,
        "zwanzig" to 20,
        "einundzwanzig" to 21,
        "zweiundzwanzig" to 22,
        "dreiundzwanzig" to 23,
        "vierundzwanzig" to 24,
        "fünfundzwanzig" to 25,
        "sechsundzwanzig" to 26,
        "siebenundzwanzig" to 27,
        "achtundzwanzig" to 28,
        "neunundzwanzig" to 29,
        "dreißig" to 30,
        "einunddreißig" to 31,
        "zweiunddreißig" to 32,
        "dreiunddreißig" to 33,
        "vierunddreißig" to 34,
        "fünfunddreißig" to 35,
        "sechsunddreißig" to 36,
        "siebenunddreißig" to 37,
        "achtunddreißig" to 38,
        "neununddreißig" to 39,
        "vierzig" to 40,
        "einundvierzig" to 41,
        "zweiundvierzig" to 42,
        "dreiundvierzig" to 43,
        "vierundvierzig" to 44,
        "fünfundvierzig" to 45,
        "sechsundvierzig" to 46,
        "siebenundvierzig" to 47,
        "achtundvierzig" to 48,
        "neunundvierzig" to 49,
        "fünfzig" to 50,
        "einundfünfzig" to 51,
        "zweiundfünfzig" to 52,
        "dreiundfünfzig" to 53,
        "vierundfünfzig" to 54,
        "fünfundfünfzig" to 55,
        "sechsundfünfzig" to 56,
        "siebenundfünfzig" to 57,
        "achtundfünfzig" to 58,
        "neunundfünfzig" to 59,
        "sechzig" to 60,
        "einundsechzig" to 61,
        "zweiundsechzig" to 62,
        "dreiundsechzig" to 63,
        "vierundsechzig" to 64,
        "fünfundsechzig" to 65,
        "sechsundsechzig" to 66,
        "siebenundsechzig" to 67,
        "achtundsechzig" to 68,
        "neunundsechzig" to 69,
        "siebzig" to 70,
        "einundsiebzig" to 71,
        "zweiundsiebzig" to 72,
        "dreiundsiebzig" to 73,
        "vierundsiebzig" to 74,
        "fünfundsiebzig" to 75,
        "sechsundsiebzig" to 76,
        "siebenundsiebzig" to 77,
        "achtundsiebzig" to 78,
        "neunundsiebzig" to 79,
        "achtzig" to 80,
        "einundachtzig" to 81,
        "zweiundachtzig" to 82,
        "dreiundachtzig" to 83,
        "vierundachtzig" to 84,
        "fünfundachtzig" to 85,
        "sechsundachtzig" to 86,
        "siebenundachtzig" to 87,
        "achtundachtzig" to 88,
        "neunundachtzig" to 89,
        "neunzig" to 90,
        "einundneunzig" to 91,
        "zweiundneunzig" to 92,
        "dreiundneunzig" to 93,
        "vierundneunzig" to 94,
        "fünfundneunzig" to 95,
        "sechsundneunzig" to 96,
        "siebenundneunzig" to 97,
        "achtundneunzig" to 98,
        "neunundneunzig" to 99,
        "hundert" to 100
    )

    // SpeechRecognizer und Intent initialisieren
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }
    fun startSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        } else {
            speechRecognizer.startListening(speechIntent)
        }
    }

    // Funktion zum Aktualisieren eines Eintrags in der History
    fun updateRecord(updatedRecord: WorkoutRecord, originalRecord: WorkoutRecord) {
        val currentHistory = WorkoutHistoryRepository.loadHistory(context).toMutableList()
        val index = currentHistory.indexOf(originalRecord)
        if (index != -1) {
            currentHistory[index] = updatedRecord.copy(
                goalReps = updatedRecord.goalReps ?: originalRecord.goalReps,
                goalSets = updatedRecord.goalSets ?: originalRecord.goalSets
            )
            WorkoutHistoryRepository.saveHistory(context, currentHistory)
            history = currentHistory
        }
    }

    // Funktion zum Löschen eines Eintrags
    fun deleteRecord(record: WorkoutRecord) {
        val currentHistory = WorkoutHistoryRepository.loadHistory(context).toMutableList()
        currentHistory.remove(record)
        WorkoutHistoryRepository.saveHistory(context, currentHistory)
        history = currentHistory
    }

    fun addWorkoutRecord(countOverride: Int? = null) {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val currentHistory = WorkoutHistoryRepository.loadHistory(context).toMutableList()

        val existingRecord = currentHistory.find { it.date == date && it.type == workoutType }
        if (existingRecord != null) {
            val updatedRecord = existingRecord.copy(
                count = ((countOverride ?: ((existingRecord.count ?: 0) + 1))),  // Verwende countOverride, wenn vorhanden, andernfalls den bisherigen Wert
                goalReps = reps.toIntOrNull() ?: existingRecord.goalReps,
                goalSets = sets.toIntOrNull() ?: existingRecord.goalSets
            )
            currentHistory[currentHistory.indexOf(existingRecord)] = updatedRecord
        } else {
            val newRecord = WorkoutRecord(
                date = date,
                type = workoutType,
                count = countOverride ?: 1,  // Falls countOverride null ist, setze den Standardwert 1
                goalReps = reps.toIntOrNull() ?: 0,
                goalSets = sets.toIntOrNull() ?: 0
            )
            currentHistory.add(newRecord)
        }

        WorkoutHistoryRepository.saveHistory(context, currentHistory)
        history = currentHistory
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull() ?: ""
            val number = recognizedText.toIntOrNull() ?: numberMap[recognizedText.lowercase(Locale.getDefault())]
            if (number != null) {
                count = number  // Zähler aktualisieren
                addWorkoutRecord(count)
                vibratePhone(context, 100)
            }
            // Nach 500ms erneut starten
            Handler(Looper.getMainLooper()).postDelayed({
                speechRecognizer.startListening(speechIntent)
            }, 250)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull() ?: ""
            val number = recognizedText.toIntOrNull() ?: numberMap[recognizedText.lowercase(Locale.getDefault())]
            if (number != null) {
                count = number

            }
        }

        override fun onError(error: Int) {
            // Bei Fehlern 500ms warten und erneut starten
            Handler(Looper.getMainLooper()).postDelayed({
                speechRecognizer.startListening(speechIntent)
            }, 250)
        }

        // Unbenutzte Methoden
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onRmsChanged(rmsdB: Float) {}
    })

    speechRecognizer.startListening(speechIntent)

    // Beim Start werden die Ziele aus SharedPreferences geladen
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
                val (minutes, seconds) = WorkoutSettingsRepository.getTargetTime(context, WorkoutType.PLANK)
                reps = minutes.toString()
                sets = seconds.toString()
            }
            WorkoutType.MOUNTAIN_CLIMBER -> {
                val (minutes, seconds) = WorkoutSettingsRepository.getTargetTime(context, WorkoutType.MOUNTAIN_CLIMBER)
                reps = minutes.toString()
                sets = seconds.toString()
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        view.keepScreenOn = true
        startSpeechRecognition()
    }
    DisposableEffect(Unit) {
        onDispose { speechRecognizer.stopListening()
                    speechRecognizer.destroy()
                    view.keepScreenOn = false
        }
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Wiederholungen") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            TextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("Sätze") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
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
                    WorkoutType.PLANK -> WorkoutSettingsRepository.saveTargetTime(context, WorkoutType.PLANK, repsInt, setsInt, setsInt)
                    WorkoutType.MOUNTAIN_CLIMBER -> WorkoutSettingsRepository.saveTargetTime(context, WorkoutType.MOUNTAIN_CLIMBER, repsInt, setsInt, setsInt)
                }
                showDialog = true
            },
            modifier = Modifier
                .shadow(10.dp, shape = RoundedCornerShape(8.dp))
                .fillMaxWidth(),
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    count++
                    addWorkoutRecord()
                },
                modifier = Modifier.fillMaxWidth(),
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    count = 0
                    vibratePhone(context, 100)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(text = "Zurücksetzen", color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
    if (showDialog) {
        vibratePhone(context, 100)
        WorkoutAlert(
            title = "Speichern erfolgreich",
            message = "Das tägliche Ziel wurde erfolgreich gespeichert",
            onDismiss = { showDialog = false }
        )
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
    val allRecords = WorkoutHistoryRepository.loadHistory(context)
    val recordsByDate = allRecords.groupBy { it.date }.toSortedMap(Comparator.reverseOrder())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
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
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(text = date, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                recordsForDate.groupBy { it.type }.forEach { (type, recordsOfType) ->
                                    val (targetSets, achievedSets, goalReached) = when (type) {
                                        WorkoutType.PLANK, WorkoutType.MOUNTAIN_CLIMBER -> {
                                            // Bei Mountain Climbers und Planks wird nur goalSets überprüft
                                            val totalSets = recordsOfType.sumOf { it.sets ?: 0 }
                                            val goalSets = recordsOfType.firstOrNull()?.goalSets ?: 0
                                            val goalReached = totalSets >= goalSets
                                            Triple(goalSets, totalSets, goalReached)
                                        }

                                        else -> {
                                            // Für andere Übungen wird count überprüft (count >= goalReps * goalSets)
                                            val goalReps = recordsOfType.firstOrNull()?.goalReps ?: 0
                                            val goalSets = recordsOfType.firstOrNull()?.goalSets ?: 0
                                            val totalCount = recordsOfType.sumOf { it.count ?: 0 }
                                            val goalReached = totalCount >= (goalReps * goalSets)
                                            Triple(goalReps * goalSets, totalCount, goalReached)
                                        }
                                    }

                                    val progress = if (targetSets > 0) achievedSets.toFloat() / targetSets.toFloat() else 1f

                                    var animatedProgress by remember { mutableFloatStateOf(0f) }
                                    LaunchedEffect(progress) {
                                        animatedProgress = progress
                                    }
                                    val animatedProgressState by animateFloatAsState(
                                        targetValue = animatedProgress,
                                        animationSpec = tween(durationMillis = 1000)
                                    )

                                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = getWorkoutName(type),
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                text = if (goalReached) "Ziel erreicht" else "Ziel nicht erreicht",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (goalReached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Star",
                                                tint = if (goalReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { animatedProgressState },
                                            modifier = Modifier
                                                .padding(vertical = 8.dp)
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.background,
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
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp).fillMaxWidth()
                .align(Alignment.CenterHorizontally),
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.stats_cropped),
                contentDescription = "App Logo",
                modifier = Modifier.size(90.dp)
            )
            Text(
                text = "Gesamtstatistik",
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
                                // Berechnung der Gesamtzahl für Zeitbasierte und Wiederholungs-Übungen
                                val totalValue = when (type) {
                                    WorkoutType.PUSH_UP,
                                    WorkoutType.SQUAT,
                                    WorkoutType.LUNGE,
                                    WorkoutType.ROWING,
                                    WorkoutType.CRUNCHES,
                                    WorkoutType.SHOULDER_PRESS,
                                    WorkoutType.BURPEES,
                                    WorkoutType.LEG_RAISES,
                                    WorkoutType.TRIZEPS_DIPS -> {
                                        records.sumOf { it.count ?: 0 }
                                    }

                                    WorkoutType.PLANK,
                                    WorkoutType.MOUNTAIN_CLIMBER -> {
                                        records.sumOf { (it.durationMillis?.toLong() ?: 0L) * (it.sets ?: 1) }
                                    }
                                }

                                // Initialisierungswert für Animation
                                var animatedTotalValue by remember { mutableFloatStateOf(0f) }

                                // Animation starten
                                LaunchedEffect(totalValue) {
                                    animatedTotalValue = totalValue.toFloat()
                                }

                                val animatedProgress by animateFloatAsState(
                                    targetValue = animatedTotalValue,
                                    animationSpec = tween(durationMillis = 1000)
                                )

                                val displayValue = when (type) {
                                    WorkoutType.PLANK, WorkoutType.MOUNTAIN_CLIMBER -> {
                                        formatTime(animatedProgress.toLong())
                                    }
                                    else -> {
                                        animatedProgress.toInt().toString()
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val imageId = when (type) {
                                        WorkoutType.SQUAT -> R.drawable.squats
                                        WorkoutType.BURPEES -> R.drawable.burpees
                                        WorkoutType.LUNGE -> R.drawable.lunges
                                        WorkoutType.MOUNTAIN_CLIMBER -> R.drawable.mountainclimber
                                        WorkoutType.PUSH_UP -> R.drawable.pushups
                                        WorkoutType.PLANK -> R.drawable.planks
                                        WorkoutType.SHOULDER_PRESS -> R.drawable.shoulderpress
                                        WorkoutType.LEG_RAISES -> R.drawable.legraises
                                        WorkoutType.ROWING -> R.drawable.rowing
                                        WorkoutType.TRIZEPS_DIPS -> R.drawable.trizepsdips
                                        WorkoutType.CRUNCHES -> R.drawable.crunches
                                    }

                                    Image(
                                        painter = painterResource(id = imageId),
                                        contentDescription = "Workout Icon",
                                        modifier = Modifier.size(90.dp)
                                    )

                                    Text(
                                        text = displayValue,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                        text = when (type) {
                                            WorkoutType.PLANK -> "Gesamtzeit"
                                            WorkoutType.MOUNTAIN_CLIMBER -> "Gesamtzeit"
                                            else -> "Wiederholungen"
                                        },
                                        style = MaterialTheme.typography.bodyLarge
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
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp).fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AchievementsScreen(context: Context) {
    val allRecords = WorkoutHistoryRepository.loadHistory(context)
    val recordsByType = allRecords.groupBy { it.type }
    val goals = getWorkoutGoals()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.achievments_cropped),
                contentDescription = "Achievements Icon",
                modifier = Modifier.size(90.dp)
            )
            Text(
                text = "Errungenschaften",
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                goals.forEach { (type, targets) ->
                    item {
                        Card(
                            modifier = Modifier
                                .shadow(10.dp, shape = RoundedCornerShape(8.dp))
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Berechnung der Gesamtzeit für Zeitbasierte Übungen
                                val totalValue = recordsByType[type]?.sumOf { record ->
                                    when (type) {
                                        WorkoutType.PLANK, WorkoutType.MOUNTAIN_CLIMBER -> (record.durationMillis?.toLong()
                                            ?: 0) * (record.sets ?: 1) // In Millisekunden umrechnen
                                        else -> (record.count
                                            ?: 0).toLong() // Normale Wiederholungen bleiben unverändert
                                    }
                                } ?: 0L

                                Text(
                                    text = getWorkoutName(type),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                targets.forEach { goal ->
                                    val goalMillis = if (type in listOf(
                                            WorkoutType.PLANK,
                                            WorkoutType.MOUNTAIN_CLIMBER
                                        )
                                    ) goal * 1000L else goal.toLong()
                                    val targetProgress =
                                        (totalValue.toFloat() / goalMillis.toFloat()).coerceIn(
                                            0f,
                                            1f
                                        )
                                    var animatedProgress by remember { mutableFloatStateOf(0f) }

                                    LaunchedEffect(targetProgress) {
                                        animatedProgress = targetProgress
                                    }

                                    val progress by animateFloatAsState(
                                        targetValue = animatedProgress,
                                        animationSpec = tween(
                                            durationMillis = 1000,
                                            easing = FastOutSlowInEasing
                                        ),
                                        label = "progressAnimation"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val goalText = if (type in listOf(
                                                WorkoutType.PLANK,
                                                WorkoutType.MOUNTAIN_CLIMBER
                                            )
                                        ) {
                                            "Schaffe ${formatTime(goalMillis)} Gesamtzeit"
                                        } else {
                                            "Schaffe $goal Wiederholungen"
                                        }

                                        Text(
                                            text = goalText,
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Achievement Star",
                                            tint = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.background,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp).fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun getWorkoutGoals(): Map<WorkoutType, List<Int>> {
    return mapOf(
        WorkoutType.PUSH_UP to listOf(10, 100, 500, 1000, 2000, 5000, 10000, 15000, 25000, 50000),
        WorkoutType.PLANK to listOf(60, 180, 600, 1200, 2400, 3600, 7200, 10800, 18000, 28800),
        WorkoutType.MOUNTAIN_CLIMBER to listOf(60, 180, 300, 600, 1200, 2400, 3600, 5400, 7200, 10800),
        WorkoutType.SQUAT to listOf(30, 200, 500, 1000, 2000, 5000, 10000, 15000, 25000, 50000),
        WorkoutType.LUNGE to listOf(30, 200, 500, 1000, 2000, 4000, 7000, 12000, 20000, 30000),
        WorkoutType.ROWING to listOf(30, 200, 500, 1000, 2000, 4000, 6000, 10000, 20000, 30000),
        WorkoutType.CRUNCHES to listOf(50, 200, 500, 1000, 2500, 5000, 10000, 20000, 30000, 50000),
        WorkoutType.SHOULDER_PRESS to listOf(50, 100, 250, 500, 1000, 1500, 3000, 5000, 7000, 10000),
        WorkoutType.BURPEES to listOf(10, 100, 500, 1000, 2500, 5000, 10000, 20000, 35000, 50000),
        WorkoutType.LEG_RAISES to listOf(20, 100, 250, 500, 1000, 2000, 3000, 5000, 8000, 12000),
        WorkoutType.TRIZEPS_DIPS to listOf(20, 100, 250, 500, 1000, 1500, 2500, 4000, 6000, 10000)
    )
}


@SuppressLint("DefaultLocale")
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
fun DataTransferScreen() {
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