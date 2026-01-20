package com.example.dozo.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class DoseStatus { PENDING, TAKEN, SKIPPED }

data class Dose(
    @JvmField
    val id: String = UUID.randomUUID().toString(),
    @JvmField
    val time: String = LocalTime.NOON.toString() // <-- FIX: Must be String
) {
    constructor() : this(UUID.randomUUID().toString(), LocalTime.NOON.toString())
}

data class Medicine(
    @JvmField
    val id: String = UUID.randomUUID().toString(),
    @JvmField
    val name: String = "",
    @JvmField
    val dosage: String = "",
    @JvmField
    val doses: List<Dose> = emptyList(), // This will be a List<Dose>
    @JvmField
    val daysOfWeek: List<DayOfWeek> = emptyList(),
    @JvmField
    val startDate: String = LocalDate.MIN.toString(), // <-- FIX: Must be String
    @JvmField
    val endDate: String? = null // Nullable String is fine
) {
    constructor() : this(UUID.randomUUID().toString(), "", "", emptyList(), emptyList(), LocalDate.MIN.toString(), null)
}