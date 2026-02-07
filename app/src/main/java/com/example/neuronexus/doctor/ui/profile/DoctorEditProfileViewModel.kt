package com.example.neuronexus.doctor.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DoctorEditProfileViewModel : ViewModel() {

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun saveChanges(
        name: String,
        phone: String,
        address: String,
        qualification: String,
        fee: String,
        schedule: String,
        pass: String
    ) {
        // 1. Validation
        if (name.isBlank() || phone.isBlank() || address.isBlank()) {
            _errorMessage.value = "Basic details cannot be empty"
            return
        }
        if (fee.isBlank()) {
            _errorMessage.value = "Please enter your consultation fee"
            return
        }
        if (schedule.isBlank()) {
            _errorMessage.value = "Please specify your availability"
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            delay(1000)
            _saveStatus.value = true
        }
    }
}