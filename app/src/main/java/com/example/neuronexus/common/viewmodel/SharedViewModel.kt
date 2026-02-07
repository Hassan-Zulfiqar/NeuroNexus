package com.example.neuronexus.common.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.common.repository.AppRepository
import com.example.neuronexus.models.Doctor
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import com.example.neuronexus.patient.models.PatientProfile
import com.example.neuronexus.patient.model.TimeSlot
import com.example.neuronexus.patient.utils.ScheduleParser
import java.util.Calendar
import java.util.Locale

class SharedViewModel(private val repository: AppRepository? = null) : ViewModel() {

    // 1. Selected Doctor
    private val _selectedDoctor = MutableLiveData<Doctor>()
    val selectedDoctor: LiveData<Doctor> = _selectedDoctor

    // 2. Booking Data
    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    private val _selectedTimeSlot = MutableLiveData<String>()
    val selectedTimeSlot: LiveData<String> = _selectedTimeSlot

    // 3. Generated Time Slots
    private val _availableTimeSlots = MutableLiveData<List<TimeSlot>>()
    val availableTimeSlots: LiveData<List<TimeSlot>> = _availableTimeSlots

    // 4. Patient Profile State
    private val _patientProfiles = MutableLiveData<List<PatientProfile>>()
    val patientProfiles: LiveData<List<PatientProfile>> = _patientProfiles

    private val _selectedPatientProfile = MutableLiveData<PatientProfile>()
    val selectedPatientProfile: LiveData<PatientProfile> = _selectedPatientProfile

    // 5. Reason for Visit (New)
    private val _bookingReason = MutableLiveData<String>()
    val bookingReason: LiveData<String> = _bookingReason

    // 6. Lab Selection (NEW ADDITION)
    private val _selectedLab = MutableLiveData<Lab>()
    val selectedLab: LiveData<Lab> = _selectedLab

    private val _selectedLabTest = MutableLiveData<LabTest>()
    val selectedLabTest: LiveData<LabTest> = _selectedLabTest

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
        generateSlotsForDate(timestamp)
    }

    fun selectTimeSlot(time: String) {
        _selectedTimeSlot.value = time
    }

    fun selectPatientProfile(profile: PatientProfile) {
        _selectedPatientProfile.value = profile
    }

    fun setDraftPatientProfile(profile: PatientProfile) {
        _selectedPatientProfile.value = profile
    }

    // New Method
    fun setBookingReason(reason: String) {
        _bookingReason.value = reason
    }

    // Lab Functions (NEW ADDITION)
    fun selectLab(lab: Lab) {
        _selectedLab.value = lab
    }

    fun selectLabTest(test: LabTest) {
        _selectedLabTest.value = test
    }

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

    // ... Internal Slot Generation Logic ...
    private fun generateSlotsForDate(dateTimestamp: Long) {
        val doctor = _selectedDoctor.value ?: return
        val rawSchedule = doctor.schedule

        val doctorSchedule = ScheduleParser.parseSchedule(rawSchedule)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateTimestamp

        val androidDay = calendar.get(Calendar.DAY_OF_WEEK)
        val ourDayIndex = if (androidDay == Calendar.SUNDAY) 7 else androidDay - 1

        val ranges = doctorSchedule.weeklyAvailability[ourDayIndex]
        val slots = mutableListOf<TimeSlot>()

        if (ranges != null) {
            for (range in ranges) {
                var currentMinute = range.startMinute
                while (currentMinute < range.endMinute) {
                    val timeLabel = formatMinutesToTime(currentMinute)
                    slots.add(TimeSlot(timeLabel, currentMinute, isAvailable = true))
                    currentMinute += 30
                }
            }
        }
        _availableTimeSlots.value = slots
    }

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