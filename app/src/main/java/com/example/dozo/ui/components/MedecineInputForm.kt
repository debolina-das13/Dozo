package com.example.dozo.ui.components

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dozo.data.Dose
import com.example.dozo.data.Medicine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicineInputForm(
    onAddMedicine: (Medicine) -> Unit,
    onFormSubmitted: () -> Unit
) {
    // --- State for all our inputs ---
    var medicineName by remember { mutableStateOf("") }
    var doseQuantity by remember { mutableIntStateOf(1) }
    var doseType by remember { mutableStateOf("Tablet") }
    val selectedTimes = remember { mutableStateListOf<LocalTime>() }
    // --- FIX: Use a List for the days state ---
    var selectedDays by remember { mutableStateOf<List<DayOfWeek>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var isOneTimeReminder by remember { mutableStateOf(false) }

    // --- State to drive animations ---
    val isDetailsComplete = medicineName.isNotBlank() && doseQuantity > 0 && doseType.isNotBlank()
    val isScheduleComplete = isOneTimeReminder || selectedDays.isNotEmpty()
    val isFormComplete = isDetailsComplete && isScheduleComplete && selectedTimes.isNotEmpty()

    val detailsColor by animateColorAsState(
        targetValue = if (isDetailsComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(500), label = "details_color_animation"
    )
    val scheduleColor by animateColorAsState(
        targetValue = if (isScheduleComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(500), label = "schedule_color_animation"
    )

    // --- Time Picker Dialog Logic ---
    if (showTimePicker) {
        val now = LocalTime.now()
        TimePickerDialog(
            context,
            { _, hour, minute -> selectedTimes.add(LocalTime.of(hour, minute)) },
            now.hour, now.minute, false
        ).apply {
            setOnDismissListener { showTimePicker = false }
            show()
        }
        DisposableEffect(Unit) { onDispose { showTimePicker = false } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Add New Reminder",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- Section 1: Medicine Details ---
        Text(
            "1. Medicine Details",
            style = MaterialTheme.typography.titleLarge,
            color = detailsColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = medicineName,
            onValueChange = { medicineName = it },
            label = { Text("Medicine Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        DoseTypeDropdown(onTypeSelected = { doseType = it })
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { if (doseQuantity > 1) doseQuantity-- },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease dose quantity")
            }
            Text(
                text = "$doseQuantity",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            OutlinedButton(
                onClick = { doseQuantity++ },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase dose quantity",tint = Color.Green)
            }
        }
        Text(
            text = "Quantity",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        // --- Section 2: Schedule ---
        AnimatedVisibility(
            visible = isDetailsComplete,
            enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500))
        ) {
            Column {
                Text(
                    "2. Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheduleColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isOneTimeReminder = !isOneTimeReminder }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "One-Time Reminder (Today)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isOneTimeReminder,
                        onCheckedChange = { isOneTimeReminder = it })
                }

                AnimatedVisibility(visible = !isOneTimeReminder) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        DayOfWeekSelector(
                            selectedDays = selectedDays.toSet(), // Pass as a Set for the UI
                            onDaySelected = { day ->
                                // --- FIX: Use List add/remove logic ---
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            }
                        )
                    }
                }
            }
        } // <-- This brace was missing in snippet 2

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        // --- Section 3: Dose Times ---
        AnimatedVisibility(
            visible = isScheduleComplete,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + expandVertically(
                animationSpec = tween(500, 200)
            )
        ) {
            Column {
                Text("3. Dose Times", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedTimes.sorted().forEach { time ->
                        InputChip(
                            selected = false,
                            onClick = { /* No action */ },
                            label = { Text(time.format(DateTimeFormatter.ofPattern("hh:mm a"))) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { selectedTimes.remove(time) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove time"
                                    )
                                }
                            }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Time")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Dose Time")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Animated Submit Button ---
        val buttonElevation by animateDpAsState(
            targetValue = if (isFormComplete) 8.dp else 0.dp,
            label = "button_elevation_animation"
        )
        Button(
            onClick = {
                if (isFormComplete) {
                    val today = LocalDate.now()
                    val finalDaysOfWeek =
                        if (isOneTimeReminder) listOf(today.dayOfWeek) else selectedDays
                    val finalEndDate = if (isOneTimeReminder) today else null

                    val newMedicine = Medicine(
                        id = UUID.randomUUID().toString(),
                        name = medicineName,
                        dosage = "$doseQuantity $doseType",
                        // --- FIX: Convert LocalTime to String ---
                        doses = selectedTimes.map { Dose(time = it.toString()) },
                        // --- FIX: Pass the final List<DayOfWeek> ---
                        daysOfWeek = finalDaysOfWeek,
                        // --- FIX: Convert LocalDate to String ---
                        startDate = today.toString(),
                        endDate = finalEndDate?.toString()
                    )
                    onAddMedicine(newMedicine)
                    onFormSubmitted()
                }
            },
            enabled = isFormComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(elevation = buttonElevation, shape = RoundedCornerShape(50.dp))
        ) {
            Text("Save Reminder", style = MaterialTheme.typography.titleMedium)
        }
    }
}