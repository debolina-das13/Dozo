package com.example.dozo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dozo.data.DoseStatus
import com.example.dozo.ui.components.FlippableCardContainer
import com.example.dozo.ui.components.HorizontalCalendar
import com.example.dozo.ui.components.MedicineListItem
import com.example.dozo.ui.components.ThreeDTiltCard
import com.example.dozo.viewmodel.HomeUiState
import com.example.dozo.viewmodel.ReminderInstance
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    uiState: HomeUiState,
    onDeleteClicked: (ReminderInstance) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDoseStatusChanged: (String, String, DoseStatus) -> Unit,
    instanceToDelete: ReminderInstance?
) {
    val today = LocalDate.now()

    val remindersForSelectedDay = remember(uiState.remindersByDate, uiState.selectedDate) {
        uiState.remindersByDate[uiState.selectedDate] ?: emptyList()
    }
    val hasRemindersForSelectedDay = remindersForSelectedDay.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- 3D FLIPPABLE PROGRESS CARD ---
        item {
            FlippableCardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp),
                front = { ProgressCardFront(uiState) },
                back = { ProgressCardBack(uiState) }
            )
        }

        // --- Horizontal Calendar ---
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                HorizontalCalendar(
                    selectedDate = uiState.selectedDate,
                    onDateSelected = onDateSelected
                )
            }
        }

        // --- "No Reminders" Message ---
        if (!hasRemindersForSelectedDay) {
            item {
                Text(
                    text = "No reminders for ${if (uiState.selectedDate == today) "today" else uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMMM d"))}.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // --- Sort Logic ---
        val (nextDoseInstance, otherDoseInstances) = sortInstancesByNextDose(
            remindersForSelectedDay,
            uiState.selectedDate == today
        )

        // --- 1. NEXT DOSE (High Priority 3D Card) ---
        if (nextDoseInstance != null) {
            item(key = "next-dose-${nextDoseInstance.instanceId}") {
                // --- RECODED: Removed AnimatedVisibility ---
                Column(
                    modifier = Modifier.animateItemPlacement() // Keep this!
                ) {
                    Text(
                        "Up Next",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    ThreeDTiltCard(
                        onClick = { /* Optional: Navigate to details */ },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        ) {
                            MedicineListItem(
                                instance = nextDoseInstance,
                                onDeleteClicked = onDeleteClicked,
                                onDoseStatusChanged = onDoseStatusChanged,
                                instanceToDelete = instanceToDelete
                            )
                        }
                    }
                }
            }
        }

        // --- 2. OTHER DOSES ---
        if (otherDoseInstances.isNotEmpty()) {
            item {
                // --- RECODED: Removed AnimatedVisibility ---
                Text(
                    "Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .animateItemPlacement() // Keep this!
                )
            }
        }

        items(
            items = otherDoseInstances,
            key = { it.instanceId }
        ) { instance ->
            // --- RECODED: Removed AnimatedVisibility ---
            ThreeDTiltCard(
                onClick = { /* Navigate */ },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .animateItemPlacement() // Keep this!
            ) {
                MedicineListItem(
                    instance = instance,
                    onDeleteClicked = onDeleteClicked,
                    onDoseStatusChanged = onDoseStatusChanged,
                    instanceToDelete = instanceToDelete
                )
            }
        }
    }
}

// --- Sub-Composables for the Flip Card ---
@Composable
fun ProgressCardFront(uiState: HomeUiState) {
    val progress = if (uiState.todaysDoseCount > 0) {
        uiState.takenDoseCountToday.toFloat() / uiState.todaysDoseCount.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily Goal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${uiState.takenDoseCountToday} of ${uiState.todaysDoseCount} doses completed",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("(Tap to see details)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    strokeWidth = 8.dp,
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProgressCardBack(uiState: HomeUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Keep it up!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Consistency is key to your health.",
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper function (Unchanged)
private fun sortInstancesByNextDose(
    instances: List<ReminderInstance>,
    isToday: Boolean
): Pair<ReminderInstance?, List<ReminderInstance>> {
    if (!isToday) return Pair(null, instances)
    val currentTime = LocalTime.now()
    val nextInstance = instances.minByOrNull {
        it.getNextUpcomingDoseTime(currentTime) ?: LocalTime.MAX
    }
    if (nextInstance != null && !nextInstance.allDosesTaken) {
        val otherInstances = instances.filterNot { it.instanceId == nextInstance.instanceId }
        return Pair(nextInstance, otherInstances)
    }
    return Pair(null, instances)
}