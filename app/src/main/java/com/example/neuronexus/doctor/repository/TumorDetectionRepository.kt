package com.example.neuronexus.doctor.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class TumorDetectionRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    interface SaveCallback {
        fun onSuccess(recordId: String)
        fun onError(message: String)
    }

    fun saveTumorDetection(
        doctorId: String,
        imageUri: Uri,
        location: String,
        tumorDetected: String,
        size: String,
        callback: SaveCallback
    ) {
        val recordId = db.child("detect_tumor").push().key ?: return callback.onError("Failed to generate record ID")

        val imageFileName = "tumor_scan_${System.currentTimeMillis()}.jpg"
        val imageRef = storage.child("tumor_scans/$doctorId/$imageFileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val timestamp = System.currentTimeMillis()
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(timestamp))

                    val detectionRecord = mapOf(
                        "recordId" to recordId,
                        "doctorId" to doctorId,
                        "imageUrl" to downloadUrl.toString(),
                        "location" to location,
                        "tumorDetected" to tumorDetected,
                        "size" to size,
                        "timestamp" to timestamp,
                        "date" to formattedDate
                    )

                    db.child("detect_tumor").child(recordId).setValue(detectionRecord)
                        .addOnSuccessListener {
                            callback.onSuccess(recordId)
                        }
                        .addOnFailureListener { e ->
                            callback.onError(e.message ?: "Failed to save record")
                        }
                }
            }
            .addOnFailureListener { e ->
                callback.onError(e.message ?: "Failed to upload image")
            }
    }
}

