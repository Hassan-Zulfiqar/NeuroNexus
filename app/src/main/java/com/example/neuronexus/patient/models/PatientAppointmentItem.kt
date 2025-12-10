package com.example.neuronexus.patient.models

data class PatientAppointmentItem(
    val doctorName: String,
    val specialty: String,
    val date: String,
    val time: String,
    val doctorImageRes: Int,
    val status: String
)

