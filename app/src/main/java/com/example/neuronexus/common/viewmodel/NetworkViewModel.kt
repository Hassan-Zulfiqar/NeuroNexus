package com.example.neuronexus.common.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.common.models.User
import com.example.neuronexus.common.repository.AppRepository
import com.example.neuronexus.models.Doctor
import com.example.neuronexus.models.Patient
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest

class NetworkViewModel(private val repository: AppRepository) : ViewModel() {

    // 1. Doctor List State
    private val _doctorsList = MutableLiveData<Result<List<Doctor>>>()
    val doctorsList: LiveData<Result<List<Doctor>>> = _doctorsList

    // 2. Booking Result State
    private val _bookingResult = MutableLiveData<Result<String>?>()
    val bookingResult: LiveData<Result<String>?> = _bookingResult

    // 3. Patient Appointments State
    private val _myAppointments = MutableLiveData<Result<List<Booking>>>()
    val myAppointments: LiveData<Result<List<Booking>>> = _myAppointments

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

    // 10. Lab Tests List (NEW)
    private val _labTests = MutableLiveData<Result<List<LabTest>>>()
    val labTests: LiveData<Result<List<LabTest>>> = _labTests

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

    // Book Appointment
    fun bookAppointment(booking: Booking) {
        _loading.value = true
        repository.saveBooking(booking) { result ->
            _loading.value = false
            _bookingResult.value = result
        }
    }

    // Fetch Patient Appointments
    fun fetchMyAppointments() {
        _loading.value = true
        val uid = repository.getCurrentUserUid()
        if (uid != null) {
            repository.getMyAppointments(uid) { result ->
                _loading.value = false
                _myAppointments.value = result
            }
        } else {
            _loading.value = false
            _myAppointments.value = Result.failure(Exception("User not logged in"))
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

    // Update Patient Profile
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

    fun fetchTestsForLab(labId: String) {
        _loading.value = true
        repository.getTestsForLab(labId) { result ->
            _loading.value = false
            _labTests.value = result
        }
    }

    // Helper to reset states
    fun resetBookingState() {
        _bookingResult.value = null
    }

    fun resetProfileUpdateState() {
        _profileUpdateResult.value = null
    }

    fun resetImageUpdateState() {
        _imageUpdateResult.value = null
    }
}