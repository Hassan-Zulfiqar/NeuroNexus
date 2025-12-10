package com.example.neuronexus.doctor.models

data class Doctor(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val licenseNumber: String = "",
    val specialization: String = "",
    val qualification: String = "",
    val clinicAddress: String = "",
    val consultationFee: String = "",
    val schedule: String = "",
    val profileImageUrl: String = "",
    val licenseImageUrl: String = "",
    val registrationStatus: String = "pending",
    val rating: Float = 0.0f
)

