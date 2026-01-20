package com.example.dozo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dozo.data.MedicineRepository
import com.example.dozo.data.ReminderScheduler // <-- Make sure this is imported

class MedicineViewModelFactory(
    private val repository: MedicineRepository,
    private val scheduler: ReminderScheduler // <-- This is included
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicineViewModel(repository, scheduler) as T // <-- Both are passed
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}