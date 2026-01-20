package com.example.dozo.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DoseHistory(
    val instanceId: String = "",
    val doseId: String = "",
    val status: DoseStatus = DoseStatus.PENDING,
    @ServerTimestamp
    val lastUpdated: Date? = null
)