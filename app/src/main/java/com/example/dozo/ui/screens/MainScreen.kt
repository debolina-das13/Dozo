package com.example.dozo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings // <-- Added Import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.dozo.data.DoseStatus
import com.example.dozo.data.Medicine
import com.example.dozo.ui.components.EmptyStateView
import com.example.dozo.ui.components.MedicineInputForm
import com.example.dozo.viewmodel.HomeUiState
import com.example.dozo.viewmodel.ReminderInstance
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState, // <-- State is passed in
    instanceToDelete: ReminderInstance?, // <-- State is passed in
    onAddMedicine: (Medicine) -> Unit,
    onDeleteClicked: (ReminderInstance) -> Unit, // <-- Event is passed up
    onDeleteMedicine: (Medicine) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDoseStatusChanged: (String, String, DoseStatus) -> Unit,
    onDeleteInstance: (String) -> Unit,
    onUndoDeleteInstance: (String) -> Unit,
    onNavigateToSettings: () -> Unit // <-- Navigation event is passed in
) {
    // --- Local state (not related to navigation) ---
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // --- Dialog logic is LIFTED to AppNavigation, so it is REMOVED from here ---

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                MedicineInputForm(
                    onAddMedicine = onAddMedicine,
                    onFormSubmitted = {
                        scope.launch {
                            drawerState.close()
                            snackbarHostState.showSnackbar("Reminder added.")
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }, // <-- Use the passed-in state
            topBar = {
                TopAppBar(
                    title = { Text("Your Reminders") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    // --- Settings Button is ADDED ---
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { scope.launch { drawerState.open() } },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Medicine")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // --- 3D Page Pull Animation is KEPT ---
                AnimatedContent(
                    targetState = uiState.remindersByDate.isEmpty(),
                    transitionSpec = {
                        if (targetState) {
                            // CASE 1: Becoming Empty (User deleted all items)
                            slideInVertically(
                                animationSpec = tween(600),
                                initialOffsetY = { -it } // Start from top
                            ) + fadeIn() togetherWith fadeOut(animationSpec = tween(400))
                        } else {
                            // CASE 2: Adding Medicine (The "Pull Up" Effect)
                            fadeIn(animationSpec = tween(600, delayMillis = 200)) togetherWith
                                    slideOutVertically(
                                        animationSpec = tween(800),
                                        targetOffsetY = { -it } // Slide to top
                                    ) + fadeOut(animationSpec = tween(600))
                        }
                    },
                    label = "page_transition"
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyStateView(
                            onAddClicked = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    } else {
                        DashboardScreen(
                            uiState = uiState,
                            onDeleteClicked = onDeleteClicked, // Pass down
                            onDateSelected = onDateSelected,
                            onDoseStatusChanged = onDoseStatusChanged,
                            instanceToDelete = instanceToDelete // Pass down
                        )
                    }
                }
            }
        }
    }
}