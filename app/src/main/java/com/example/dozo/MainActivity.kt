package com.example.dozo

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dozo.data.MedicineRepository
import com.example.dozo.data.ReminderScheduler
import com.example.dozo.ui.components.DeleteConfirmationDialog
import com.example.dozo.ui.screens.MainScreen
import com.example.dozo.ui.screens.SettingsScreen // <-- IMPORT YOUR NEW SCREEN
import com.example.dozo.ui.theme.DozoTheme
import com.example.dozo.viewmodel.HomeUiState
import com.example.dozo.viewmodel.MedicineViewModel
import com.example.dozo.viewmodel.MedicineViewModelFactory
import com.example.dozo.viewmodel.ReminderInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 1. Initialize Data Layer
    private val repository by lazy { MedicineRepository() }
    private val scheduler by lazy { ReminderScheduler(applicationContext) }

    // 2. Initialize ViewModel with Factory
    private val viewModel: MedicineViewModel by viewModels {
        MedicineViewModelFactory(repository, scheduler)
    }

    // 3. Permission Handling State
    private var showExactAlarmDialog by mutableStateOf(false)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                checkAndRequestExactAlarmPermission()
            } else {
                Log.w("MainActivity", "Notification permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup System Services
        checkAndRequestPermissions()
        signInAnonymously()
        createNotificationChannel()

        setContent {
            DozoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()

                    // Call the Navigation wrapper
                    AppNavigation(
                        viewModel = viewModel,
                        uiState = uiState
                    )

                    // Global Permission Dialog
                    if (showExactAlarmDialog) {
                        PermissionDialog(
                            onDismiss = { showExactAlarmDialog = false },
                            onGoToSettings = {
                                showExactAlarmDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when returning from Settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog = false
            }
        }
    }

    // --- Helper Functions ---

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    checkAndRequestExactAlarmPermission()
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            checkAndRequestExactAlarmPermission()
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog = true
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Medicine Reminders"
        val descriptionText = "High priority channel for medicine reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("medicine_channel", name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun signInAnonymously() {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously()
                .addOnFailureListener { e -> Log.e("Auth", "Sign in failed", e) }
        }
    }
}

// --- THIS COMPOSABLE NOW CONTROLS ALL NAVIGATION ---
@Composable
fun AppNavigation(
    viewModel: MedicineViewModel,
    uiState: HomeUiState
) {
    val navController = rememberNavController()
    var instanceToDelete by remember { mutableStateOf<ReminderInstance?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Shared Delete Dialog
    if (instanceToDelete != null) {
        DeleteConfirmationDialog(
            onConfirmDeleteInstance = {
                val instanceToUndo = instanceToDelete
                instanceToDelete?.let { viewModel.deleteReminderInstance(it.instanceId) }
                instanceToDelete = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Reminder deleted for this day.",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        instanceToUndo?.let { viewModel.undoDeleteInstance(it.instanceId) }
                    }
                }
            },
            onConfirmDeleteRule = {
                instanceToDelete?.medicine?.let { viewModel.deleteMedicineRule(it) }
                instanceToDelete = null
                scope.launch {
                    snackbarHostState.showSnackbar("Entire schedule deleted.")
                }
            },
            onDismiss = { instanceToDelete = null }
        )
    }

    // --- THIS IS YOUR NAVHOST WITH SLIDE ANIMATIONS ---
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(400)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(400)) }
    ) {
        composable("dashboard") {
            MainScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                instanceToDelete = instanceToDelete,
                onAddMedicine = viewModel::addMedicine,
                onDeleteClicked = { instance -> instanceToDelete = instance },
                onDeleteMedicine = viewModel::deleteMedicineRule,
                onDateSelected = viewModel::onDateSelected,
                onDoseStatusChanged = { instanceId, doseId, status ->
                    viewModel.onDoseStatusChanged(instanceId, doseId, status)
                },
                onDeleteInstance = viewModel::deleteReminderInstance,
                onUndoDeleteInstance = viewModel::undoDeleteInstance,
                onNavigateToSettings = {
                    navController.navigate("settings") // This makes the button work
                }
            )
        }

        // --- ADDED THE NEW SETTINGS SCREEN ROUTE ---
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack() // This makes the back button work
                }
            )
        }
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = { Text("To ensure your reminders ring at the exact time, please allow the 'Alarms & Reminders' permission.") },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("Go to Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}