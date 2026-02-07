package com.example.neuronexus.common.repository

import android.net.Uri
import com.example.neuronexus.common.models.User
import com.example.neuronexus.models.Doctor
import com.example.neuronexus.models.Patient
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import com.example.neuronexus.patient.models.LabTestBooking
import com.example.neuronexus.patient.models.PatientProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
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
    fun getBookedSlots(doctorId: String, dateString: String, callback: (Result<List<String>>) -> Unit) {
        db.child("appointments").orderByChild("doctorId").equalTo(doctorId).get()
            .addOnSuccessListener { snapshot ->
                val bookedSlots = mutableListOf<String>()
                for (child in snapshot.children) {
                    try {
                        val appointment = child.getValue(DoctorAppointment::class.java)
                        if (appointment != null && appointment.appointmentDate == dateString) {
                            bookedSlots.add(appointment.appointmentTime)
                        }
                    } catch (e: Exception) {
                        // Ignore parse errors
                    }
                }
                callback(Result.success(bookedSlots))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 9. PATIENT PROFILE MANAGEMENT
    // ----------------------------------------------------------------
    fun getPatientProfiles(accountHolderId: String, callback: (Result<List<PatientProfile>>) -> Unit) {
        db.child("users").child(accountHolderId).child("patient_profiles").get()
            .addOnSuccessListener { snapshot ->
                val profiles = mutableListOf<PatientProfile>()
                for (child in snapshot.children) {
                    val profile = child.getValue(PatientProfile::class.java)
                    if (profile != null) {
                        profiles.add(profile)
                    }
                }

                val uniqueProfiles = profiles.distinctBy {
                    "${it.fullName.trim().lowercase()}|${it.relation.trim().lowercase()}"
                }

                callback(Result.success(uniqueProfiles))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 10. BOOKING (SAVE)
    // ----------------------------------------------------------------
    fun saveBooking(booking: Booking, callback: (Result<String>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(Result.failure(Exception("User not authenticated")))
            return
        }

        // Prepare Patient Profile ID
        val profile = booking.patientInfo
        val profileId = if (profile.profileId.isEmpty()) {
            db.child("users").child(currentUser.uid).child("patient_profiles").push().key
                ?: java.util.UUID.randomUUID().toString()
        } else {
            profile.profileId
        }

        val emailToSave = if (profile.email.isEmpty() && currentUser.email != null) currentUser.email else profile.email

        val finalProfile = profile.copy(
            profileId = profileId,
            accountHolderId = currentUser.uid,
            email = emailToSave ?: ""
        )

        val bookingId = if (booking.bookingId.isEmpty()) {
            db.child("appointments").push().key ?: java.util.UUID.randomUUID().toString()
        } else {
            booking.bookingId
        }

        val dummyPayment = booking.payment.copy(
            paymentId = "PAY_DUMMY_${System.currentTimeMillis()}",
            amount = booking.payment.amount,
            paymentStatus = "pending",
            paymentMethod = "PAY_AT_CLINIC",
            transactionDate = System.currentTimeMillis()
        )

        val finalBooking = when (booking) {
            is DoctorAppointment -> {
                booking.copy(
                    bookingId = bookingId,
                    accountHolderId = currentUser.uid,
                    patientProfileId = profileId,
                    patientInfo = finalProfile,
                    payment = dummyPayment,
                    status = "created",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            is LabTestBooking -> {
                booking.copy(
                    bookingId = bookingId,
                    accountHolderId = currentUser.uid,
                    patientProfileId = profileId,
                    patientInfo = finalProfile,
                    payment = dummyPayment,
                    status = "created",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            else -> booking
        }

        val updates = hashMapOf<String, Any>()
        updates["/users/${currentUser.uid}/patient_profiles/$profileId"] = finalProfile
        updates["/appointments/$bookingId"] = finalBooking

        db.updateChildren(updates)
            .addOnSuccessListener {
                callback(Result.success(bookingId))
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
                        val appointment = child.getValue(DoctorAppointment::class.java)
                        if (appointment != null) {
                            appointments.add(appointment)
                        }
                        // Note: For labs, we might need a polymorphic check here later,
                        // but focusing on existing logic preservation.
                    } catch (e: Exception) {
                    }
                }
                callback(Result.success(appointments))
            }
            .addOnFailureListener {
                callback(Result.failure(it))
            }
    }

    // ----------------------------------------------------------------
    // 12. FETCH SINGLE DOCTOR
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