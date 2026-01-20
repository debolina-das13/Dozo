package com.example.dozo.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build // Required for version check
import android.util.Log // Required for logging
import com.example.dozo.broadcast.NotificationReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(medicine: Medicine) {
        // 1. Find the next alarm time for this medicine
        val nextAlarm = findNextAlarm(medicine)

        // If an alarm is found, schedule it
        if (nextAlarm != null) {
            val (triggerAtMillis, dose) = nextAlarm

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("MEDICINE_NAME", medicine.name)
                putExtra("MEDICINE_DOSAGE", medicine.dosage)
                putExtra("MEDICINE_ID", medicine.id)
                putExtra("DOSE_ID", dose.id)
            }

            // Create a unique request code for each DOSE
            val requestCode = dose.hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // --- THE FIX: Check permission before scheduling Exact Alarm ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, we must check if we have the permission
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Permission denied: Log warning and fall back to inexact alarm to prevent crash
                    Log.w("ReminderScheduler", "Exact alarm permission missing. Scheduling inexact alarm.")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                // For Android 11 and below, permission is granted automatically
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            // --- END OF FIX ---
        }
    }

    fun cancel(medicine: Medicine) {
        // You must cancel the pending intent for every dose of this medicine
        medicine.doses.forEach { dose ->
            val requestCode = dose.hashCode()
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * This is the complex logic to find the *single next* alarm.
     * It checks today, then tomorrow, and so on.
     */
    private fun findNextAlarm(medicine: Medicine): Pair<Long, Dose>? {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        // 1. Parse all dose times from Strings to LocalTime
        val doseTimes = medicine.doses.mapNotNull {
            try { it to LocalTime.parse(it.time) } catch (e: Exception) { null }
        }.sortedBy { it.second }

        // 2. Check today
        if (isScheduledForDay(medicine, today)) {
            val nextDoseToday = doseTimes.firstOrNull { it.second.isAfter(now.toLocalTime()) }
            if (nextDoseToday != null) {
                val (dose, time) = nextDoseToday
                val triggerAtMillis = today.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                return triggerAtMillis to dose
            }
        }

        // 3. If no time today, check the next 7 days
        for (i in 1..7) {
            val checkDate = today.plusDays(i.toLong())
            if (isScheduledForDay(medicine, checkDate)) {
                // This is a valid day. Get the first dose of that day.
                if (doseTimes.isNotEmpty()) {
                    val (dose, time) = doseTimes.first()
                    val triggerAtMillis = checkDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    return triggerAtMillis to dose
                }
            }
        }

        // 4. No upcoming alarms found
        return null
    }

    /**
     * Helper to check if a medicine is scheduled for a specific date,
     * checking its start date, end date, and days of the week.
     */
    private fun isScheduledForDay(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = try { LocalDate.parse(medicine.startDate) } catch (e: Exception) { LocalDate.MAX }
        val endDate = try { medicine.endDate?.let { LocalDate.parse(it) } } catch (e: Exception) { null }

        val isAfterStartDate = !date.isBefore(startDate)
        val isBeforeEndDate = endDate == null || !date.isAfter(endDate)
        val isCorrectDayOfWeek = medicine.daysOfWeek.contains(date.dayOfWeek)

        return isAfterStartDate && isBeforeEndDate && isCorrectDayOfWeek
    }
}