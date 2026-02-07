package com.example.neuronexus.doctor.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.models.DoctorAppointmentItem

class DoctorHomeViewModel : ViewModel() {

    private val _appointments = MutableLiveData<List<DoctorAppointmentItem>>()
    val appointments: LiveData<List<DoctorAppointmentItem>> = _appointments

    init {
        loadAppointments()
    }

    private fun loadAppointments() {
        // this data will come from a Repository, firebase
        val data = listOf(
            DoctorAppointmentItem("Eleanor Vance", "TUE", "24", "09:00 AM - 09:30 AM"),
            DoctorAppointmentItem("Marcus Holloway", "TUE", "24", "10:30 AM - 11:00 AM"),
            DoctorAppointmentItem("Clara Oswald", "TUE", "24", "01:00 PM - 01:45 PM"),
            DoctorAppointmentItem("John Smith", "WED", "25", "09:00 AM - 09:30 AM"),
            DoctorAppointmentItem("Sarah Jones", "WED", "25", "11:00 AM - 11:30 AM")
        )

        _appointments.value = data
    }
}