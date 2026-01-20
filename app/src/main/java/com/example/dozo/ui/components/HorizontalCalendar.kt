@file:OptIn(ExperimentalFoundationApi::class)

package com.example.dozo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun HorizontalCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    // 1. Generate dates (e.g., 2 weeks back, 2 weeks forward)
    val dates = remember {
        val today = LocalDate.now()
        (-14..30).map { today.plusDays(it.toLong()) }
    }

    // 2. Auto-scroll to today
    val startIndex = dates.indexOfFirst { it == selectedDate }.takeIf { it >= 0 } ?: 14
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    // 3. Calculate Screen Center for 3D effect
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val centerOffset = screenWidthPx / 2f

    // 4. Snap Behavior (So it stops exactly on a date)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Clean background
            .padding(bottom = 16.dp)
    ) {
        // Month Header
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )

        LazyRow(
            state = listState,
            flingBehavior = snapBehavior,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = screenWidth / 2 - 35.dp), // Center the item
            modifier = Modifier.height(110.dp)
        ) {
            items(dates) { date ->

                // --- 3D MATH CALCULATIONS ---
                val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.key == date }

                // Calculate rotation/scale based on how far the item is from center
                val transformations by remember(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex) {
                    derivedStateOf {
                        val currentItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == dates.indexOf(date) }

                        if (currentItemInfo != null) {
                            val itemCenter = currentItemInfo.offset + (currentItemInfo.size / 2f)
                            val distanceFromCenter = itemCenter - centerOffset

                            // Rotate up to 50 degrees as it moves away
                            val rotationY = (distanceFromCenter / (screenWidthPx / 2f)).coerceIn(-1f, 1f) * 50f

                            // Scale down to 80% size at edges
                            val scale = 1f - (distanceFromCenter.absoluteValue / (screenWidthPx)).coerceIn(0f, 0.2f)

                            // Fade out to 60% opacity at edges
                            val alpha = 1f - (distanceFromCenter.absoluteValue / (screenWidthPx / 1.5f)).coerceIn(0f, 0.4f)

                            Triple(rotationY, scale, alpha)
                        } else {
                            Triple(0f, 1f, 1f)
                        }
                    }
                }

                val (rotationY, scale, alpha) = transformations

                // Render the 3D Date Card
                CalendarDay3DItem(
                    date = date,
                    isSelected = date == selectedDate,
                    rotationY = rotationY,
                    scale = scale,
                    alpha = alpha,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDay3DItem(
    date: LocalDate,
    isSelected: Boolean,
    rotationY: Float,
    scale: Float,
    alpha: Float,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .width(70.dp) // Slightly wider for better look
            .height(90.dp)
            .graphicsLayer {
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                cameraDistance = 12f * density // Adds the 3D perspective
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp), // Soft, modern corners
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 10.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day of Week (e.g., "Mon")
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEE")),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Day of Month (e.g., "15")
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Optional: Small dot if it's today
            if (date == LocalDate.now()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}