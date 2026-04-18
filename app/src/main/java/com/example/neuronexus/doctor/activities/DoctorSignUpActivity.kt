package com.example.neuronexus.doctor.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.databinding.ActivityDoctorSignUpBinding
import com.example.neuronexus.doctor.models.Doctor
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class DoctorSignUpActivity : AppCompatActivity() {

    private var _binding: ActivityDoctorSignUpBinding? = null
    private val binding get() = _binding!!
    // Inject NetworkViewModel for categories
    private val networkViewModel: NetworkViewModel by viewModel()
    // 1. Inject ViewModel using Koin
    private val viewModel: AuthViewModel by viewModel()

    private var profileImageUri: Uri? = null
    private var licenseImageUri: Uri? = null
    private var tempCameraUri: Uri? = null

    // Image Launchers
    private val profileGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            profileImageUri = uri
            binding.imgProfile.setImageURI(uri)
            binding.textUploadHint.text = "Profile Photo Selected"
        }
    }

    private val licenseGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            licenseImageUri = uri
            binding.imgLicensePreview.setImageURI(uri)
            binding.imgLicensePreview.scaleType = ImageView.ScaleType.CENTER_CROP
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

    private val licenseCameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            licenseImageUri = uri
            binding.imgLicensePreview.setImageURI(uri)
            binding.imgLicensePreview.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permission granted. Try again.", Toast.LENGTH_SHORT).show()
        } else {
            AlertUtils.showError(this, "Camera permission is required to take photos.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDoctorSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }
        networkViewModel.fetchDoctorCategories()
        setupClickListeners()
        setupObservers()
    }

    private fun restoreState(savedInstanceState: Bundle) {
        profileImageUri = savedInstanceState.getParcelable("profile_uri")
        licenseImageUri = savedInstanceState.getParcelable("license_uri")
        tempCameraUri = savedInstanceState.getParcelable("temp_uri")

        if (profileImageUri != null) {
            binding.imgProfile.setImageURI(profileImageUri)
            binding.textUploadHint.text = "Profile Photo Selected"
        }
        if (licenseImageUri != null) {
            binding.imgLicensePreview.setImageURI(licenseImageUri)
            binding.imgLicensePreview.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("profile_uri", profileImageUri)
        outState.putParcelable("license_uri", licenseImageUri)
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
            showImageSourceDialog(isProfile = true)
        }

        binding.cardLicenseUpload.setOnClickListener {
            showImageSourceDialog(isProfile = false)
        }

        binding.btnRegisterDoctor.setOnClickListener {
            validateAndRegister()
        }
    }

    // 2. Setup Observers
    private fun setupObservers() {
        // Observe Loading State
        viewModel.loading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        // Observe Registration Result (Success or Failure)
        viewModel.authMessage.observe(this) { result ->
            result.onSuccess { message ->
                // Success
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            result.onFailure { error ->
                // Failure
                AlertUtils.showError(this, error.message ?: "Registration Failed")
            }
        }

        networkViewModel.doctorCategories.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val categories = result.getOrNull() ?: emptyList()
                if (categories.isEmpty()) {
                    // Fallback — if no categories in Firebase yet, allow free text
                    binding.inputSpec.editText?.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                } else {
                    val adapter = android.widget.ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        categories
                    )
                    (binding.inputSpec.editText as? android.widget.AutoCompleteTextView)?.setAdapter(adapter)
                }
            } else {
                // Firebase fetch failed — allow free text as fallback
                binding.inputSpec.editText?.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                Toast.makeText(this, "Could not load specializations. Please type manually.", Toast.LENGTH_SHORT).show()
            }
            networkViewModel.resetDoctorCategories()
        }
    }

    private fun showImageSourceDialog(isProfile: Boolean) {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen(isProfile)
                    1 -> {
                        if (isProfile) profileGalleryLauncher.launch("image/*")
                        else licenseGalleryLauncher.launch("image/*")
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen(isProfile: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera(isProfile)
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera(isProfile: Boolean) {
        val uri = createImageUri()
        tempCameraUri = uri
        if (isProfile) profileCameraLauncher.launch(uri)
        else licenseCameraLauncher.launch(uri)
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
        val password = binding.inputPass.editText?.text.toString().trim()
        val confirmPass = binding.inputConfirmPass.editText?.text.toString().trim()
        val license = binding.inputLicense.editText?.text.toString().trim()
        val specialization = binding.inputSpec.editText?.text.toString().trim()
        val qualification = binding.inputQual.editText?.text.toString().trim()
        val address = binding.inputAddress.editText?.text.toString().trim()
        val fee = binding.inputFee.editText?.text.toString().trim()
        val schedule = binding.inputSchedule.editText?.text.toString().trim()


        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || license.isEmpty()) {
            AlertUtils.showError(this, "Please fill all required fields", "Missing Info")
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

        if (licenseImageUri == null) {
            AlertUtils.showError(this, "Please upload your License Copy", "License Missing")
            return
        }

        val doctor = Doctor(
            name = name,
            email = email,
            phone = phone,
            licenseNumber = license,
            specialization = specialization,
            qualification = qualification,
            clinicAddress = address,
            consultationFee = fee,
            schedule = schedule,
            registrationStatus = "pending"
        )

        // 4. Trigger ViewModel Action
        viewModel.registerDoctor(doctor, password, profileImageUri, licenseImageUri)
    }

    private fun showLoading(isLoading: Boolean) {
        val progressBar = findViewById<View>(R.id.progressBar)
        if (isLoading) {
            progressBar?.visibility = View.VISIBLE
            binding.btnRegisterDoctor.isEnabled = false
            binding.btnRegisterDoctor.text = "Registering..."
        } else {
            progressBar?.visibility = View.GONE
            binding.btnRegisterDoctor.isEnabled = true
            binding.btnRegisterDoctor.text = "SIGN UP"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}