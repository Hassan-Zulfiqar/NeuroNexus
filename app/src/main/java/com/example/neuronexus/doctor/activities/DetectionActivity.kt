package com.example.neuronexus.doctor.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ActivityDetectionBinding
import com.example.neuronexus.doctor.repository.TumorDetectionRepository
import com.google.firebase.auth.FirebaseAuth

class DetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectionBinding
    private var imageUri: Uri? = null
    private val tumorDetectionRepository = TumorDetectionRepository()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)


        imageUri = intent.getParcelableExtra("image_uri")

        Log.e("CHECK_IMG", "Detection: ${imageUri.toString()}")


        if (imageUri != null) {
            displayImage(imageUri!!)
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupClickListeners()
    }

    private fun displayImage(uri: Uri) {
        try {
            binding.imageViewScan.setImageURI(uri)
        } catch (e: Exception) {
            binding.imageViewScan.setImageResource(R.drawable.brain_tumor)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSave.setOnClickListener {
            saveDetectionRecord()
        }
    }

    private fun saveDetectionRecord() {
        val location = binding.tvLocation.text.toString().trim()
        val tumorDetected = binding.tvTumorDetected.text.toString().trim()
        val size = binding.tvSize.text.toString().trim()

        if (location.isEmpty() || tumorDetected.isEmpty() || size.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "No image available", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        tumorDetectionRepository.saveTumorDetection(
            doctorId = currentUser.uid,
            imageUri = imageUri!!,
            location = location,
            tumorDetected = tumorDetected,
            size = size,
            callback = object : TumorDetectionRepository.SaveCallback {
                override fun onSuccess(recordId: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        this@DetectionActivity,
                        "Record saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                override fun onError(message: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this@DetectionActivity, "Error: $message", Toast.LENGTH_LONG)
                        .show()
                }
            }
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}

