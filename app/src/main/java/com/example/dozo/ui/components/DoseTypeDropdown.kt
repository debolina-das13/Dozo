package com.example.dozo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseTypeDropdown(
    onTypeSelected: (String) -> Unit
) {
    val doseTypes = listOf("Tablet", "Capsule", "Syrup", "Injection", "Drops")
    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(doseTypes[0]) }

    LaunchedEffect(selectedType) {
        onTypeSelected(selectedType)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Medicine Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            doseTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        selectedType = type
                        expanded = false
                    }
                )
            }
        }
    }
}