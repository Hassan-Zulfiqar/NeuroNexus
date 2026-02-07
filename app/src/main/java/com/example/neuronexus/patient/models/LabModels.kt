package com.example.neuronexus.patient.models

import java.io.Serializable

data class Lab(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val labDescription: String = "",
    val labTiming: String = "",
    val logo: String = "",
    val profilePicUrl: String = "",
    val licenseNumber: String = "",
    val licenseImageUrl: String = "",
    val registrationStatus: String = "", // e.g. "approved", "rejected"
    val rejectedAt: Long = 0L,

    // NEW: Rating fields for "Top Rated" logic
    val rating: Double = 0.0,
    val reviewCount: Int = 0
) : Serializable

data class LabTest(
    val id: String = "",
    val createdBy: String = "", // The Lab UID
    val testName: String = "",
    val category: String = "",
    val description: String = "",
    val price: String = "0", // Stored as String in JSON
    val installments: String = "no",
    val noOfInstallments: String = "0",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,

    // Future-proofing: Placeholder for sample report image
    val sampleReportImageUrl: String = ""
) : Serializable