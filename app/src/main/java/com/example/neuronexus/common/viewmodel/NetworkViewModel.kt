package com.example.neuronexus.common.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.common.models.AppAnnouncement
import com.example.neuronexus.common.models.AppNotification
import com.example.neuronexus.common.models.User
import com.example.neuronexus.common.repository.AppRepository
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.models.Patient
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import com.example.neuronexus.patient.models.LabTestBooking
import com.example.neuronexus.patient.models.InstallmentPlan
import com.example.neuronexus.patient.models.InstallmentRecord
import com.example.neuronexus.patient.models.LabReport
import com.example.neuronexus.patient.models.PatientProfile
import com.example.neuronexus.patient.models.Prescription
import com.example.neuronexus.patient.models.Review
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class NetworkViewModel(private val repository: AppRepository) : ViewModel() {

    // 1. Doctor List State
    private val _doctorsList = MutableLiveData<Result<List<Doctor>>>()
    val doctorsList: LiveData<Result<List<Doctor>>> = _doctorsList

    // 2. Booking Result State (Reused for both Doctor & Lab to maintain consistency)
    private val _bookingResult = MutableLiveData<Result<String>?>()
    val bookingResult: LiveData<Result<String>?> = _bookingResult

    // 3. Patient Appointments State
    private val _myAppointments = MutableLiveData<Result<List<Booking>>>()
    val myAppointments: LiveData<Result<List<Booking>>> = _myAppointments

    // 3.5 Doctor Appointments State (NEW)
    private val _doctorAppointments = MutableLiveData<Result<List<DoctorAppointment>>>()
    val doctorAppointments: LiveData<Result<List<DoctorAppointment>>> = _doctorAppointments

    // 4. Single Doctor Details
    private val _doctorDetails = MutableLiveData<Result<Doctor>>()
    val doctorDetails: LiveData<Result<Doctor>> = _doctorDetails

    // 5. User Details (Legacy - For Auth)
    private val _userDetails = MutableLiveData<Result<User>>()
    val userDetails: LiveData<Result<User>> = _userDetails

    // 6. Patient Details (For Dashboard/Profile)
    private val _patientDetails = MutableLiveData<Result<Patient>>()
    val patientDetails: LiveData<Result<Patient>> = _patientDetails

    // 7. Profile Update Result
    private val _profileUpdateResult = MutableLiveData<Result<String>?>()
    val profileUpdateResult: LiveData<Result<String>?> = _profileUpdateResult

    // 8. Image Update Result
    private val _imageUpdateResult = MutableLiveData<Result<String>?>()
    val imageUpdateResult: LiveData<Result<String>?> = _imageUpdateResult

    // 9. Lab Lists (NEW)
    private val _labsList = MutableLiveData<Result<List<Lab>>>()
    val labsList: LiveData<Result<List<Lab>>> = _labsList

    // 10. Lab Tests List
    private val _labTests = MutableLiveData<Result<List<LabTest>>>()
    val labTests: LiveData<Result<List<LabTest>>> = _labTests

    // 11. Single Lab Details
    private val _labDetails = MutableLiveData<Result<Lab>>()
    val labDetails: LiveData<Result<Lab>> = _labDetails

    // 12. Single Lab Test Details
    private val _labTestDetails = MutableLiveData<Result<LabTest>>()
    val labTestDetails: LiveData<Result<LabTest>> = _labTestDetails

    // Prescription State
    private val _prescriptionResult = MutableLiveData<Result<Prescription>?>()
    val prescriptionResult: LiveData<Result<Prescription>?> = _prescriptionResult

    // ----------------------------------------------------------------
    // SINGLE BOOKING FETCH STATE
    // ----------------------------------------------------------------
    private val _bookingByIdResult = MutableLiveData<Result<Booking?>?>()
    val bookingByIdResult: LiveData<Result<Booking?>?> = _bookingByIdResult

    // ==========================================
    // MEDICAL RECORDS LIVEDATA
    // ==========================================

    private val _prescriptionsResult = MutableLiveData<Result<List<Prescription>>?>()
    val prescriptionsResult: LiveData<Result<List<Prescription>>?> = _prescriptionsResult

    private val _labReportsResult = MutableLiveData<Result<List<LabReport>>?>()
    val labReportsResult: LiveData<Result<List<LabReport>>?> = _labReportsResult

    // ----------------------------------------------------------------
    // SINGLE LAB REPORT FETCH STATE
    // ----------------------------------------------------------------
    private val _labReportResult = MutableLiveData<Result<LabReport?>?>()
    val labReportResult: LiveData<Result<LabReport?>?> = _labReportResult

    // 13. Save Patient Profile Result for booking, its not main user account
    private val _saveProfileResult = MutableLiveData<Result<PatientProfile>?>()
    val saveProfileResult: LiveData<Result<PatientProfile>?> = _saveProfileResult

    // 14. Save Prescription Result
    private val _savePrescriptionResult = MutableLiveData<Result<String>?>()
    val savePrescriptionResult: LiveData<Result<String>?> = _savePrescriptionResult

    // ==========================================
    // NOTIFICATION LIVEDATA & LISTENERS
    // ==========================================

    // Notification LiveData
    private val _notifications = MutableLiveData<Result<List<AppNotification>>?>()
    val notifications: LiveData<Result<List<AppNotification>>?> = _notifications

    private val _announcements = MutableLiveData<Result<List<AppAnnouncement>>?>()
    val announcements: LiveData<Result<List<AppAnnouncement>>?> = _announcements

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _markReadResult = MutableLiveData<Result<Unit>?>()
    val markReadResult: LiveData<Result<Unit>?> = _markReadResult

    private val _doctorCategories = MutableLiveData<Result<List<String>>?>()
    val doctorCategories: LiveData<Result<List<String>>?> = _doctorCategories

    // Store listeners for cleanup in onCleared()
    private var notificationsListener: ValueEventListener? = null
    private var announcementsListener: ValueEventListener? = null
    private var unreadCountListener: ValueEventListener? = null

    // ----------------------------------------------------------------
    // REVIEWS STATE
    // ----------------------------------------------------------------
    private val _saveReviewResult = MutableLiveData<Result<String>?>()
    val saveReviewResult: LiveData<Result<String>?> = _saveReviewResult

    private val _reviewExistsResult = MutableLiveData<Boolean?>()
    val reviewExistsResult: LiveData<Boolean?> = _reviewExistsResult

    private val _entityReviews = MutableLiveData<Result<List<Review>>?>()
    val entityReviews: LiveData<Result<List<Review>>?> = _entityReviews

    // ----------------------------------------------------------------
    // COMPLAINTS STATE
    // ----------------------------------------------------------------
    private val _saveComplaintResult = MutableLiveData<Result<String>?>()
    val saveComplaintResult: LiveData<Result<String>?> = _saveComplaintResult

    // ================================================================
    // SINGLE DOCTOR APPOINTMENT FETCH STATE
    // ================================================================
    private val _doctorAppointmentByIdResult = MutableLiveData<Result<DoctorAppointment?>?>()
    val doctorAppointmentByIdResult: LiveData<Result<DoctorAppointment?>?> = _doctorAppointmentByIdResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Fetch Doctors
    fun fetchAllDoctors() {
        _loading.value = true
        repository.getAllApprovedDoctors { result ->
            _loading.value = false
            _doctorsList.value = result
        }
    }

    // Book Appointment (EXISTING DOCTOR FLOW - UNTOUCHED)
    fun bookAppointment(booking: Booking) {
        _loading.value = true
        repository.saveBooking(booking) { result ->
            _loading.value = false
            _bookingResult.value = result
        }
    }

    // Fetch Patient Appointments (Doctor and Labs both)
    fun fetchMyAppointments() {
        _loading.value = true
        val uid = repository.getCurrentUserUid()

        if (uid != null) {
            repository.getMyAppointments(uid) { result ->
                _loading.value = false

                result.onSuccess { unifiedBookings ->
                    _myAppointments.value = Result.success(unifiedBookings)
                }.onFailure { error ->
                    _myAppointments.value = Result.failure(error)
                }
            }
        } else {
            _loading.value = false
            _myAppointments.value = Result.failure(Exception("User auth token missing or expired."))
        }
    }

    // Fetch Doctor's Own Appointments
    fun fetchDoctorAppointments() {
        _loading.value = true
        val uid = repository.getCurrentUserUid()

        if (uid != null) {
            repository.getDoctorAppointments(uid) { result ->
                _loading.value = false
                _doctorAppointments.value = result
            }
        } else {
            _loading.value = false
            _doctorAppointments.value = Result.failure(Exception("User auth token missing or expired."))
        }
    }

    fun updateDoctorAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        accountHolderId: String,
        patientName: String,
        doctorName: String
    ) {
        _loading.value = true
        repository.updateAppointmentStatus(appointmentId, newStatus, accountHolderId, patientName, doctorName) { result ->
            _loading.value = false
            result.onSuccess { status ->
                _bookingResult.value = Result.success(status)
                // Silently refresh the list in the background
                fetchDoctorAppointments()
            }.onFailure { error ->
                _bookingResult.value = Result.failure(error)
            }
        }
    }

    fun incrementNoShowCount(patientUid: String) {
        // Fire-and-forget background operation - no loading state
        repository.incrementNoShowCount(patientUid) { result ->
            result.onSuccess {
                Log.d("NetworkViewModel", "No-show count incremented successfully for $patientUid")
            }.onFailure { error ->
                Log.e("NetworkViewModel", "Failed to increment no-show count for $patientUid", error)
            }
        }
    }

    // Fetch Single Doctor Details
    fun fetchDoctorDetails(uid: String) {
        _loading.value = true
        repository.getDoctor(uid) { result ->
            _loading.value = false
            _doctorDetails.value = result
        }
    }

    fun getCurrentUserUid(): String? {
        return repository.getCurrentUserUid()
    }

    // Fetch Current User Details (Auth)
    fun fetchUserDetails() {
        val uid = repository.getCurrentUserUid()
        if (uid != null) {
            repository.getUser(uid, object : AppRepository.LoginCallback {
                override fun onSuccess(user: User) {
                    _userDetails.value = Result.success(user)
                }
                override fun onError(message: String) {
                    _userDetails.value = Result.failure(Exception(message))
                }
            })
        }
    }

    // Fetch Patient Details
    fun fetchPatientDetails() {
        val uid = repository.getCurrentUserUid()
        if (uid != null) {
            repository.getPatient(uid) { result ->
                _patientDetails.value = result
            }
        }
    }

    fun fetchPrescription(patientProfileId: String, prescriptionId: String) {
        _loading.value = true
        repository.getPrescription(patientProfileId, prescriptionId) { result ->
            _loading.value = false
            _prescriptionResult.value = result
        }
    }

    fun fetchBookingById(bookingId: String) {
        _loading.value = true
        repository.getBookingById(bookingId) { result ->
            _loading.postValue(false)
            _bookingByIdResult.postValue(result)
        }
    }

    fun fetchDoctorAppointmentById(appointmentId: String) {
        _loading.value = true
        repository.getDoctorAppointmentById(appointmentId) { result ->
            _loading.postValue(false)
            _doctorAppointmentByIdResult.postValue(result)
        }
    }


    fun savePrescription(prescription: Prescription) {
        _loading.value = true
        repository.savePrescription(prescription) { result ->
            _loading.value = false
            _savePrescriptionResult.value = result
        }
    }


    // Update User profile (Patient)
    fun saveProfileChanges(name: String, phone: String, imageUri: Uri?) {
        val uid = repository.getCurrentUserUid() ?: return
        _loading.value = true

        if (imageUri != null) {
            repository.uploadPatientImage(uid, imageUri) { uploadResult ->
                uploadResult.onSuccess { downloadUrl ->
                    val updates = mapOf(
                        "name" to name,
                        "phone" to phone,
                        "profileImageUrl" to downloadUrl
                    )
                    performDatabaseUpdate(uid, updates)
                }.onFailure {
                    _loading.value = false
                    _profileUpdateResult.value = Result.failure(it)
                }
            }
        } else {
            val updates = mapOf(
                "name" to name,
                "phone" to phone
            )
            performDatabaseUpdate(uid, updates)
        }
    }

    fun saveDoctorProfileChanges(
        name: String,
        phone: String,
        address: String,
        qualification: String,
        specialization: String,
        fee: String,
        schedule: String,
        imageUri: android.net.Uri?
    ) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _profileUpdateResult.value = Result.failure(Exception("User not authenticated."))
            return
        }

        _loading.value = true

        // 1. Construct Map safely - only include non-empty fields
        val updates = mutableMapOf<String, Any>()
        if (name.isNotEmpty()) updates["name"] = name
        if (phone.isNotEmpty()) updates["phone"] = phone
        if (address.isNotEmpty()) updates["clinicAddress"] = address
        if (qualification.isNotEmpty()) updates["qualification"] = qualification
        if (specialization.isNotEmpty()) updates["specialization"] = specialization
        if (fee.isNotEmpty()) updates["consultationFee"] = fee
        if (schedule.isNotEmpty()) updates["schedule"] = schedule

        // 2. Sequence operations: Image upload first (if exists), then Database update
        if (imageUri != null) {
            repository.uploadDoctorProfileImage(imageUri, uid) { imageResult ->
                imageResult.onSuccess { downloadUrl ->
                    updates["profileImageUrl"] = downloadUrl

                    // Proceed to DB update after successful image upload
                    repository.updateDoctorProfile(uid, updates) { dbResult ->
                        _loading.value = false
                        dbResult.onSuccess {
                            _profileUpdateResult.value = Result.success("Profile updated successfully")
                        }.onFailure { error ->
                            _profileUpdateResult.value = Result.failure(error)
                        }
                    }
                }.onFailure { error ->
                    // Fail early if image upload fails
                    _loading.value = false
                    _profileUpdateResult.value = Result.failure(error)
                }
            }
        } else {
            // Skip image upload, go straight to DB update
            repository.updateDoctorProfile(uid, updates) { dbResult ->
                _loading.value = false
                dbResult.onSuccess {
                    _profileUpdateResult.value = Result.success("Profile updated successfully")
                }.onFailure { error ->
                    _profileUpdateResult.value = Result.failure(error)
                }
            }
        }
    }

     // patient profile for booking, its not main user profile
    fun savePatientProfile(accountHolderId: String, profile: PatientProfile) {
        _loading.value = true
        repository.savePatientProfile(accountHolderId, profile) { result ->
            _loading.value = false
            _saveProfileResult.value = result
        }
    }

    private fun performDatabaseUpdate(uid: String, updates: Map<String, Any>) {
        repository.updatePatientProfile(uid, updates) { result ->
            _loading.value = false
            _profileUpdateResult.value = result

            if (result.isSuccess) {
                fetchPatientDetails()
            }
        }
    }

    // ----------------------------------------------------------------
    // LAB LOGIC
    // ----------------------------------------------------------------

    fun fetchAllLabs() {
        _loading.value = true
        repository.getAllLabs { result ->
            _loading.value = false
            _labsList.value = result
        }
    }

    // Fetch Single Lab Details
    fun fetchLabDetails(uid: String) {
        _loading.value = true
        repository.getLab(uid) { result ->
            _loading.value = false
            _labDetails.value = result
        }
    }

    fun fetchTestsForLab(labId: String) {
        _loading.value = true
        repository.getTestsForLab(labId) { result ->
            _loading.value = false
            _labTests.value = result
        }
    }

    fun fetchLabTestDetails(testId: String) {
        _loading.value = true
        repository.getLabTest(testId) { result ->
            _loading.value = false
            _labTestDetails.value = result
        }
    }


    // ================================================================
    // NEW: LAB TEST BOOKING (ORCHESTRATES INSTALLMENTS)
    // ================================================================

    /**
     * Handles lab test bookings, generating installment plan contracts
     * and ledger records if the user opted for partial payments.
     */
    fun bookLabTest(
        booking: LabTestBooking,
        isInstallment: Boolean = false,
        totalAmount: Double = 0.0,
        numInstallments: Int = 0
    ) {
        _loading.value = true

        // 1. Ensure booking has an ID so we can establish cross-linking foreign keys immediately
        val finalBookingId = if (booking.bookingId.isEmpty()) {
            UUID.randomUUID().toString()
        } else {
            booking.bookingId
        }
        val finalBooking = booking.copy(bookingId = finalBookingId)

        var plan: InstallmentPlan? = null
        val records = mutableListOf<InstallmentRecord>()

        // 2. Orchestrate business logic for Installment Plan Generation
        if (isInstallment && numInstallments > 0) {
            val planId = UUID.randomUUID().toString()
            val installmentAmount = totalAmount / numInstallments
            val currentTime = System.currentTimeMillis()

            // Assuming a standard 30-day billing cycle for subsequent installments
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000

            // Master Contract
            plan = InstallmentPlan(
                planId = planId,
                bookingId = finalBookingId,
                totalAmount = totalAmount,
                numInstallments = numInstallments,
                installmentAmount = installmentAmount,
                startDate = currentTime,
                status = "active"
            )

            // Individual Ledger Slots
            for (i in 1..numInstallments) {
                records.add(
                    InstallmentRecord(
                        recordId = UUID.randomUUID().toString(),
                        planId = planId,
                        installmentNumber = i,
                        dueDate = currentTime + ((i - 1) * thirtyDaysInMillis),
                        amount = installmentAmount,
                        // Defaults to pending. Repository will intercept and mark the 1st as "paid" atomically.
                        status = "pending"
                    )
                )
            }
        }

        // 3. Delegate to Repository for Firebase Atomic Batch Write
        repository.saveBooking(
            booking = finalBooking,
            installmentPlan = plan,
            installmentRecords = records
        ) { result ->
            _loading.value = false
            // Reusing existing _bookingResult so UI navigation logic stays exactly the same
            _bookingResult.value = result
        }
    }

    // ================================================================
    // CANCEL BOOKING
    // ================================================================
    fun cancelBooking(booking: Booking) {
        _loading.value = true
        repository.cancelBooking(booking) { result ->
            _loading.value = false
            _bookingResult.value = result
        }
    }

    fun checkAndExpirePendingAppointments() {
        val uid = repository.getCurrentUserUid() ?: return
        repository.checkAndExpirePendingAppointments(uid) { result ->
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                if (count > 0) {
                    // Refresh appointments after expiry
                    fetchMyAppointments()
                }
            }
            // Silent failure — expiry check is background operation
        }
    }

    // ==========================================
    // NOTIFICATION FUNCTIONS
    // ==========================================

    fun startListeningToNotifications(uid: String) {
        // Remove existing listener before adding new one — prevents duplicates
        stopListeningToNotifications()
        notificationsListener = repository.getNotifications(uid) { result ->
            _notifications.postValue(result)
        }
    }

    fun stopListeningToNotifications() {
        notificationsListener?.let {
            repository.removeNotificationListener(it)
            notificationsListener = null
        }
    }

    fun startListeningToAnnouncements(targetAudience: String) {
        stopListeningToAnnouncements()
        announcementsListener = repository.getAnnouncements(targetAudience) { result ->
            _announcements.postValue(result)
        }
    }

    fun stopListeningToAnnouncements() {
        announcementsListener?.let {
            repository.removeAnnouncementListener(it)
            announcementsListener = null
        }
    }

    fun startListeningToUnreadCount(uid: String) {
        stopListeningToUnreadCount()
        unreadCountListener = repository.getUnreadNotificationCount(uid) { count ->
            _unreadCount.postValue(count)
        }
    }

    fun stopListeningToUnreadCount() {
        unreadCountListener?.let {
            repository.removeUnreadCountListener(it)
            unreadCountListener = null
        }
    }

    fun markNotificationAsRead(uid: String, notificationId: String) {
        repository.markNotificationAsRead(uid, notificationId) { result ->
            // Silent — real-time listener will update the list automatically
            // Only post failure if needed for error handling
            if (result.isFailure) {
                _markReadResult.postValue(result)
            }
        }
    }

    fun markAllNotificationsAsRead(uid: String, notifications: List<AppNotification>) {
        repository.markAllNotificationsAsRead(uid, notifications) { result ->
            _markReadResult.postValue(result)
        }
    }

    // ==========================================
    // MEDICAL RECORDS FUNCTIONS
    // ==========================================

    fun fetchPatientPrescriptions(patientProfileId: String) {
        _loading.value = true
        repository.getPatientPrescriptions(patientProfileId) { result ->
            // Use postValue to safely update from Firebase background thread
            _loading.postValue(false)
            _prescriptionsResult.postValue(result)
        }
    }

    fun fetchPatientLabReports(patientProfileId: String) {
        _loading.value = true
        repository.getPatientLabReports(patientProfileId) { result ->
            // Use postValue to safely update from Firebase background thread
            _loading.postValue(false)
            _labReportsResult.postValue(result)
        }
    }
    //single report
    fun fetchLabReport(patientProfileId: String, reportId: String) {
        _loading.value = true
        repository.getLabReport(patientProfileId, reportId) { result ->
            _loading.postValue(false)
            _labReportResult.postValue(result)
        }
    }


    // Review
    fun saveReview(review: com.example.neuronexus.patient.models.Review) {
        _loading.value = true
        repository.saveReview(review) { result ->
            _loading.postValue(false)
            _saveReviewResult.postValue(result)
        }
    }

    fun checkReviewExists(appointmentId: String, entityId: String, patientUid: String) {
        repository.checkReviewExists(appointmentId, entityId, patientUid) { exists ->
            _reviewExistsResult.postValue(exists)
        }
    }

    fun fetchEntityReviews(entityId: String) {
        _loading.value = true
        repository.getReviewsForEntity(entityId) { result ->
            _loading.postValue(false)
            _entityReviews.postValue(result)
        }
    }

    //complains
    fun saveComplaint(complaint: com.example.neuronexus.patient.models.Complaint) {
        _loading.value = true
        repository.saveComplaint(complaint) { result ->
            _loading.postValue(false)
            _saveComplaintResult.postValue(result)
        }
    }

    fun fetchDoctorCategories() {
        repository.getDoctorCategories() { result ->
            _doctorCategories.postValue(result)
        }
    }

    fun resetDoctorCategories() {
        _doctorCategories.value = null
    }

    fun resetSaveComplaintResult() {
        _saveComplaintResult.value = null
    }

    fun resetSaveReviewResult()
    {
        _saveReviewResult.value = null
    }
    fun resetReviewExistsResult()
    {
        _reviewExistsResult.value = null
    }
    fun resetEntityReviews()
    {
        _entityReviews.value = null
    }

    fun resetPrescriptionsResult()
    {
        _prescriptionsResult.value = null
    }

    fun resetLabReportsResult()
    {
        _labReportsResult.value = null
    }

    fun resetLabReportResult()
    {
        _labReportResult.value = null
    }

    fun resetMarkReadState() {
        _markReadResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningToNotifications()
        stopListeningToAnnouncements()
        stopListeningToUnreadCount()
    }

    // Helper to reset states
    fun resetBookingState() {
        _bookingResult.value = null
    }


    fun resetPrescriptionState() {
        _prescriptionResult.value = null
    }

    fun resetSavePrescriptionState() {
        _savePrescriptionResult.value = null
    }

    fun resetSaveProfileState() {
        _saveProfileResult.value = null
    }


    fun resetProfileUpdateState() {
        _profileUpdateResult.value = null
    }

    fun resetBookingByIdResult() {
        _bookingByIdResult.value = null
    }

    fun resetDoctorAppointmentByIdResult() {
        _doctorAppointmentByIdResult.value = null
    }

    fun resetImageUpdateState() {
        _imageUpdateResult.value = null
    }
}