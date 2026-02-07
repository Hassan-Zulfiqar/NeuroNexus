package com.example.neuronexus.patient.models

import java.io.Serializable

// ==========================================
// 1. IDENTITY MODELS
// ==========================================

/**
 * Represents a person receiving care.
 * A single Auth User (Account Holder) can manage multiple PatientProfiles.
 */
data class PatientProfile(
    val profileId: String = "", // Unique UUID
    val accountHolderId: String = "", // Firebase Auth UID of the manager
    val fullName: String = "",
    val relation: String = "Self", // "Self", "Parent", "Child", "Spouse", "Other"
    val age: String = "",
    val gender: String = "",
    val contactNumber: String = "",
    val email: String = ""
) : Serializable

// ==========================================
// 2. BOOKING MODELS
// ==========================================

open class Booking(
    open val bookingId: String = "",
    open val accountHolderId: String = "", // User who booked (Auth ID)
    open val patientProfileId: String = "", // The actual patient (Profile ID)
    open val patientNameSnapshot: String = "", // Snapshot of name at time of booking (for history)

    // COMPOSITION: Booking contains a snapshot of the patient info
    open val patientInfo: PatientProfile = PatientProfile(),

    open val status: String = "pending", // pending, confirmed, completed, cancelled
    open val bookingType: String = "", // "doctor_appointment" or "lab_test"
    open val payment: Payment = Payment(),
    open val createdAt: Long = System.currentTimeMillis(),
    open val updatedAt: Long = System.currentTimeMillis()
) : Serializable

data class DoctorAppointment(
    override val bookingId: String = "",
    override val accountHolderId: String = "",
    override val patientProfileId: String = "",
    override val patientNameSnapshot: String = "",
    override val patientInfo: PatientProfile = PatientProfile(),
    override val status: String = "pending",
    override val payment: Payment = Payment(),
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),

    // Specifics
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorSpecialization: String = "",
    val doctorImageUrl: String = "",
    val appointmentDate: String = "", // e.g. "30 Jan 2026"
    val appointmentTime: String = "", // e.g. "08:00 PM"
    val reasonForVisit: String = "",

    // Composition: An appointment results in a prescription
    val prescriptionId: String? = null
) : Booking(bookingId, accountHolderId, patientProfileId, patientNameSnapshot, patientInfo, status, "doctor_appointment", payment, createdAt, updatedAt)

data class LabTestBooking(
    override val bookingId: String = "",
    override val accountHolderId: String = "",
    override val patientProfileId: String = "",
    override val patientNameSnapshot: String = "",
    override val patientInfo: PatientProfile = PatientProfile(),
    override val status: String = "pending",
    override val payment: Payment = Payment(),
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),

    // Specifics
    val labId: String = "",
    val labName: String = "",
    val testId: String = "", // Added for better tracking
    val testName: String = "",
    val testType: String = "",
    val testDate: String = "",
    val testTime: String = "",

    // Composition: A lab booking results in a report
    val reportId: String? = null
) : Booking(bookingId, accountHolderId, patientProfileId, patientNameSnapshot, patientInfo, status, "lab_test", payment, createdAt, updatedAt)

// ==========================================
// 3. MEDICAL RECORDS (Composition)
// ==========================================

data class MedicalRecord(
    val recordId: String = "",
    val patientProfileId: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable

data class Prescription(
    val prescriptionId: String = "",
    val bookingId: String = "", // Link back to the appointment
    val doctorId: String = "",
    val doctorName: String = "",
    val patientProfileId: String = "", // Who is this for?
    val appointmentTime: Long = 0L,

    val medications: List<String> = emptyList(),
    val instructions: String = "",
    val diagnosis: String = "",

    val followUpDate: Long? = null,
    val issuedDate: Long = System.currentTimeMillis()
) : Serializable

data class LabReport(
    val reportId: String = "",
    val bookingId: String = "",
    val labId: String = "",
    val patientProfileId: String = "",
    val testName: String = "",
    val resultSummary: String = "",
    val fileUrl: String = "",
    val issuedDate: Long = System.currentTimeMillis()
) : Serializable

// ==========================================
// 4. UTILITY MODELS
// ==========================================

data class Payment(
    val paymentId: String = "",
    val amount: Double = 0.0,
    val currency: String = "PKR",
    val paymentMethod: String = "",
    val paymentStatus: String = "pending",
    val transactionDate: Long = System.currentTimeMillis()
) : Serializable