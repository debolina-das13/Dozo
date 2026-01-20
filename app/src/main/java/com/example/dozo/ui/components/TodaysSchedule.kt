package com.example.dozo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dozo.data.Dose
import com.example.dozo.data.DoseStatus
import com.example.dozo.viewmodel.ReminderInstance
import java.time.format.DateTimeFormatter

// A private helper data class to make the list easier to manage
private data class DisplayDose(
    val instanceId: String,
    val medicineName: String,
    val dose: Dose,
    val status: DoseStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaysSchedule(
    todaysSchedule: List<ReminderInstance>,
    onDoseStatusChanged: (instanceId: String, doseId: String, newStatus: DoseStatus) -> Unit
) {
    // This creates a flat, sorted list of all individual doses for the day.
    val allDosesForToday = remember(todaysSchedule) {
        todaysSchedule.flatMap { instance ->
            instance.medicine.doses.map { dose ->
                DisplayDose(
                    instanceId = instance.instanceId,
                    medicineName = instance.medicine.name,
                    dose = dose,
                    status = instance.doseStatuses[dose.id] ?: DoseStatus.PENDING
                )
            }
        }.sortedBy { it.dose.time }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Today's Schedule",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = allDosesForToday,
                key = { "${it.instanceId}-${it.dose.id}" } // Create a stable, unique key
            ) { displayDose ->

                val newStatus = if (displayDose.status == DoseStatus.TAKEN) DoseStatus.PENDING else DoseStatus.TAKEN

                FilterChip(
                    selected = displayDose.status == DoseStatus.TAKEN,
                    onClick = {
                        onDoseStatusChanged(displayDose.instanceId, displayDose.dose.id, newStatus)
                    },
                    label = {
                        Text("${displayDose.medicineName} at ${displayDose.dose.time.format(DateTimeFormatter.ofPattern("hh:mm a"))}")
                    }
                )
            }
        }
    }
}