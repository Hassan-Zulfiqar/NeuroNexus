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
import com.example.neuronexus.R
import com.example.neuronexus.common.auth.LoginActivity
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.AuthViewModel
import com.example.neuronexus.databinding.ActivityPatientSignUpBinding
import com.example.neuronexus.models.Patient
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class PatientSignUpActivity : AppCompatActivity() {

    private var _binding: ActivityPatientSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModel()

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
            AlertUtils.showInfo(this, "Camera permission granted. You can now take a photo.")
        } else {
            AlertUtils.showError(this, "Camera permission is required to take photos.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPatientSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore State if available
        if (savedInstanceState != null) {
            profileImageUri = savedInstanceState.getParcelable("profile_uri")
            tempCameraUri = savedInstanceState.getParcelable("temp_uri")

            if (profileImageUri != null) {
                binding.imgProfile.setImageURI(profileImageUri)
                binding.textUploadHint.text = "Profile Photo Selected"
            }
        }

        setupClickListeners()
        setupObservers()
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

    // 2. Observe ViewModel State
    private fun setupObservers() {
        viewModel.loading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.authMessage.observe(this) { result ->
            result.onSuccess { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
            result.onFailure { error ->
                AlertUtils.showError(this, error.message ?: "Registration Failed")
            }
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

        // UI Validations
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || cnic.isEmpty() || password.isEmpty()) {
            AlertUtils.showError(this, "Please fill all required fields", "Missing Information")
            return
        }

        if (password.length < 6) {
            AlertUtils.showError(this, "Password must be at least 6 characters")
            return
        }

        if (password != confirmPass) {
            AlertUtils.showError(this, "Passwords do not match")
            return
        }

        val patient = Patient(
            name = name,
            email = email,
            phone = phone,
            cnic = cnic
        )

        // 3. Trigger ViewModel Registration
        viewModel.registerPatient(patient, password, profileImageUri)
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

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}