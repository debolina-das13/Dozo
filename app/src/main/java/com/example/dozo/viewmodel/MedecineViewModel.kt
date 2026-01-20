package com.example.dozo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dozo.data.Dose
import com.example.dozo.data.DoseStatus
import com.example.dozo.data.Medicine
import com.example.dozo.data.MedicineRepository
import com.example.dozo.data.ReminderScheduler // <-- ADD THIS IMPORT
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

// Represents a unique instance of a reminder on a specific date for the UI
data class ReminderInstance(
    val instanceId: String,
    val date: LocalDate,
    val medicine: Medicine, // This holds the 'Medicine' with String dates
    val doseStatuses: Map<String, DoseStatus>
) {
    val takenDoseCount: Int
        get() = doseStatuses.count { it.value == DoseStatus.TAKEN }

    val allDosesTaken: Boolean
        get() = doseStatuses.isNotEmpty() && doseStatuses.all { it.value == DoseStatus.TAKEN }

    val someDosesTaken: Boolean
        get() = doseStatuses.any { it.value == DoseStatus.TAKEN } && !allDosesTaken

    // This helper now parses the time string
    fun getNextUpcomingDoseTime(currentTime: LocalTime = LocalTime.now()): LocalTime? {
        val pendingDoses = medicine.doses.filter {
            (doseStatuses[it.id] ?: DoseStatus.PENDING) == DoseStatus.PENDING
        }
        return pendingDoses
            .map { LocalTime.parse(it.time) } // Parse String to LocalTime
            .filter { it.isAfter(currentTime) }
            .minOrNull()
            ?: pendingDoses.map { LocalTime.parse(it.time) }.minOrNull() // Parse String to LocalTime
    }
}

// Defines the rich UI state that the DashboardScreen needs
data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val takenDoseCountToday: Int = 0,
    val todaysDoseCount: Int = 0,
    val remindersByDate: Map<LocalDate, List<ReminderInstance>> = emptyMap(),
    val isLoading: Boolean = false
)

// --- UPDATE: The ViewModel now takes the Repository AND Scheduler ---
class MedicineViewModel(
    private val repository: MedicineRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    // --- Internal State (Source of Truth from Repository) ---
    private val _medicines: StateFlow<List<Medicine>> = repository.getMedicinesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    private val _doseStatuses: Flow<Map<String, DoseStatus>> = repository.getDoseHistoryFlow()
    private val _deletedInstanceIds: StateFlow<List<String>> = repository.getDeletedInstancesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _viewedMonth = MutableStateFlow(YearMonth.now())

    // --- Public UI State (Combines all internal flows) ---
    val homeUiState: StateFlow<HomeUiState> =
        combine(
            _medicines, _selectedDate, _deletedInstanceIds, _doseStatuses, _viewedMonth
        ) { medicines, selectedDate, deletedIds, doseStatuses, viewedMonth ->

            val remindersByDate = groupRemindersByVisibleMonth(medicines, doseStatuses, viewedMonth)

            val filteredReminders = remindersByDate.mapValues { (_, instances) ->
                instances.filterNot { it.instanceId in deletedIds }
            }.filterValues { it.isNotEmpty() }

            val dosesForSelectedDate = filteredReminders[selectedDate] ?: emptyList()
            val totalDoses = dosesForSelectedDate.sumOf { it.medicine.doses.size }
            val takenDoses = dosesForSelectedDate.sumOf { it.takenDoseCount }

            HomeUiState(
                selectedDate = selectedDate,
                todaysDoseCount = totalDoses,
                takenDoseCountToday = takenDoses,
                remindersByDate = filteredReminders,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    // --- Public Functions (Events from the UI) ---

    fun addMedicine(newMedicine: Medicine) {
        viewModelScope.launch {
            repository.addMedicine(newMedicine)
            scheduler.schedule(newMedicine) // <-- MERGED
        }
    }

    fun deleteMedicineRule(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
            scheduler.cancel(medicine) // <-- MERGED
        }
    }

    fun onDoseStatusChanged(instanceId: String, doseId: String, newStatus: DoseStatus) {
        viewModelScope.launch {
            repository.updateDoseStatus(instanceId, doseId, newStatus)
            // Re-schedule the next alarm for this medicine
            _medicines.value.find { med -> med.doses.any { it.id == doseId } }?.let {
                scheduler.schedule(it) // <-- MERGED
            }
        }
    }

    fun deleteReminderInstance(instanceId: String) {
        viewModelScope.launch {
            repository.addDeletedInstance(instanceId)
        }
    }

    fun undoDeleteInstance(instanceId: String) {
        viewModelScope.launch {
            repository.removeDeletedInstance(instanceId)
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        _viewedMonth.value = YearMonth.from(date)
    }

    fun onMonthChanged(newMonth: YearMonth) {
        _viewedMonth.value = newMonth
        _selectedDate.value = newMonth.atDay(1)
    }

    // --- Private Helper Functions ---
    private fun groupRemindersByVisibleMonth(
        medicines: List<Medicine>,
        doseStatuses: Map<String, DoseStatus>,
        visibleMonth: YearMonth
    ): Map<LocalDate, List<ReminderInstance>> {
        val reminders = mutableMapOf<LocalDate, MutableList<ReminderInstance>>()

        val medicineWithParsedDates = medicines.mapNotNull { med ->
            try {
                val startDate = LocalDate.parse(med.startDate)
                val endDate = med.endDate?.let { LocalDate.parse(it) }
                Triple(med, startDate, endDate)
            } catch (e: Exception) {
                null
            }
        }

        val startPeriod = visibleMonth.atDay(1).minusDays(15)
        val endPeriod = visibleMonth.atEndOfMonth().plusDays(15)

        var currentDate = startPeriod
        while (!currentDate.isAfter(endPeriod)) {
            val remindersForDate = medicineWithParsedDates
                .filter { (med, _, _) -> med.daysOfWeek.contains(currentDate.dayOfWeek) }
                .filter { (_, _, endDate) -> endDate == null || !currentDate.isAfter(endDate) }
                .filter { (_, startDate, _) -> !currentDate.isBefore(startDate) }
                .map { (med, _, _) ->
                    val stableInstanceId = "${med.id}-${currentDate}"
                    val instanceDoseStatuses = med.doses.associate { dose ->
                        val key = "$stableInstanceId-${dose.id}"
                        val status = doseStatuses[key] ?: DoseStatus.PENDING
                        dose.id to status
                    }
                    ReminderInstance(
                        instanceId = stableInstanceId,
                        date = currentDate,
                        medicine = med,
                        doseStatuses = instanceDoseStatuses
                    )
                }
            if (remindersForDate.isNotEmpty()) {
                reminders[currentDate] = remindersForDate.toMutableList()
            }
            currentDate = currentDate.plusDays(1)
        }
        return reminders.toSortedMap()
    }
}