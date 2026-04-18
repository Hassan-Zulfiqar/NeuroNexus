package com.example.neuronexus.common.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.common.repository.AppRepository
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import com.example.neuronexus.patient.models.PatientProfile
import com.example.neuronexus.patient.model.TimeSlot
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.utils.ScheduleParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SharedViewModel(private val repository: AppRepository? = null) : ViewModel() {

    // 1. Selected Doctor
    private val _selectedDoctor = MutableLiveData<Doctor?>()
    val selectedDoctor: LiveData<Doctor?> = _selectedDoctor

    // 2. Booking Data
    private val _selectedDate = MutableLiveData<Long?>()
    val selectedDate: LiveData<Long?> = _selectedDate

    private val _selectedTimeSlot = MutableLiveData<String?>()
    val selectedTimeSlot: LiveData<String?> = _selectedTimeSlot

    // 3. Generated Time Slots (List handles empty state natively)
    private val _availableTimeSlots = MutableLiveData<List<TimeSlot>>()
    val availableTimeSlots: LiveData<List<TimeSlot>> = _availableTimeSlots

    // 4. Patient Profile State (List handles empty state natively)
    private val _patientProfiles = MutableLiveData<List<PatientProfile>>()
    val patientProfiles: LiveData<List<PatientProfile>> = _patientProfiles

    private val _selectedPatientProfile = MutableLiveData<PatientProfile?>()
    val selectedPatientProfile: LiveData<PatientProfile?> = _selectedPatientProfile

    // 5. Reason for Visit (New)
    private val _bookingReason = MutableLiveData<String?>()
    val bookingReason: LiveData<String?> = _bookingReason

    // 6. Lab Selection (NEW ADDITION)
    private val _selectedLab = MutableLiveData<Lab?>()
    val selectedLab: LiveData<Lab?> = _selectedLab

    private val _selectedLabTest = MutableLiveData<LabTest?>()
    val selectedLabTest: LiveData<LabTest?> = _selectedLabTest

    // 7. Previous Booking Reference (For Rebooking)
    private val _previousBookingId = MutableLiveData<String?>()
    val previousBookingId: LiveData<String?> = _previousBookingId

    // 8. Doctor Appointment Selection (Doctor Dashboard)
    private val _selectedDoctorAppointment = MutableLiveData<DoctorAppointment?>()
    val selectedDoctorAppointment: LiveData<DoctorAppointment?> = _selectedDoctorAppointment

    // ----------------------------------------------------------------
    // PATIENT BOOKING SELECTION (Handles both Doctor & Lab bookings)
    // ----------------------------------------------------------------
    private val _selectedPatientBooking = MutableLiveData<Booking?>()
    val selectedPatientBooking: LiveData<Booking?> = _selectedPatientBooking

    // 9. Medical Records Tab State (0 = Prescriptions, 1 = Lab Reports)
    private val _selectedMedicalRecordTab = MutableLiveData<Int>()
    val selectedMedicalRecordTab: LiveData<Int> = _selectedMedicalRecordTab

    // 10. Symptom Checker State
    private val _symptomScore = MutableLiveData<Int>(0)
    val symptomScore: LiveData<Int> = _symptomScore

    private val _symptomQuestionIndex = MutableLiveData<Int>(0)
    val symptomQuestionIndex: LiveData<Int> = _symptomQuestionIndex

    private val _symptomQuestions = MutableLiveData<List<com.example.neuronexus.common.models.Symptoms>>()
    val symptomQuestions: LiveData<List<com.example.neuronexus.common.models.Symptoms>> = _symptomQuestions

    private val _finalRiskScore = MutableLiveData<Int>(0)
    val finalRiskScore: LiveData<Int> = _finalRiskScore

    // 11. Patient History — status filter
    private val _patientHistoryStatusFilter = MutableLiveData<String>("all")
    val patientHistoryStatusFilter: LiveData<String> = _patientHistoryStatusFilter

    // Setter function
    fun selectMedicalRecordTab(index: Int) {
        _selectedMedicalRecordTab.value = index
    }

    fun setPatientHistoryStatusFilter(status: String) {
        _patientHistoryStatusFilter.value = status.lowercase(Locale.getDefault())
    }

    // Loading State
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // -----------------------------------------------------------
    // Logic Methods
    // -----------------------------------------------------------

    fun selectDoctor(doctor: Doctor) {
        _selectedDoctor.value = doctor
    }

    fun selectDate(timestamp: Long) {
        _selectedDate.value = timestamp

        if (_selectedDoctor.value != null) {
            generateDoctorSlotsForDate(timestamp)
        } else if (_selectedLab.value != null) {
            generateLabSlotsForDate(timestamp)
        } else {
            _availableTimeSlots.value = emptyList()
        }
    }

    fun selectPatientProfile(profile: PatientProfile) {
        _selectedPatientProfile.value = profile
    }

    fun setDraftPatientProfile(profile: PatientProfile) {
        _selectedPatientProfile.value = profile
    }

    fun setBookingReason(reason: String) {
        _bookingReason.value = reason
    }

    fun selectLab(lab: Lab) {
        _selectedLab.value = lab
    }

    fun selectLabTest(test: LabTest) {
        _selectedLabTest.value = test
    }

    fun setPreviousBookingId(bookingId: String?) {
        _previousBookingId.value = bookingId
    }

    fun selectDoctorAppointment(appointment: DoctorAppointment?) {
        _selectedDoctorAppointment.value = appointment
    }

    // -----------------------------------------------------------
    // State Isolation
    // -----------------------------------------------------------

    fun clearBookingState() {
        _selectedDoctor.value = null
        _selectedLab.value = null
        _selectedLabTest.value = null
        _selectedDate.value = null
        _selectedTimeSlot.value = null
        _selectedPatientProfile.value = null
        _bookingReason.value = null
        _previousBookingId.value = null
        _availableTimeSlots.value = emptyList()
        _selectedDoctorAppointment.value = null
        _selectedPatientBooking.value = null
        _selectedMedicalRecordTab.value = 0
    }

    // -----------------------------------------------------------
    // Domain-Specific Slot Generation (Entry Points)
    // -----------------------------------------------------------

    fun generateLabSlotsForDate(dateTimestamp: Long) {
        val lab = _selectedLab.value ?: return

        // 1. Generate all possible mathematical slots from the schedule
        val rawSlots = generateSlotsFromString(
            rawSchedule = lab.labTiming,
            timestamp = dateTimestamp,
            intervalMinutes = 30
        )

        // Null safety for repository
        if (repository == null) {
            _availableTimeSlots.value = rawSlots
            return
        }

        // 2. Format the date precisely as it's saved in Firebase
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateString = sdf.format(Date(dateTimestamp))

        // 3. Initiate async fetch and update loading state
        _loading.value = true
        repository.getBookedSlots(lab.uid, dateString, "lab") { result ->
            _loading.value = false
            result.onSuccess { bookedTimes ->
                // 4. Filter out any slots that already exist in the database
                _availableTimeSlots.value = rawSlots.filterNot { bookedTimes.contains(it.timeLabel) }
            }.onFailure {
                // Fallback: If network fails, show raw slots so user isn't hard-blocked
                _availableTimeSlots.value = rawSlots
            }
        }
    }

    fun generateDoctorSlotsForDate(dateTimestamp: Long) {
        val doctor = _selectedDoctor.value ?: return

        // 1. Generate all possible mathematical slots from the schedule
        val rawSlots = generateSlotsFromString(
            rawSchedule = doctor.schedule,
            timestamp = dateTimestamp,
            intervalMinutes = 30
        )

        // Null safety for repository
        if (repository == null) {
            _availableTimeSlots.value = rawSlots
            return
        }

        // 2. Format the date precisely as it's saved in Firebase
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateString = sdf.format(Date(dateTimestamp))

        // 3. Initiate async fetch and update loading state
        _loading.value = true
        repository.getBookedSlots(doctor.uid, dateString, "doctor") { result ->
            _loading.value = false
            result.onSuccess { bookedTimes ->
                // 4. Filter out any slots that already exist in the database
                _availableTimeSlots.value = rawSlots.filterNot { bookedTimes.contains(it.timeLabel) }
            }.onFailure {
                // Fallback
                _availableTimeSlots.value = rawSlots
            }
        }
    }

    // -----------------------------------------------------------
    // Shared Core Utility for slot generation
    // -----------------------------------------------------------

    private fun generateSlotsFromString(rawSchedule: String, timestamp: Long, intervalMinutes: Int): List<TimeSlot> {
        val parsedSchedule = ScheduleParser.parseSchedule(rawSchedule)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val androidDay = calendar.get(Calendar.DAY_OF_WEEK)
        val ourDayIndex = if (androidDay == Calendar.SUNDAY) 7 else androidDay - 1

        val ranges = parsedSchedule.weeklyAvailability[ourDayIndex]
        val slots = mutableListOf<TimeSlot>()

        if (ranges != null) {
            val nowMinute = getCurrentMinuteOfDay()
            val isToday = isSameDay(timestamp, System.currentTimeMillis())

            for (range in ranges) {
                var currentMinute = range.startMinute
                while (currentMinute < range.endMinute) {
                    if (!isToday || currentMinute > nowMinute) {
                        val timeLabel = formatMinutesToTime(currentMinute)
                        slots.add(TimeSlot(timeLabel, currentMinute, isAvailable = true))
                    }
                    currentMinute += intervalMinutes
                }
            }
        }

        return slots
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
            && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getCurrentMinuteOfDay(): Int {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }

    private fun parseTimeLabelToMinutes(timeLabel: String): Int {
        return try {
            val formatter = SimpleDateFormat("hh:mm aa", Locale.getDefault())
            val time = formatter.parse(timeLabel)
            if (time == null) -1 else {
                val cal = Calendar.getInstance().apply { this.time = time }
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            }
        } catch (e: Exception) {
            -1
        }
    }

    fun selectTimeSlot(time: String) {
        val selectedTimestamp = _selectedDate.value
        if (selectedTimestamp != null && isSameDay(selectedTimestamp, System.currentTimeMillis())) {
            val slotMinute = parseTimeLabelToMinutes(time)
            if (slotMinute != -1 && slotMinute <= getCurrentMinuteOfDay()) {
                Log.w("SharedViewModel", "Attempted to select past time slot on today: $time")
                return
            }
        }

        _selectedTimeSlot.value = time
    }

    // -----------------------------------------------------------
    // Profile Fetching Methods
    // -----------------------------------------------------------

    fun fetchPatientProfiles() {
        if (repository == null) return

        _loading.value = true
        val uid = repository.getCurrentUserUid()

        if (uid != null) {
            repository.getPatientProfiles(uid) { result ->
                _loading.value = false
                result.onSuccess { profiles ->
                    _patientProfiles.value = profiles
                }.onFailure {
                    _patientProfiles.value = emptyList()
                }
            }
        } else {
            _loading.value = false
        }
    }

    fun setPatientProfiles(profiles: List<PatientProfile>) {
        _patientProfiles.value = profiles
    }

    fun selectPatientBooking(booking: Booking?) {
        _selectedPatientBooking.value = booking
    }

    // Risk Analysis Symptoms Questions
    fun initSymptomChecker() {
        val picked = com.example.neuronexus.common.utils.Constant.getSymptomsData()
        _symptomQuestions.value = picked
        _symptomScore.value = 0
        _symptomQuestionIndex.value = 0
    }

    fun submitSymptomAnswer(answer: String) {
        val score = _symptomScore.value ?: 0
        _symptomScore.value = when (answer) {
            "Yes"       -> score + 2
            "Sometimes" -> score + 1
            else        -> score         // "No" = +0
        }
        val index = _symptomQuestionIndex.value ?: 0
        _symptomQuestionIndex.value = index + 1
    }

    fun setFinalRiskScore(score: Int) {
        _finalRiskScore.value = score
    }

    fun clearSymptomState() {
        _symptomScore.value = 0
        _symptomQuestionIndex.value = 0
        _symptomQuestions.value = emptyList()
    }

    // -----------------------------------------------------------
    // Formatting Helpers
    // -----------------------------------------------------------

    private fun formatMinutesToTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        val amPm = if (hours < 12) "AM" else "PM"
        var displayHour = hours
        if (hours > 12) displayHour -= 12
        if (displayHour == 0) displayHour = 12
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, mins, amPm)
    }
}