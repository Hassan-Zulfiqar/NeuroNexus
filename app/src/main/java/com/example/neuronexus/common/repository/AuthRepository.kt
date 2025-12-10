package com.example.neuronexus.common.repository

import android.net.Uri
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.patient.models.Patient
import com.example.neuronexus.common.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AuthRepository {

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

    fun registerDoctor(
        doctor: Doctor,
        password: String,
        profileImageUri: Uri?,
        licenseImageUri: Uri?,
        callback: RegisterCallback
    ) {
        auth.createUserWithEmailAndPassword(doctor.email, password)
            .addOnFailureListener { callback.onError(it.message ?: "Auth Failed") }
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                val profileRef = storage.child("profile_pics/doctors/$uid.jpg")
                val licenseRef = storage.child("license_images/doctors/$uid.jpg")

                uploadImage(profileRef, profileImageUri) { profileUrl ->

                    uploadImage(licenseRef, licenseImageUri) { licenseUrl ->

                        val finalDoctor = doctor.copy(
                            uid = uid,
                            profileImageUrl = profileUrl,
                            licenseImageUrl = licenseUrl
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
                                callback.onSuccess("Doctor Registration Successful!")
                            }
                            .addOnFailureListener {
                                callback.onError(it.message ?: "Database Error")
                            }
                    }
                }
            }
    }


    fun registerPatient(
        patient: Patient,
        password: String,
        profileImageUri: Uri?,
        callback: RegisterCallback
    ) {
        auth.createUserWithEmailAndPassword(patient.email, password)
            .addOnFailureListener { callback.onError(it.message ?: "Auth Failed") }
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

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

    fun loginUser(email: String, pass: String, callback: LoginCallback) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnFailureListener {
                callback.onError(it.message ?: "Login Failed")
            }
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                db.child("users").child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            if (user != null) {
                                callback.onSuccess(user)
                            } else {
                                callback.onError("User data is empty")
                            }
                        } else {
                            callback.onError("User record not found in Database")
                        }
                    }
                    .addOnFailureListener {
                        callback.onError(it.message ?: "Database Error")
                    }
            }
    }

    fun sendPasswordResetEmail(email: String, callback: RegisterCallback) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                callback.onSuccess("Reset link sent to your email!")
            }
            .addOnFailureListener {
                callback.onError(it.message ?: "Failed to send reset email")
            }
    }

}

