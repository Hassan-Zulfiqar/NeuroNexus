package com.example.neuronexus.patient.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.ui.ImagePickerHelper
import com.example.neuronexus.common.ui.ImageSelectionDialog
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.databinding.FragmentPatientEditProfileBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class PatientEditProfileFragment : Fragment() {

    private var _binding: FragmentPatientEditProfileBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()

    // 1. Declare the Helper
    private lateinit var imagePickerHelper: ImagePickerHelper
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientEditProfileBinding.inflate(inflater, container, false)

        // 2. Initialize the Helper immediately in onCreateView
        // This matches the working DoctorHomeFragment pattern
        imagePickerHelper = ImagePickerHelper(this) { imageUri ->
            onImageSelected(imageUri)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        setupUI() // Hides password, etc.
        setupListeners()
        setupObservers()

        // Fetch data if needed
        if (networkViewModel.patientDetails.value == null) {
            networkViewModel.fetchPatientDetails()
        }
    }

//    private fun setupUI() {
//        // Hide Password Field (Security Best Practice)
//        (binding.etPassword.parent.parent as? View)?.visibility = View.GONE
//    }

    private fun showImageSelectionDialog() {
        // Create the dialog and pass the click actions
        val dialog = ImageSelectionDialog(
            onCameraClick = {
                imagePickerHelper.openCamera()
            },
            onGalleryClick = {
                imagePickerHelper.openGallery()
            }
        )
        dialog.show(parentFragmentManager, "ImageSelectionDialog")
    }

    private fun onImageSelected(uri: Uri) {
        selectedImageUri = uri
        // Show preview ONLY (Do not upload yet)
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.doctor)
            .into(binding.imgProfile)
    }

    private fun setupObservers() {
        // 1. Pre-fill Data
        networkViewModel.patientDetails.observe(viewLifecycleOwner) { result ->
            result.onSuccess { patient ->
                // Only populate if fields are empty (first load)
                if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(patient.name)
                if (binding.etEmail.text.isNullOrEmpty()) binding.etEmail.setText(patient.email)
                if (binding.etPhone.text.isNullOrEmpty()) binding.etPhone.setText(patient.phone)
                if (binding.etCnic.text.isNullOrEmpty()) binding.etCnic.setText(patient.cnic)

                // Load existing image only if user hasn't picked a new one yet
                if (selectedImageUri == null) {
                    Glide.with(this)
                        .load(patient.profileImageUrl)
                        .placeholder(R.drawable.doctor)
                        .error(R.drawable.doctor)
                        .into(binding.imgProfile)
                }
            }
        }

        // 2. Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnChangePhoto.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "SAVING..." else "SAVE CHANGES"
        }

        // 3. Profile Update Result
        networkViewModel.profileUpdateResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess {
                    Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                    networkViewModel.resetProfileUpdateState()
                    findNavController().navigateUp()
                }.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Update Failed")
                    networkViewModel.resetProfileUpdateState()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChangePhoto.setOnClickListener {
            showImageSelectionDialog()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                AlertUtils.showError(requireContext(), "Name and Phone cannot be empty")
                return@setOnClickListener
            }

            // Call the consolidated save function
            networkViewModel.saveProfileChanges(name, phone, selectedImageUri)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}