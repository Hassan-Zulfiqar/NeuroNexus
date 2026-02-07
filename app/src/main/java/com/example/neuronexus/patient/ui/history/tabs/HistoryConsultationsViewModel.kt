package com.example.neuronexus.patient.ui.history.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.R
import com.example.neuronexus.models.HistoryConsultationItem

class HistoryConsultationsViewModel : ViewModel() {

    private val _consultations = MutableLiveData<List<HistoryConsultationItem>>()
    val consultations: LiveData<List<HistoryConsultationItem>> = _consultations

    init {
        loadData()
    }

    private fun loadData() {
        val list = listOf(
            HistoryConsultationItem("Dr. Eleanor Vance", "Cardiologist", "Dec 12, 2024", "10:00 AM", "Completed", R.drawable.doctor),
            HistoryConsultationItem("Dr. John Smith", "Neurologist", "Nov 28, 2024", "02:30 PM", "Cancelled", R.drawable.doctor),
            HistoryConsultationItem("Dr. Sarah Lee", "Dermatologist", "Oct 15, 2024", "09:00 AM", "Completed", R.drawable.doctor),
            HistoryConsultationItem("Dr. Marcus Jones", "General Physician", "Sep 01, 2024", "11:15 AM", "Completed", R.drawable.doctor)
        )
        _consultations.value = list
    }
}