package com.example.dozo.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickStatsCard(todaysDoseCount: Int) {
    var animatedDoseCount by remember { mutableStateOf(0) }
    LaunchedEffect(todaysDoseCount) {
        animatedDoseCount = todaysDoseCount
    }

    val count by animateIntAsState(
        targetValue = animatedDoseCount,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem("Today's Doses", count.toString())
            // You can add more stats here
            StatItem("Adherence", "95%")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}