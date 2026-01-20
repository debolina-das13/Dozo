package com.example.dozo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dozo.data.Dose
import com.example.dozo.data.DoseStatus
import com.example.dozo.data.Medicine
import com.example.dozo.viewmodel.ReminderInstance
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListItem(
    instance: ReminderInstance,
    onDeleteClicked: (ReminderInstance) -> Unit,
    onDoseStatusChanged: (instanceId: String, doseId: String, newStatus: DoseStatus) -> Unit,
    instanceToDelete: ReminderInstance? // State from MainScreen to fix swipe bug
) {
    val medicine = instance.medicine
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Needed to reset swipe

    // Calculate colors directly in the composable scope
    val cardBackgroundColor = when {
        instance.allDosesTaken -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        instance.someDosesTaken -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentAlpha by animateFloatAsState(
        targetValue = if (instance.allDosesTaken) 0.6f else 1f, label = "contentAlpha"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDeleteClicked(instance) // Pass the full instance to show dialog
            }
            return@rememberSwipeToDismissBoxState false // Always return false to prevent auto-dismiss
        }
    )

    // FIX: This effect watches the dialog state. When the dialog
    // is dismissed (instanceToDelete becomes null), it resets the swipe.
    LaunchedEffect(instanceToDelete) {
        if (instanceToDelete == null) {
            scope.launch {
                dismissState.reset()
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Calculate color directly in the composable scope
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp)
                    .alpha(contentAlpha)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medicine.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textDecoration = if (instance.allDosesTaken) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = medicine.dosage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val nextDoseTime = instance.getNextUpcomingDoseTime()
                if (nextDoseTime != null) {
                    Text(
                        text = buildAnnotatedString {
                            append("Next: ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append(nextDoseTime.format(DateTimeFormatter.ofPattern("hh:mm a")))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (instance.allDosesTaken) {
                    Text(
                        text = "All doses taken for today!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = tween(durationMillis = 200)),
                    exit = shrinkVertically(animationSpec = tween(durationMillis = 200))
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        medicine.doses.forEach { dose ->
                            // Get the status for this specific instance
                            val doseStatus = instance.doseStatuses[dose.id] ?: DoseStatus.PENDING

                            DoseActionRow(
                                dose = dose,
                                status = doseStatus,
                                onStatusChange = { newStatus ->
                                    // Pass the unique instanceId to fix the cascade bug
                                    onDoseStatusChanged(instance.instanceId, dose.id, newStatus)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Days: ${formatDaysOfWeek(medicine.daysOfWeek as List<DayOfWeek>)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseActionRow(dose: Dose, status: DoseStatus, onStatusChange: (DoseStatus) -> Unit) {
    val isPending = status == DoseStatus.PENDING
    val contentAlpha by animateFloatAsState(targetValue = if (isPending) 1f else 0.5f, label = "doseAlpha")
    val textDecoration = if (isPending) TextDecoration.None else TextDecoration.LineThrough

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            // --- FIX: Parse the String to LocalTime before formatting ---
            text = LocalTime.parse(dose.time).format(DateTimeFormatter.ofPattern("hh:mm a")),
            modifier = Modifier.weight(1f),
            textDecoration = textDecoration
        )

        if (!isPending) {
            Text(
                text = if (status == DoseStatus.TAKEN) "Taken" else "Skipped",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (status == DoseStatus.TAKEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        } else {
            Row {
                TextButton(onClick = { onStatusChange(DoseStatus.SKIPPED) }) {
                    Text("Skip")
                }
                Button(onClick = { onStatusChange(DoseStatus.TAKEN) }) {
                    Text("Take")
                }
            }
        }
    }
}

// Helper function for formatting days of week
@Composable
private fun formatDaysOfWeek(days: List<DayOfWeek>): String {
    if (days.size == 7) return "Every day"
    if (days.size == 5 && !days.contains(DayOfWeek.SATURDAY) && !days.contains(DayOfWeek.SUNDAY)) return "Weekdays"
    if (days.size == 2 && days.contains(DayOfWeek.SATURDAY) && days.contains(DayOfWeek.SUNDAY)) return "Weekends"

    // Convert the List to a Set first for efficient sorting
    val sortedDays = days.toSortedSet()

    return sortedDays.joinToString(", ") {
        it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}