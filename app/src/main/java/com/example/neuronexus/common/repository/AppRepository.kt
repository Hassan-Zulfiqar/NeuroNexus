package com.example.neuronexus.common.repository

import android.net.Uri
import android.util.Log
import com.example.neuronexus.common.models.AppAnnouncement
import com.example.neuronexus.common.models.AppNotification
import com.example.neuronexus.common.models.User
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.models.Patient
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import com.example.neuronexus.patient.models.LabTestBooking
import com.example.neuronexus.patient.models.PatientProfile
import com.example.neuronexus.patient.models.InstallmentPlan
import com.example.neuronexus.patient.models.InstallmentRecord
import com.example.neuronexus.patient.models.LabReport
import com.example.neuronexus.patient.models.Prescription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class AppRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    interface RegisterCallback {
        fun onSuccess(message: String)
        fun onError(message: String)
    }

    interface LoginCallback {
        fun onSuccess(user: User)
        fun onError(message: String)
    }

    // ----------------------------------------------------------------
    // 1. REGISTER DOCTOR
    // ----------------------------------------------------------------
    fun registerDoctor(
        doctor: Doctor,
        password: String,
        profileImageUri: Uri?,
        licenseImageUri: Uri?,
        callback: RegisterCallback
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            currentUser.updatePassword(password)
                .addOnSuccessListener {
                    saveDoctorToDatabase(currentUser.uid, doctor, profileImageUri, licenseImageUri, callback)
                }
                .addOnFailureListener { e ->
                    callback.onError("Failed to link password: ${e.message}")
                }
        } else {
            auth.createUserWithEmailAndPassword(doctor.email, password)
                .addOnFailureListener { callback.onError(it.message ?: "Auth Failed") }
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener
                    saveDoctorToDatabase(uid, doctor, profileImageUri, licenseImageUri, callback)
                }
        }
    }

    // ----------------------------------------------------------------
    // 2. REGISTER PATIENT
    // ----------------------------------------------------------------
    fun registerPatient(
        patient: Patient,
        password: String,
        profileImageUri: Uri?,
        callback: RegisterCallback
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            currentUser.updatePassword(password)
                .addOnSuccessListener {
                    savePatientToDatabase(currentUser.uid, patient, profileImageUri, callback)
                }
                .addOnFailureListener { e ->
                    callback.onError("Failed to link password: ${e.message}")
                }
        } else {
            auth.createUserWithEmailAndPassword(patient.email, password)
                .addOnFailureListener { callback.onError(it.message ?: "Auth Failed") }
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener
                    savePatientToDatabase(uid, patient, profileImageUri, callback)
                }
        }
    }

    // ----------------------------------------------------------------
    // 3. LOGIN USER (Email/Password)
    // ----------------------------------------------------------------
    fun loginUser(email: String, pass: String, callback: LoginCallback) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnFailureListener {
                callback.onError(it.message ?: "Login Failed")
            }
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                fetchUserAndVerify(uid, callback)
            }
    }

    // ----------------------------------------------------------------
    // 4. GOOGLE SIGN IN
    // ----------------------------------------------------------------
    fun firebaseAuthWithGoogle(idToken: String, callback: LoginCallback) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnFailureListener {
                callback.onError(it.message ?: "Google Auth Failed")
            }
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener
                val email = authResult.user?.email ?: ""

                db.child("users").child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists() && snapshot.hasChild("role")) {
                            fetchUserAndVerify(uid, callback)
                        } else {
                            val newUser = User(uid = uid, email = email, role = "new_user")
                            callback.onSuccess(newUser)
                        }
                    }
                    .addOnFailureListener {
                        callback.onError("Database Error: ${it.message}")
                    }
            }
    }

    // ----------------------------------------------------------------
    // 5. RESET PASSWORD
    // ----------------------------------------------------------------
    fun sendPasswordResetEmail(email: String, callback: RegisterCallback) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                callback.onSuccess("Reset link sent to your email!")
            }
            .addOnFailureListener {
                callback.onError(it.message ?: "Failed to send reset email")
            }
    }

    // ----------------------------------------------------------------
    // 6. UTILITIES (Session / User Fetching)
    // ----------------------------------------------------------------
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    fun getUser(uid: String, callback: LoginCallback) {
        fetchUserAndVerify(uid, callback)
    }

    // ----------------------------------------------------------------
    // 7. DOCTOR DISCOVERY
    // ----------------------------------------------------------------
    fun getAllApprovedDoctors(callback: (Result<List<Doctor>>) -> Unit) {
        db.child("doctors").get()
            .addOnSuccessListener { snapshot ->
                val doctorsList = mutableListOf<Doctor>()
                for (docSnap in snapshot.children) {
                    val doctor = docSnap.getValue(Doctor::class.java)
                    if (doctor != null && doctor.registrationStatus == "approved") {
                        doctorsList.add(doctor)
                    }
                }
                callback(Result.success(doctorsList))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 8. SCHEDULE / APPOINTMENT FETCHING
    // ----------------------------------------------------------------
    fun getBookedSlots(
        providerId: String,
        dateString: String,
        providerType: String, // "doctor" or "lab"
        callback: (Result<List<String>>) -> Unit
    ) {
        // 1. Determine which foreign key to query based on provider type
        val queryKey = if (providerType == "doctor") "doctorId" else "labId"

        db.child("appointments").orderByChild(queryKey).equalTo(providerId).get()
            .addOnSuccessListener { snapshot ->
                val bookedSlots = mutableListOf<String>()

                for (child in snapshot.children) {
                    try {
                        // 2. Determine the correct date/time fields based on the booking type models
                        val dateField = if (providerType == "doctor") "appointmentDate" else "testDate"
                        val timeField = if (providerType == "doctor") "appointmentTime" else "testTime"

                        // 3. Safely extract just the date and time strings directly from the snapshot.
                        val date = child.child(dateField).getValue(String::class.java)
                        val time = child.child(timeField).getValue(String::class.java)

                        // 4. If the date matches what the user selected, add the time to the booked list
                        if (date == dateString && time != null) {
                            bookedSlots.add(time)
                        }
                    } catch (e: Exception) {
                        // Ignore malformed nodes silently
                    }
                }
                callback(Result.success(bookedSlots))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }


    // ----------------------------------------------------------------
    // 9. PATIENT PROFILE MANAGEMENT
    // ----------------------------------------------------------------

    fun savePatientProfile(
        accountHolderId: String,
        profile: PatientProfile,
        callback: (Result<PatientProfile>) -> Unit
    ) {
        val profileRef = db.child("users")
            .child(accountHolderId)
            .child("patient_profiles")
            .push()

        val stableProfileId = profileRef.key ?: java.util.UUID.randomUUID().toString()

        val profileWithId = profile.copy(
            profileId = stableProfileId,
            accountHolderId = accountHolderId
        )

        profileRef.setValue(profileWithId)
            .addOnSuccessListener {
                callback(Result.success(profileWithId))
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }

    fun getPatientProfiles(accountHolderId: String, callback: (Result<List<PatientProfile>>) -> Unit) {
        db.child("users").child(accountHolderId).child("patient_profiles").get()
            .addOnSuccessListener { snapshot ->
                val profiles = mutableListOf<PatientProfile>()
                for (child in snapshot.children) {
                    // Map Firebase node key to profileId after deserialization
                    val profile = child.getValue(PatientProfile::class.java)?.copy(profileId = child.key ?: "")
                    if (profile != null) {
                        profiles.add(profile)
                    }
                }

                callback(Result.success(profiles))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 10. BOOKING (SAVE)
    // ----------------------------------------------------------------
    fun saveBooking(
        booking: Booking,
        // NEW: Optional installment parameters. Defaulting to null/empty ensures
        // 100% backward compatibility with existing Doctor booking calls.
        installmentPlan: InstallmentPlan? = null,
        installmentRecords: List<InstallmentRecord> = emptyList(),
        callback: (Result<String>) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(Result.failure(Exception("User not authenticated")))
            return
        }

        // Safety guard — profileId must be valid before booking proceeds
        if (booking.patientInfo.profileId.isEmpty()) {
            callback(Result.failure(Exception("Patient profile ID is missing. Please re-select patient and try again.")))
            return
        }

        // Profile already saved to Firebase in PatientSelectionFragment
        // profileId is guaranteed stable at this point
        val profileId = booking.patientInfo.profileId

        val bookingId = if (booking.bookingId.isEmpty()) {
            db.child("appointments").push().key ?: java.util.UUID.randomUUID().toString()
        } else {
            booking.bookingId
        }

        val updates = hashMapOf<String, Any>()

        // ====================================================================
        // NEW INSTALLMENT & ATOMIC BATCH LOGIC
        // ====================================================================

        var dummyPayment = booking.payment.copy(
            paymentId = "PAY_DUMMY_${System.currentTimeMillis()}",
            amount = booking.payment.amount,
            // If an installment plan exists, this first payment is strictly "partial"
            paymentStatus = if (installmentPlan != null) "partial" else "pending",
            paymentMethod = "PAY_AT_CLINIC",
            transactionDate = System.currentTimeMillis()
        )

        if (installmentPlan != null && installmentRecords.isNotEmpty()) {
            // Find the first record to settle immediately (Bi-directional Traceability)
            val firstRecord = installmentRecords.firstOrNull { it.installmentNumber == 1 }

            if (firstRecord != null) {
                // Link upfront payment transaction to this specific ledger slot
                dummyPayment = dummyPayment.copy(installmentRecordId = firstRecord.recordId)
            }

            // Queue the master contract to the batch write
            updates["/installment_plans/${installmentPlan.planId}"] = installmentPlan

            // Queue all generated slots to the batch write
            installmentRecords.forEach { record ->
                val finalRecord = if (record.installmentNumber == 1) {
                    // Settle the first installment automatically with the upfront payment
                    record.copy(
                        status = "paid",
                        paidAt = System.currentTimeMillis(),
                        paymentId = dummyPayment.paymentId
                    )
                } else {
                    record
                }
                updates["/installment_records/${finalRecord.recordId}"] = finalRecord
            }
        }

        // ====================================================================
        // END OF NEW INSTALLMENT & ATOMIC BATCH LOGIC
        // ====================================================================

        val finalBooking = when (booking) {
            is DoctorAppointment -> {
                booking.copy(
                    bookingId = bookingId,
                    accountHolderId = currentUser.uid,
                    patientProfileId = profileId,
                    patientInfo = booking.patientInfo,
                    payment = dummyPayment,
                    status = "pending",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            is LabTestBooking -> {
                booking.copy(
                    bookingId = bookingId,
                    accountHolderId = currentUser.uid,
                    patientProfileId = profileId,
                    patientInfo = booking.patientInfo,
                    payment = dummyPayment,
                    status = "pending",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    // NEW: Link the booking to the installment plan if it exists
                    installmentPlanId = installmentPlan?.planId
                )
            }
            else -> booking
        }

        // Add core updates to the batch
        updates["/appointments/$bookingId"] = finalBooking

        // Commit the atomic batch
        db.updateChildren(updates)
            .addOnSuccessListener {
                callback(Result.success(bookingId))
                // NOTIFICATION ---
                // NOTIFICATION ---
                if (booking is DoctorAppointment) {
                    createNotification(
                        recipientUid = booking.doctorId,
                        senderName = booking.patientNameSnapshot,
                        type = "NEW_BOOKING",
                        title = "New Appointment Request",
                        message = "${booking.patientNameSnapshot} has requested an appointment on ${booking.appointmentDate} at ${booking.appointmentTime}",
                        bookingId = bookingId,
                        referenceId = null
                    )
                }
                else if (booking is LabTestBooking) {
                    createNotification(
                        recipientUid = booking.labId,
                        senderName = booking.patientNameSnapshot,
                        type = "NEW_LAB_BOOKING",
                        title = "New Lab Test Booking",
                        message = "${booking.patientNameSnapshot} has booked a ${booking.testName} test on ${booking.testDate} at ${booking.testTime}",
                        bookingId = bookingId,
                        referenceId = null
                    )
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ----------------------------------------------------------------
    // 11. FETCH PATIENT APPOINTMENTS
    // ----------------------------------------------------------------
    fun getMyAppointments(uid: String, callback: (Result<List<Booking>>) -> Unit) {
        db.child("appointments").orderByChild("accountHolderId").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                val appointments = mutableListOf<Booking>()

                for (child in snapshot.children) {
                    try {
                        // 1. Safe Type Checking: Inspecting the raw JSON keys before parsing
                        if (child.hasChild("doctorId")) {
                            // It's a Doctor Appointment
                            val doctorAppointment = child.getValue(DoctorAppointment::class.java)
                            if (doctorAppointment != null) {
                                appointments.add(doctorAppointment)
                            }
                        } else if (child.hasChild("labId")) {
                            // It's a Lab Booking
                            val labBooking = child.getValue(LabTestBooking::class.java)
                            if (labBooking != null) {
                                appointments.add(labBooking)
                            }
                        }
                    } catch (e: Exception) {
                        // Safe parsing: If one item fails to parse, ignore it
                        // so the rest of the valid timeline still loads.
                    }
                }
                callback(Result.success(appointments))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ----------------------------------------------------------------
    // 11.2 FETCH DOCTOR APPOINTMENTS (NEW)
    // ----------------------------------------------------------------
    fun getDoctorAppointments(uid: String, callback: (Result<List<DoctorAppointment>>) -> Unit) {
        db.child("appointments").orderByChild("doctorId").equalTo(uid).get()
            .addOnSuccessListener { snapshot ->
                val appointments = mutableListOf<DoctorAppointment>()

                for (child in snapshot.children) {
                    try {
                        val appointment = child.getValue(DoctorAppointment::class.java)
                        if (appointment != null) {
                            appointments.add(appointment)
                        }
                    } catch (e: Exception) {
                        // Safe parsing: Ignore malformed nodes so the rest of the list loads
                    }
                }
                callback(Result.success(appointments))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ----------------------------------------------------------------
    // 11.5 CANCEL BOOKING
    // ----------------------------------------------------------------
    fun cancelBooking(booking: Booking, callback: (Result<String>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(Result.failure(Exception("User not authenticated")))
            return
        }

        val updates = hashMapOf<String, Any>()

        // 1. Payment-Aware Stub (Stripe Integration Hook)
        // If the payment method was ONLINE, we flag it for the future refund Cloud Function.
        val penaltyStatus = if (booking.payment.paymentMethod == "ONLINE") {
            "PENDING_REFUND"
        } else {
            "NONE"
        }

        // 2. Cancel the main booking and apply the penalty/refund status
        updates["/appointments/${booking.bookingId}/status"] = "cancelled"
        updates["/appointments/${booking.bookingId}/penaltyStatus"] = penaltyStatus
        updates["/appointments/${booking.bookingId}/updatedAt"] = System.currentTimeMillis()

        // 3. Cancel associated Installment Plan if it exists (Lab Tests)
        if (booking is LabTestBooking && booking.installmentPlanId != null) {
            updates["/installment_plans/${booking.installmentPlanId}/status"] = "cancelled"
        }

        // 4. Execute Atomic Batch Update
        db.updateChildren(updates)
            .addOnSuccessListener {
                callback(Result.success(booking.bookingId))
                if (booking is DoctorAppointment) {
                    createNotification(
                        recipientUid = booking.doctorId,
                        senderName = booking.patientNameSnapshot,
                        type = "BOOKING_CANCELLED",
                        title = "Appointment Cancelled",
                        message = "${booking.patientNameSnapshot} has cancelled their appointment",
                        bookingId = booking.bookingId,
                        referenceId = null
                    )
                }
                else if (booking is LabTestBooking) {
                    createNotification(
                        recipientUid = booking.labId,
                        senderName = booking.patientNameSnapshot,
                        type = "LAB_BOOKING_CANCELLED",
                        title = "Lab Booking Cancelled",
                        message = "${booking.patientNameSnapshot} has cancelled their ${booking.testName} booking",
                        bookingId = booking.bookingId,
                        referenceId = null
                    )
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ==========================================
    // PRESCRIPTION FUNCTIONS
    // ==========================================

    fun savePrescription(
        prescription: Prescription,
        callback: (Result<String>) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(Result.failure(Exception("User not authenticated")))
            return
        }

        // Validate required fields before any Firebase operation
        if (prescription.patientProfileId.isEmpty()) {
            callback(Result.failure(Exception("Invalid patient profile")))
            return
        }
        if (prescription.bookingId.isEmpty()) {
            callback(Result.failure(Exception("Invalid booking reference")))
            return
        }
        if (prescription.diagnosis.isBlank()) {
            callback(Result.failure(Exception("Diagnosis is required")))
            return
        }
        if (prescription.medications.isEmpty()) {
            callback(Result.failure(Exception("At least one medication is required")))
            return
        }

        // Generate stable prescriptionId
        val prescriptionRef = db
            .child("medical_records")
            .child(prescription.patientProfileId)
            .child("prescriptions")
            .push()

        val prescriptionId = prescriptionRef.key
            ?: java.util.UUID.randomUUID().toString()

        val prescriptionWithId = prescription.copy(
            prescriptionId = prescriptionId
        )

        // Atomic write — both paths in single updateChildren
        val updates = hashMapOf<String, Any>(
            "/medical_records/${prescription.patientProfileId}/prescriptions/$prescriptionId"
                    to prescriptionWithId,
            "/appointments/${prescription.bookingId}/prescriptionId"
                    to prescriptionId
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                callback(Result.success(prescriptionId))
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }

    // ----------------------------------------------------------------
    // DOCTOR SCHEDULE & STATUS UPDATES
    // ----------------------------------------------------------------

    fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        accountHolderId: String,
        patientName: String,
        doctorName: String,
        callback: (Result<String>) -> Unit
    ) {
        val updates = mapOf(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis()
        )

        db.child("appointments").child(appointmentId).updateChildren(updates)
            .addOnSuccessListener {
                // Fire existing callback immediately
                callback(Result.success(newStatus))

                // --- NOTIFICATION ---
                val type: String
                val title: String
                val message: String

                when (newStatus) {
                    "confirmed" -> {
                        type = "BOOKING_CONFIRMED"
                        title = "Appointment Confirmed"
                        message = "Dr. $doctorName has confirmed your appointment"
                    }
                    "rejected" -> {
                        type = "BOOKING_REJECTED"
                        title = "Appointment Rejected"
                        message = "Dr. $doctorName has rejected your appointment request"
                    }
                    "completed" -> {
                        type = "APPOINTMENT_COMPLETED"
                        title = "Appointment Completed"
                        message = "Your appointment with Dr. $doctorName has been marked as complete"
                    }
                    "no_show" -> {
                        type = "NO_SHOW"
                        title = "Missed Appointment"
                        message = "You were marked as a no-show for your appointment with Dr. $doctorName. Repeated no-shows will result in your account being blocked from future bookings."
                    }
                    else -> return@addOnSuccessListener // Skip if unknown status
                }

                createNotification(
                    recipientUid = accountHolderId,
                    senderName = doctorName,
                    type = type,
                    title = title,
                    message = message,
                    bookingId = appointmentId,
                    referenceId = null
                )
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
    // will be implement using cloud function later
    fun checkAndExpirePendingAppointments(
        accountHolderId: String,
        callback: (Result<Int>) -> Unit  // returns count of expired appointments
    ) {
        val currentUser = auth.currentUser ?: run {
            callback(Result.failure(Exception("Not authenticated")))
            return
        }

        db.child("appointments")
            .orderByChild("accountHolderId")
            .equalTo(accountHolderId)
            .get()
            .addOnSuccessListener { snapshot ->
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis

                val updates = hashMapOf<String, Any>()
                var expiredCount = 0

                for (child in snapshot.children) {
                    // Determine booking type and extract relevant fields
                    val status = child.child("status").getValue(String::class.java)
                        ?.lowercase() ?: continue
                    if (status != "pending") continue

                    // Get appointment date — DoctorAppointment uses "appointmentDate"
                    // LabTestBooking uses "testDate"
                    // Check both fields — whichever is non-null is the correct type
                    val dateString = child.child("appointmentDate")
                        .getValue(String::class.java)?.takeIf { it.isNotBlank() }
                        ?: child.child("testDate")
                            .getValue(String::class.java)?.takeIf { it.isNotBlank() }
                        ?: continue  // neither field exists — skip

                    val appointmentDate = try {
                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            .parse(dateString)?.time ?: continue
                    } catch (e: Exception) { continue }

                    if (appointmentDate < today) {
                        val bookingId = child.key ?: continue
                        updates["/appointments/$bookingId/status"] = "expired"
                        updates["/appointments/$bookingId/updatedAt"] = System.currentTimeMillis()
                        expiredCount++
                    }
                }

                if (updates.isEmpty()) {
                    callback(Result.success(0))
                    return@addOnSuccessListener
                }

                db.updateChildren(updates)
                    .addOnSuccessListener { callback(Result.success(expiredCount)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun incrementNoShowCount(patientUid: String, callback: (Result<Unit>) -> Unit) {
        val noShowRef = db.child("users").child(patientUid).child("metrics").child("no_show_count")

        noShowRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                // Treat null (non-existent) as 0, then increment by 1
                val currentCount = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentCount + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    callback(Result.failure(error.toException()))
                } else if (!committed) {
                    callback(Result.failure(Exception("Transaction aborted.")))
                } else {
                    callback(Result.success(Unit))
                }
            }
        })
    }

    // ----------------------------------------------------------------
    // 12. FETCH SINGLE DOCTOR and LAB
    // ----------------------------------------------------------------
    fun getDoctor(uid: String, callback: (Result<Doctor>) -> Unit) {
        db.child("doctors").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val doctor = snapshot.getValue(Doctor::class.java)
                if (doctor != null) {
                    callback(Result.success(doctor))
                } else {
                    callback(Result.failure(Exception("Doctor not found")))
                }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun getLab(uid: String, callback: (Result<Lab>) -> Unit) {
        db.child("labs").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val lab = snapshot.getValue(Lab::class.java)
                if (lab != null) {
                    callback(Result.success(lab))
                } else {
                    callback(Result.failure(Exception("Lab not found")))
                }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 13. FETCH PATIENT DETAILS
    // ----------------------------------------------------------------
    fun getPatient(uid: String, callback: (Result<Patient>) -> Unit) {
        db.child("patients").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val patient = snapshot.getValue(Patient::class.java)
                if (patient != null) {
                    callback(Result.success(patient))
                } else {
                    callback(Result.failure(Exception("Patient profile not found")))
                }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 14. UPDATE PATIENT PROFILE (TEXT)
    // ----------------------------------------------------------------
    fun updatePatientProfile(uid: String, updates: Map<String, Any>, callback: (Result<String>) -> Unit) {
        db.child("patients").child(uid).updateChildren(updates)
            .addOnSuccessListener { callback(Result.success("Profile Updated")) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    // ----------------------------------------------------------------
    // 15. UPLOAD PATIENT IMAGE
    // ----------------------------------------------------------------
    fun uploadPatientImage(uid: String, uri: Uri, callback: (Result<String>) -> Unit) {
        val profileRef = storage.child("profile_pics/patients/$uid.jpg")
        uploadImage(profileRef, uri) { url ->
            if (url.isNotEmpty()) {
                callback(Result.success(url))
            } else {
                callback(Result.failure(Exception("Failed to upload image")))
            }
        }
    }

    // ----------------------------------------------------------------
    // DOCTOR PROFILE UPDATES
    // ----------------------------------------------------------------

    fun updateDoctorProfile(uid: String, updates: Map<String, Any>, callback: (Result<Unit>) -> Unit) {
        db.child("doctors").child(uid).updateChildren(updates)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun uploadDoctorProfileImage(imageUri: android.net.Uri, uid: String, callback: (Result<String>) -> Unit) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("doctor_profiles/$uid.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl
                    .addOnSuccessListener { uri ->
                        callback(Result.success(uri.toString()))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ----------------------------------------------------------------
    // 16. LAB METHODS
    // ----------------------------------------------------------------

    fun getAllLabs(callback: (Result<List<Lab>>) -> Unit) {
        db.child("labs").orderByChild("registrationStatus").equalTo("approved")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val labs = mutableListOf<Lab>()
                    for (child in snapshot.children) {
                        val lab = child.getValue(Lab::class.java)
                        if (lab != null) {
                            labs.add(lab)
                        }
                    }
                    callback(Result.success(labs))
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(Result.failure(error.toException()))
                }
            })
    }

    fun getTestsForLab(labId: String, callback: (Result<List<LabTest>>) -> Unit) {
        // NOTE: Requires index on 'tests' node: ".indexOn": ["createdBy"]
        db.child("tests").orderByChild("createdBy").equalTo(labId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tests = mutableListOf<LabTest>()
                    for (child in snapshot.children) {
                        val test = child.getValue(LabTest::class.java)
                        if (test != null) {
                            tests.add(test)
                        }
                    }
                    callback(Result.success(tests))
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(Result.failure(error.toException()))
                }
            })
    }

    fun getLabTest(testId: String, callback: (Result<LabTest>) -> Unit) {
        db.child("tests").child(testId).get()
            .addOnSuccessListener { snapshot ->
                val test = snapshot.getValue(LabTest::class.java)
                if (test != null) {
                    callback(Result.success(test))
                } else {
                    callback(Result.failure(Exception("Lab Test not found")))
                }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // PRIVATE HELPER FUNCTIONS
    // ----------------------------------------------------------------

    private fun saveDoctorToDatabase(
        uid: String,
        doctor: Doctor,
        profileImageUri: Uri?,
        licenseImageUri: Uri?,
        callback: RegisterCallback
    ) {
        db.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && snapshot.hasChild("role")) {
                callback.onError("User already registered. Please login.")
                return@addOnSuccessListener
            }

            val profileRef = storage.child("profile_pics/doctors/$uid.jpg")
            val licenseRef = storage.child("license_images/doctors/$uid.jpg")

            uploadImage(profileRef, profileImageUri) { profileUrl ->
                uploadImage(licenseRef, licenseImageUri) { licenseUrl ->
                    val finalDoctor = doctor.copy(
                        uid = uid,
                        profileImageUrl = profileUrl,
                        licenseImageUrl = licenseUrl,
                        registrationStatus = "pending"
                    )

                    val userEntry = User(
                        uid = uid,
                        email = doctor.email,
                        role = "doctor",
                        status = "active",
                        createdAt = System.currentTimeMillis()
                    )

                    db.child("users").child(uid).setValue(userEntry)
                    db.child("doctors").child(uid).setValue(finalDoctor)
                        .addOnSuccessListener {
                            callback.onSuccess("Doctor Registration Successful! Please wait for approval.")
                        }
                        .addOnFailureListener {
                            callback.onError(it.message ?: "Database Error")
                        }
                }
            }
        }.addOnFailureListener {
            callback.onError("Database check failed.")
        }
    }

    private fun savePatientToDatabase(
        uid: String,
        patient: Patient,
        profileImageUri: Uri?,
        callback: RegisterCallback
    ) {
        db.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && snapshot.hasChild("role")) {
                callback.onError("User already registered. Please login.")
                return@addOnSuccessListener
            }

            val profileRef = storage.child("profile_pics/patients/$uid.jpg")

            uploadImage(profileRef, profileImageUri) { profileUrl ->
                val finalPatient = patient.copy(
                    uid = uid,
                    profileImageUrl = profileUrl
                )

                val userEntry = User(
                    uid = uid,
                    email = patient.email,
                    role = "patient",
                    status = "active",
                    createdAt = System.currentTimeMillis()
                )

                db.child("users").child(uid).setValue(userEntry)
                db.child("patients").child(uid).setValue(finalPatient)
                    .addOnSuccessListener {
                        callback.onSuccess("Patient Registration Successful!")
                    }
                    .addOnFailureListener {
                        callback.onError(it.message ?: "Database Error")
                    }
            }
        }.addOnFailureListener {
            callback.onError("Database check failed.")
        }
    }

    private fun fetchUserAndVerify(uid: String, callback: LoginCallback) {
        db.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        if (user.status == "blocked" || user.status == "block") {
                            auth.signOut()
                            callback.onError("Your account has been blocked by Admin.")
                            return@addOnSuccessListener
                        }

                        if (user.role == "doctor") {
                            checkDoctorApproval(uid, user, callback)
                        } else {
                            callback.onSuccess(user)
                        }
                    } else {
                        callback.onError("User data corrupted")
                    }
                } else {
                    callback.onError("User record not found")
                }
            }
            .addOnFailureListener {
                callback.onError(it.message ?: "Database Error")
            }
    }

    private fun checkDoctorApproval(uid: String, user: User, callback: LoginCallback) {
        db.child("doctors").child(uid).get()
            .addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    val status = docSnapshot.child("registrationStatus").getValue(String::class.java) ?: "pending"
                    when (status) {
                        "approved" -> callback.onSuccess(user)
                        "pending" -> {
                            auth.signOut()
                            callback.onError("Your registration is still Pending approval.")
                        }
                        "rejected" -> {
                            auth.signOut()
                            callback.onError("Your registration was Rejected. Contact support.")
                        }
                        else -> {
                            auth.signOut()
                            callback.onError("Unknown registration status.")
                        }
                    }
                } else {
                    callback.onError("Doctor profile not found")
                }
            }
            .addOnFailureListener {
                callback.onError("Failed to verify doctor status")
            }
    }

    // ==========================================
    // SINGLE BOOKING FETCH
    // ==========================================
    fun getBookingById(
        bookingId: String,
        callback: (Result<Booking?>) -> Unit
    ) {
        db.child("appointments")
            .child(bookingId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(Result.success(null))
                    return@addOnSuccessListener
                }

                // Try DoctorAppointment first
                val doctorAppointment = snapshot
                    .getValue(DoctorAppointment::class.java)
                if (doctorAppointment != null &&
                    doctorAppointment.doctorId.isNotEmpty()) {
                    callback(Result.success(
                        doctorAppointment.copy(bookingId = snapshot.key ?: "")
                    ))
                    return@addOnSuccessListener
                }

                // Try LabTestBooking
                val labBooking = snapshot
                    .getValue(LabTestBooking::class.java)
                if (labBooking != null &&
                    labBooking.labId.isNotEmpty()) {
                    callback(Result.success(
                        labBooking.copy(bookingId = snapshot.key ?: "")
                    ))
                    return@addOnSuccessListener
                }

                // Cannot determine type
                callback(Result.success(null))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun getDoctorAppointmentById(
        appointmentId: String,
        callback: (Result<DoctorAppointment?>) -> Unit
    ) {
        db.child("appointments")
            .child(appointmentId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(Result.success(null))
                    return@addOnSuccessListener
                }

                // Deserialize as DoctorAppointment
                val doctorAppointment = snapshot
                    .getValue(DoctorAppointment::class.java)
                if (doctorAppointment != null &&
                    doctorAppointment.doctorId.isNotEmpty()) {
                    callback(Result.success(
                        doctorAppointment.copy(bookingId = snapshot.key ?: "")
                    ))
                } else {
                    // Not a doctor appointment
                    callback(Result.success(null))
                }
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ==========================================
    // PRESCRIPTION FUNCTIONS
    // ==========================================
    // single prescription fetch
    fun getPrescription(
        patientProfileId: String,
        prescriptionId: String,
        callback: (Result<Prescription>) -> Unit
    ) {
        db.child("medical_records")
            .child(patientProfileId)
            .child("prescriptions")
            .child(prescriptionId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prescription = snapshot.getValue(Prescription::class.java)
                    if (prescription != null) {
                        callback(Result.success(prescription))
                    } else {
                        callback(Result.failure(Exception("Prescription not found")))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(Result.failure(error.toException()))
                }
            })
    }

    //for patient (fetch all)
    fun getPatientPrescriptions(
        patientProfileId: String,
        callback: (Result<List<Prescription>>) -> Unit
    ) {
        db.child("medical_records")
            .child(patientProfileId)
            .child("prescriptions")
            .get()
            .addOnSuccessListener { snapshot ->
                val prescriptions = mutableListOf<Prescription>()
                for (child in snapshot.children) {
                    val prescription = child.getValue(Prescription::class.java)
                        ?.copy(prescriptionId = child.key ?: "")
                    if (prescription != null) {
                        prescriptions.add(prescription)
                    }
                }
                prescriptions.sortByDescending { it.issuedDate }
                callback(Result.success(prescriptions))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun getPatientLabReports(
        patientProfileId: String,
        callback: (Result<List<LabReport>>) -> Unit
    ) {
        db.child("medical_records")
            .child(patientProfileId)
            .child("lab_reports")
            .get()
            .addOnSuccessListener { snapshot ->
                val reports = mutableListOf<LabReport>()
                for (child in snapshot.children) {
                    val report = child.getValue(LabReport::class.java)
                        ?.copy(reportId = child.key ?: "")
                    if (report != null) {
                        reports.add(report)
                    }
                }
                reports.sortByDescending { it.issuedDate }
                callback(Result.success(reports))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    fun getLabReport(
        patientProfileId: String,
        reportId: String,
        callback: (Result<LabReport?>) -> Unit
    ) {
        db.child("medical_records")
            .child(patientProfileId)
            .child("lab_reports")
            .child(reportId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    callback(Result.success(null))
                    return@addOnSuccessListener
                }
                val report = snapshot.getValue(LabReport::class.java)
                    ?.copy(reportId = snapshot.key ?: "")
                callback(Result.success(report))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }


    // ==========================================
    // NOTIFICATION FUNCTIONS
    // ==========================================

    private fun createNotification(
        recipientUid: String,
        senderName: String,
        type: String,
        title: String,
        message: String,
        bookingId: String? = null,
        referenceId: String? = null
    ) {
        val notificationRef = db.child("notifications").child(recipientUid).push()
        val pushKey = notificationRef.key ?: return

        val notification = AppNotification(
            notificationId = pushKey,
            recipientUid = recipientUid,
            senderName = senderName,
            type = type,
            title = title,
            message = message,
            bookingId = bookingId,
            referenceId = referenceId,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )

        notificationRef.setValue(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Notification created: $type")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Failed to create notification: ${e.message}")
            }
    }

    fun getNotifications(
        uid: String,
        callback: (Result<List<AppNotification>>) -> Unit
    ): ValueEventListener {
        val ref = db.child("notifications").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<AppNotification>()
                for (child in snapshot.children) {
                    val notification = child.getValue(AppNotification::class.java)
                        ?.copy(notificationId = child.key ?: "")
                    if (notification != null) {
                        notifications.add(notification)
                    }
                }
                // Sort by createdAt descending — newest first
                notifications.sortByDescending { it.createdAt }
                callback(Result.success(notifications))
            }
            override fun onCancelled(error: DatabaseError) {
                callback(Result.failure(error.toException()))
            }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun getAnnouncements(
        targetAudience: String,
        callback: (Result<List<AppAnnouncement>>) -> Unit
    ): ValueEventListener {
        val ref = db.child("announcements")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val announcements = mutableListOf<AppAnnouncement>()
                for (child in snapshot.children) {
                    val announcement = child.getValue(AppAnnouncement::class.java)
                        ?.copy(announcementId = child.key ?: "")
                    if (announcement != null && announcement.isActive) {
                        // Show if targetAudience is "all" or matches user role
                        if (announcement.targetAudience == "all" ||
                            announcement.targetAudience == targetAudience) {
                            announcements.add(announcement)
                        }
                    }
                }
                announcements.sortByDescending { it.createdAt }
                callback(Result.success(announcements))
            }
            override fun onCancelled(error: DatabaseError) {
                callback(Result.failure(error.toException()))
            }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun markNotificationAsRead(
        uid: String,
        notificationId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        db.child("notifications")
            .child(uid)
            .child(notificationId)
            .child("isRead")
            .setValue(true)
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun markAllNotificationsAsRead(
        uid: String,
        notifications: List<AppNotification>,
        callback: (Result<Unit>) -> Unit
    ) {
        if (notifications.isEmpty()) {
            callback(Result.success(Unit))
            return
        }

        val updates = hashMapOf<String, Any>()
        notifications
            .filter { !it.isRead }
            .forEach { notification ->
                updates["/notifications/$uid/${notification.notificationId}/isRead"] = true
            }

        if (updates.isEmpty()) {
            // All already read
            callback(Result.success(Unit))
            return
        }

        db.updateChildren(updates)
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getUnreadNotificationCount(
        uid: String,
        callback: (Int) -> Unit
    ): ValueEventListener {
        val ref = db.child("notifications").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (child in snapshot.children) {
                    val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                    if (!isRead) count++
                }
                callback(count)
            }
            override fun onCancelled(error: DatabaseError) {
                callback(0)
            }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun removeNotificationListener(listener: ValueEventListener) {
        // Remove listener from notifications path
        // uid not needed — Firebase handles listener removal by reference
        db.child("notifications").removeEventListener(listener)
    }

    fun removeAnnouncementListener(listener: ValueEventListener) {
        db.child("announcements").removeEventListener(listener)
    }

    fun removeUnreadCountListener(listener: ValueEventListener) {
        db.child("notifications").removeEventListener(listener)
    }


    // ==========================================
    // REVIEWS & RATINGS (Phase 4a)
    // ==========================================

    fun saveReview(review: com.example.neuronexus.patient.models.Review, callback: (Result<String>) -> Unit) {
        // Dynamically route to "doctors" or "labs" node
        val baseNode = if (review.reviewedEntityType == "doctor") "doctors" else "labs"
        val entityId = review.reviewedEntityId

        val reviewRef = db.child("reviews").child(entityId).push()
        val reviewId = reviewRef.key ?: java.util.UUID.randomUUID().toString()
        val finalReview = review.copy(reviewId = reviewId)

        // Step 1: Fetch current rating and count
        db.child(baseNode).child(entityId).get()
            .addOnSuccessListener { snapshot ->
                // Use Double for rating to safely cover both Float(Doctor) and Double(Lab)
                val currentRating = snapshot.child("rating").getValue(Double::class.java) ?: 0.0
                val currentCount = snapshot.child("reviewCount").getValue(Int::class.java) ?: 0

                // Step 2: Calculate new average
                val newCount = currentCount + 1
                val newRating = ((currentRating * currentCount) + finalReview.rating.toDouble()) / newCount

                // Step 3: Atomic multi-path write
                val updates = hashMapOf<String, Any>(
                    "/reviews/$entityId/$reviewId" to finalReview,
                    "/$baseNode/$entityId/rating" to newRating,
                    "/$baseNode/$entityId/reviewCount" to newCount
                )

                db.updateChildren(updates)
                    .addOnSuccessListener { callback(Result.success(reviewId)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun checkReviewExists(
        appointmentId: String,
        entityId: String,
        patientUid: String,
        callback: (Boolean) -> Unit
    ) {

        db.child("reviews").child(entityId)
            .get()
            .addOnSuccessListener { snapshot ->
                var found = false
                for (child in snapshot.children) {
                    val reviewAppointmentId = child.child("appointmentId").getValue(String::class.java) ?: ""
                    val reviewerId = child.child("reviewerId").getValue(String::class.java) ?: ""

                    if (reviewAppointmentId == appointmentId && reviewerId == patientUid) {
                        found = true
                        break
                    }
                }
                callback(found)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun getReviewsForEntity(entityId: String, callback: (Result<List<com.example.neuronexus.patient.models.Review>>) -> Unit) {
        db.child("reviews").child(entityId).get()
            .addOnSuccessListener { snapshot ->
                val reviews = mutableListOf<com.example.neuronexus.patient.models.Review>()
                for (child in snapshot.children) {
                    val review = child.getValue(com.example.neuronexus.patient.models.Review::class.java)
                        ?.copy(reviewId = child.key ?: "")
                    if (review != null) reviews.add(review)
                }
                reviews.sortByDescending { it.createdAt }
                callback(Result.success(reviews))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    // ==========================================
    // COMPLAINT FUNCTIONS
    // ==========================================

    fun saveComplaint(
        complaint: com.example.neuronexus.patient.models.Complaint,
        callback: (Result<String>) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(Result.failure(Exception("User not authenticated")))
            return
        }

        // Validate required fields
        if (complaint.subject.isBlank()) {
            callback(Result.failure(Exception("Subject is required")))
            return
        }
        if (complaint.description.isBlank()) {
            callback(Result.failure(Exception("Description is required")))
            return
        }
        if (complaint.category.isBlank()) {
            callback(Result.failure(Exception("Category is required")))
            return
        }

        // Generate complaint ID
        val complaintRef = db.child("complaints").push()
        val complaintId = complaintRef.key ?: java.util.UUID.randomUUID().toString()

        val complaintWithId = complaint.copy(
            complaintId = complaintId,
            submittedByUid = currentUser.uid
        )

        complaintRef.setValue(complaintWithId)
            .addOnSuccessListener {
                callback(Result.success(complaintId))
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }

    // ==========================================
    // DOCTOR CATEGORIES
    // ==========================================
    fun getDoctorCategories(callback: (Result<List<String>>) -> Unit) {
        db.child("doctor_categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = mutableListOf<String>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java)
                        ?: child.getValue(String::class.java)

                    if (!name.isNullOrBlank()) {
                        categories.add(name)
                    }
                }
                categories.sort() // alphabetical order
                callback(Result.success(categories))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    ////////////////////////////
    //HELPER

    private fun uploadImage(
        storageRef: com.google.firebase.storage.StorageReference,
        uri: Uri?,
        onComplete: (String) -> Unit
    ) {
        if (uri == null) {
            onComplete("")
            return
        }
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                onComplete("")
            }
    }
}