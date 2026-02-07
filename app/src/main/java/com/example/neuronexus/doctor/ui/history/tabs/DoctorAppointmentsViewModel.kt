package com.example.neuronexus.doctor.ui.history.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.models.DoctorAppointmentHistoryItem

class DoctorAppointmentsViewModel : ViewModel() {

    private val _appointments = MutableLiveData<List<DoctorAppointmentHistoryItem>>()
    val appointments: LiveData<List<DoctorAppointmentHistoryItem>> = _appointments

    init {
        loadData()
    }

    private fun loadData() {
        val list = listOf(
            DoctorAppointmentHistoryItem("Eleanor Vance", "Brain Tumor Test", "Dec 10, 2024", "10:00 AM", "Completed"),
            DoctorAppointmentHistoryItem("Marcus Holloway", "MRI Analysis", "Dec 09, 2024", "02:30 PM", "Cancelled"),
            DoctorAppointmentHistoryItem("Clara Oswald", "Consultation", "Dec 08, 2024", "11:15 AM", "Completed"),
            DoctorAppointmentHistoryItem("John Smith", "Follow-up", "Dec 05, 2024", "09:00 AM", "Completed")
        )
        _appointments.value = list
    }
}