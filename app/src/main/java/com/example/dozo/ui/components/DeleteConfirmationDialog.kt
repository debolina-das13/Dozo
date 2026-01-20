package com.example.dozo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DeleteConfirmationDialog(
    onConfirmDeleteInstance: () -> Unit, // For "Delete Once"
    onConfirmDeleteRule: () -> Unit,     // For "Delete All"
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = "Warning") },
        title = { Text("Delete Reminder") },
        text = { Text("Do you want to delete this reminder for just today, or delete the entire weekly schedule?") },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            // Show two confirmation buttons, stacked vertically
            Column {
                TextButton(onClick = onConfirmDeleteInstance) {
                    Text("Delete Just This Once")
                }
                TextButton(onClick = onConfirmDeleteRule) {
                    Text("Delete All (Permanent)")
                }
            }
        }
    )
}