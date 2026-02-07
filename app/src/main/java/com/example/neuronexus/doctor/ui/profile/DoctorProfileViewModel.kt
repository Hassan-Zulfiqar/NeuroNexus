package com.example.neuronexus.doctor.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DoctorProfileViewModel : ViewModel() {

    // Define LiveData for all doctor fields
    private val _name = MutableLiveData<String>()
    val name: LiveData<String> = _name

    private val _specialization = MutableLiveData<String>()
    val specialization: LiveData<String> = _specialization

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _phone = MutableLiveData<String>()
    val phone: LiveData<String> = _phone

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> = _address

    private val _qualification = MutableLiveData<String>()
    val qualification: LiveData<String> = _qualification

    private val _fee = MutableLiveData<String>()
    val fee: LiveData<String> = _fee

    private val _license = MutableLiveData<String>()
    val license: LiveData<String> = _license

    private val _schedule = MutableLiveData<String>()
    val schedule: LiveData<String> = _schedule

    init {
        loadDoctorProfile()
    }

    private fun loadDoctorProfile() {
        _name.value = "Dr. Ahmad"
        _specialization.value = "Cardiologist"
        _email.value = "hassanzulfiqar687@gmail.com"
        _phone.value = "0334-1245367"
        _address.value = "ABC, 124"
        _qualification.value = "MBBS, FCPS"
        _fee.value = "Rs. 3500"
        _license.value = "1234567"
        _schedule.value = "Mon-Wed: 5pm-9pm Sat-Sun: 6pm-8pm"
    }
}