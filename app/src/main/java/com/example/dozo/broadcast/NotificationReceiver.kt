package com.example.dozo.broadcast

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.dozo.R // You must have a drawable icon, e.g., R.drawable.ic_notification
import com.example.dozo.data.MedicineRepository
import com.example.dozo.data.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Reminder"
        val medicineDosage = intent.getStringExtra("MEDICINE_DOSAGE") ?: ""
        val medicineId = intent.getStringExtra("MEDICINE_ID")
        val doseId = intent.getStringExtra("DOSE_ID")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "medicine_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // CHANGE THIS to a real icon
            .setContentTitle(medicineName)
            .setContentText("Time to take your dose: $medicineDosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Use the doseId's hashcode as a unique notification ID
        notificationManager.notify(doseId.hashCode(), notification)

        // --- THIS IS THE CRITICAL PART: RE-SCHEDULE THE NEXT ALARM ---
        // We do this in a coroutine so we can call the suspend functions
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (medicineId != null) {
                    val repository = MedicineRepository()
                    val scheduler = ReminderScheduler(context)

                    // 1. Get the full medicine details
                    val medicine = repository.getMedicineById(medicineId)

                    // 2. If the medicine still exists, schedule its next alarm
                    if (medicine != null) {
                        scheduler.schedule(medicine)
                    }
                }
            } finally {
                pendingResult.finish() // Tell the system we are done
            }
        }
    }
}