package com.example.neuronexus.patient.models

import java.io.Serializable

// ==========================================
// 1. IDENTITY MODELS
// ==========================================

/**
 * Represents a person receiving care.
 * A single Auth User (Account Holder) can manage multiple PatientProfiles. New patient profile will
 * create at the time of booking or we can book for existing patients.
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
    open val updatedAt: Long = System.currentTimeMillis(),

    open val expiresAt: Long? = null,
    open val penaltyStatus: String = "NONE",
    open val previousBookingId: String? = null,
    open val exactTimeInMillis: Long = 0L
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

    override val previousBookingId: String? = null,
    override val exactTimeInMillis: Long = 0L,
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
) : Booking(bookingId, accountHolderId, patientProfileId, patientNameSnapshot,
    patientInfo, status, "doctor_appointment", payment, createdAt,
    updatedAt, null, "NONE", previousBookingId, exactTimeInMillis)

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

    override val previousBookingId: String? = null,
    override val exactTimeInMillis: Long = 0L,
    // Specifics
    val labId: String = "",
    val labName: String = "",
    val labImageUrl: String = "",
    val testId: String = "", // Added for better tracking
    val testName: String = "",
    val testType: String = "",
    val testDate: String = "",
    val testTime: String = "",

    // Composition: A lab booking results in a report
    val reportId: String? = null,

    // OPTIONAL LINK TO INSTALLMENT CONTRACT (Null means paid in full)
    val installmentPlanId: String? = null
) : Booking(bookingId, accountHolderId, patientProfileId, patientNameSnapshot, patientInfo,
    status, "lab_test", payment, createdAt, updatedAt, null,
    "NONE", previousBookingId, exactTimeInMillis)

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
    val labName: String = "",
    val patientProfileId: String = "",
    val testId: String = "",
    val testName: String = "",
    val testType: String = "",
    val testDate: String = "",
    val testTime: String = "",
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
    val transactionDate: Long = System.currentTimeMillis(),

    // Links a payment to a specific installment slot (Null means upfront/full payment)
    val installmentRecordId: String? = null
) : Serializable

// ==========================================
// 5. INSTALLMENT TRACKING (LEDGER SYSTEM)
// ==========================================

/**
 * Master contract generated at checkout if the user selects an installment plan.
 */
data class InstallmentPlan(
    val planId: String = "",
    val bookingId: String = "",          // Links back to LabTestBooking
    val totalAmount: Double = 0.0,       // Full price of the test
    val numInstallments: Int = 0,        // e.g., 4
    val installmentAmount: Double = 0.0, // totalAmount / numInstallments
    val startDate: Long = System.currentTimeMillis(),
    val status: String = "active"        // active, completed, defaulted
) : Serializable

/**
 * Individual ledger entry (slot) for a specific payment in a plan.
 */
data class InstallmentRecord(
    val recordId: String = "",
    val planId: String = "",             // Links back to InstallmentPlan
    val installmentNumber: Int = 0,      // 1, 2, 3, 4
    val dueDate: Long = 0L,              // Explicit due date for this slot
    val amount: Double = 0.0,            // Amount expected
    val status: String = "pending",      // pending, paid, overdue
    val paidAt: Long? = null,            // Timestamp of when it was paid
    val paymentId: String? = null        // The transaction that settled this slot
) : Serializable


data class Complaint(
    val complaintId: String = "",
    val submittedByUid: String = "",
    val submittedByRole: String = "",
    val submittedByName: String = "",
    val category: String = "",
    val subject: String = "",
    val description: String = "",
    val appointmentId: String? = null,
    val againstUserId: String? = null,
    val againstUserRole: String? = null,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val adminNote: String? = null
) : Serializable

data class Review(
    val reviewId: String = "",
    val appointmentId: String = "",
    val reviewedEntityId: String = "",
    val reviewedEntityType: String = "", // doctor | lab
    val reviewerId: String = "", // patientUid
    val reviewerName: String = "", // patientName
    val reviewerRole: String = "", // patient
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable