package com.example.neuronexus.patient.models

data class HistoryConsultationItem(
    val doctorName: String,
    val specialty: String,
    val date: String,
    val time: String,
    val status: String,
    val imageResId: Int
)

