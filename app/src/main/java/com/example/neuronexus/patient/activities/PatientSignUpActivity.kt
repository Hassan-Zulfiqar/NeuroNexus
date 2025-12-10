package com.example.neuronexus.patient.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.neuronexus.databinding.ActivityPatientSignUpBinding
import com.example.neuronexus.patient.models.Patient
import com.example.neuronexus.common.repository.AuthRepository
import com.example.neuronexus.common.auth.LoginActivity
import java.io.File

class PatientSignUpActivity : AppCompatActivity() {

    private var _binding: ActivityPatientSignUpBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()

    private var profileImageUri: Uri? = null
    private var tempCameraUri: Uri? = null

    private val profileGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            profileImageUri = uri
            binding.imgProfile.setImageURI(uri)
            binding.textUploadHint.text = "Profile Photo Selected"
        }
    }

    private val profileCameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = tempCameraUri

        if (success && uri != null) {
            profileImageUri = uri
            binding.imgProfile.setImageURI(uri)
            binding.textUploadHint.text = "Profile Photo Captured"
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permission granted. Try again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPatientSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            profileImageUri = savedInstanceState.getParcelable("profile_uri")
            tempCameraUri = savedInstanceState.getParcelable("temp_uri")

            if (profileImageUri != null) {
                binding.imgProfile.setImageURI(profileImageUri)
                binding.textUploadHint.text = "Profile Photo Selected"
            }
        }

        setupClickListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("profile_uri", profileImageUri)
        outState.putParcelable("temp_uri", tempCameraUri)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.textGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.layoutProfileImage.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnRegisterPatient.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> profileGalleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val uri = createImageUri()
        tempCameraUri = uri

        profileCameraLauncher.launch(uri)
    }

    private fun createImageUri(): Uri {
        val imageFileName = "JPEG_" + System.currentTimeMillis() + ".jpg"
        val imageFile = File(cacheDir, imageFileName)
        return FileProvider.getUriForFile(
            applicationContext,
            "${packageName}.fileprovider",
            imageFile
        )
    }

    private fun validateAndRegister() {
        val name = binding.inputName.editText?.text.toString().trim()
        val email = binding.inputEmail.editText?.text.toString().trim()
        val phone = binding.inputContact.editText?.text.toString().trim()
        val cnic = binding.inputCnic.editText?.text.toString().trim()
        val password = binding.inputPass.editText?.text.toString().trim()
        val confirmPass = binding.inputConfirmPass.editText?.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || cnic.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val patient = Patient(
            name = name,
            email = email,
            phone = phone,
            cnic = cnic
        )

        showLoading(true)

        authRepository.registerPatient(
            patient,
            password,
            profileImageUri,
            object : AuthRepository.RegisterCallback {
                override fun onSuccess(message: String) {
                    showLoading(false)
                    Toast.makeText(this@PatientSignUpActivity, message, Toast.LENGTH_LONG).show()

                    val intent = Intent(this@PatientSignUpActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

                override fun onError(message: String) {
                    showLoading(false)
                    Toast.makeText(this@PatientSignUpActivity, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showLoading(isLoading: Boolean) {
        val progressBar = findViewById<View>(R.id.progressBar)
        val btnRegister = binding.btnRegisterPatient

        if (isLoading) {
            progressBar?.visibility = View.VISIBLE
            btnRegister.isEnabled = false
            btnRegister.text = "Registering..."
        } else {
            progressBar?.visibility = View.GONE
            btnRegister.isEnabled = true
            btnRegister.text = "SIGN UP"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

