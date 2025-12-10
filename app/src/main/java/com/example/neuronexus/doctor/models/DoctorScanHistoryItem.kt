package com.example.neuronexus.doctor.models

data class DoctorScanHistoryItem(
    val prediction: String,
    val confidence: String,
    val patientName: String,
    val date: String,
    val imageResId: Int
)

