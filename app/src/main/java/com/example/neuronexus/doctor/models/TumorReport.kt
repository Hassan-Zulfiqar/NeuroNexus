package com.example.neuronexus.doctor.models

data class TumorReport(
    val doctorId: String = "",
    val patientName: String = "",
    val patientAge: String = "",
    val patientGender: String = "",
    val prediction: String = "",
    val confidence: Double = 0.0,
    val hasTumor: Boolean = false,
    val tumorDetected: String = "",
    val location: String = "",
    val size: String = "",
    val areaPercentage: Double = 0.0,
    val pixelCount: Int = 0,
    val maxDiameterPixels: Double = 0.0,
    // Read-side fields populated from Firebase
    val recordId: String = "",
    val originalImageUrl: String = "",
    val overlayImageUrl: String = "",
    val pdfUrl: String = "",
    val timestamp: Long = 0L,
    val date: String = ""
)
