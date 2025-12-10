package com.example.neuronexus.doctor.models

data class DoctorAppointmentHistoryItem(
    val patientName: String,
    val purpose: String,
    val date: String,
    val time: String,
    val status: String
)

