package com.example.neuronexus.common.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.common.models.User
import com.example.neuronexus.common.repository.AppRepository
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.models.Patient

class AuthViewModel(private val repository: AppRepository) : ViewModel()
{

    private val _userState = MutableLiveData<Result<User>>()
    val userState: LiveData<Result<User>> = _userState

    // 2. Registration/Reset Messages
    private val _authMessage = MutableLiveData<Result<String>>()
    val authMessage: LiveData<Result<String>> = _authMessage

    // 3. Loading State
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // -------------------------
    // LOGIN FUNCTIONS
    // -------------------------
    fun login(email: String, pass: String) {
        _loading.value = true
        repository.loginUser(email, pass, object : AppRepository.LoginCallback {
            override fun onSuccess(user: User) {
                _loading.value = false
                _userState.value = Result.success(user)
            }

            override fun onError(message: String) {
                _loading.value = false
                _userState.value = Result.failure(Exception(message))
            }
        })
    }

    fun googleLogin(idToken: String) {
        _loading.value = true
        repository.firebaseAuthWithGoogle(idToken, object : AppRepository.LoginCallback {
            override fun onSuccess(user: User) {
                _loading.value = false
                _userState.value = Result.success(user)
            }

            override fun onError(message: String) {
                _loading.value = false
                _userState.value = Result.failure(Exception(message))
            }
        })
    }

    // -------------------------
    // REGISTRATION FUNCTIONS
    // -------------------------
    fun registerDoctor(doctor: Doctor, pass: String, profileUri: Uri?, licenseUri: Uri?) {
        _loading.value = true
        repository.registerDoctor(doctor, pass, profileUri, licenseUri, object : AppRepository.RegisterCallback {
            override fun onSuccess(message: String) {
                _loading.value = false
                _authMessage.value = Result.success(message)
            }

            override fun onError(message: String) {
                _loading.value = false
                _authMessage.value = Result.failure(Exception(message))
            }
        })
    }

    fun registerPatient(patient: Patient, pass: String, profileUri: Uri?) {
        _loading.value = true
        repository.registerPatient(patient, pass, profileUri, object : AppRepository.RegisterCallback {
            override fun onSuccess(message: String) {
                _loading.value = false
                _authMessage.value = Result.success(message)
            }

            override fun onError(message: String) {
                _loading.value = false
                _authMessage.value = Result.failure(Exception(message))
            }
        })
    }

    // -------------------------
    // RESET PASSWORD
    // -------------------------
    fun resetPassword(email: String) {
        _loading.value = true
        repository.sendPasswordResetEmail(email, object : AppRepository.RegisterCallback {
            override fun onSuccess(message: String) {
                _loading.value = false
                _authMessage.value = Result.success(message)
            }

            override fun onError(message: String) {
                _loading.value = false
                _authMessage.value = Result.failure(Exception(message))
            }
        })
    }


    fun checkCurrentUser()
    {
        val uid = repository.getCurrentUserUid()
        if (uid != null)
        {
            // User is logged in, fetch their profile to know the role
            _loading.value = true
            repository.getUser(uid, object : AppRepository.LoginCallback
            {
                override fun onSuccess(user: User)
                {
                    _loading.value = false
                    _userState.value = Result.success(user)
                }

                override fun onError(message: String)
                {
                    _loading.value = false
                    _userState.value = Result.failure(Exception(message))
                }
            })
        }
        else
        {
            // No user logged in
            _userState.value = Result.failure(Exception("No user logged in"))
        }
    }

}