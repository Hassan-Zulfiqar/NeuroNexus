package com.example.neuronexus.doctor.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.example.neuronexus.common.utils.ScheduleUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.databinding.FragmentDoctorEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.viewModel

class DoctorEditProfileFragment : Fragment() {

    private var _binding: FragmentDoctorEditProfileBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()

    private lateinit var imagePickerHelper: ImagePickerHelper
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorEditProfileBinding.inflate(inflater, container, false)

        // Initialize Image Picker matching Patient side pattern
        imagePickerHelper = ImagePickerHelper(this) { imageUri ->
            onImageSelected(imageUri)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkViewModel.fetchDoctorCategories()
        setupUI()
        setupObservers()
        fetchData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChangePhoto.setOnClickListener {
            showImageSelectionDialog()
        }

        binding.btnSave.setOnClickListener {
            // Read fields safely with null-safe fallbacks
            val name = binding.etName.text?.toString()?.trim() ?: ""
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""
            val address = binding.etAddress.text?.toString()?.trim() ?: ""
            val qualification = binding.etQualification.text?.toString()?.trim() ?: ""
            val fee = binding.etFee.text?.toString()?.trim() ?: ""
            val schedule = binding.etSchedule.text?.toString()?.trim() ?: ""
            val specialization = binding.etSpecialization.text?.toString()?.trim() ?: ""

            // Strict Validation Order
            if (name.isEmpty()) {
                AlertUtils.showError(requireContext(), "Name cannot be empty")
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                AlertUtils.showError(requireContext(), "Phone number cannot be empty")
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                AlertUtils.showError(requireContext(), "Clinic address cannot be empty")
                return@setOnClickListener
            }
            if (qualification.isEmpty()) {
                AlertUtils.showError(requireContext(), "Qualification cannot be empty")
                return@setOnClickListener
            }
            if (fee.isEmpty()) {
                AlertUtils.showError(requireContext(), "Consultation fee cannot be empty")
                return@setOnClickListener
            }

            // Validate Schedule using reusable utility
            val scheduleError = ScheduleUtils.getScheduleValidationError(schedule)
            if (scheduleError != null) {
                AlertUtils.showError(requireContext(), scheduleError)
                return@setOnClickListener
            }

            if (specialization.isEmpty()) {
                AlertUtils.showError(requireContext(), "Specialization cannot be empty")
                return@setOnClickListener
            }


            // All validations passed, execute save (Email, License, Password omitted)
            networkViewModel.saveDoctorProfileChanges(
                name = name,
                phone = phone,
                address = address,
                qualification = qualification,
                specialization = specialization, // NEW
                fee = fee,
                schedule = schedule,
                imageUri = selectedImageUri
            )
        }
    }

    private fun setupObservers() {
        // Observe Doctor Details for Safe Pre-population
        networkViewModel.doctorDetails.observe(viewLifecycleOwner) { result ->
            result.onSuccess { doctor ->
                // Safe-fill pattern: Only populate if fields are currently empty
                if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(doctor.name)
                if (binding.etEmail.text.isNullOrEmpty()) binding.etEmail.setText(doctor.email)
                if (binding.etPhone.text.isNullOrEmpty()) binding.etPhone.setText(doctor.phone)
                if (binding.etAddress.text.isNullOrEmpty()) binding.etAddress.setText(doctor.clinicAddress)
                if (binding.etQualification.text.isNullOrEmpty()) binding.etQualification.setText(doctor.qualification)
                if (binding.etFee.text.isNullOrEmpty()) binding.etFee.setText(doctor.consultationFee)
                if (binding.etSchedule.text.isNullOrEmpty()) binding.etSchedule.setText(doctor.schedule)
                if (binding.etLicense.text.isNullOrEmpty()) binding.etLicense.setText(doctor.licenseNumber)
                if (binding.etSpecialization.text.isNullOrEmpty()) binding.etSpecialization.setText(doctor.specialization, false) // false = no dropdown filtering
                // Load remote image only if the user hasn't selected a local one
                if (selectedImageUri == null) {
                    if (doctor.profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(doctor.profileImageUrl)
                            .placeholder(R.drawable.doctor)
                            .error(R.drawable.doctor)
                            .into(binding.imgProfile)
                    } else {
                        binding.imgProfile.setImageResource(R.drawable.doctor)
                    }
                }
            }
        }

        // Observe Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "Saving..." else "SAVE CHANGES"
        }

        // Observe Profile Update Result
        networkViewModel.profileUpdateResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    networkViewModel.resetProfileUpdateState()
                    findNavController().navigateUp()
                }.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Update failed")
                    networkViewModel.resetProfileUpdateState()
                }
            }
        }

        networkViewModel.doctorCategories.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val categories = result.getOrNull() ?: emptyList()
                if (categories.isNotEmpty()) {
                    val adapter = android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        categories
                    )
                    binding.etSpecialization.setAdapter(adapter)
                } else {
                    binding.etSpecialization.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                }
            } else {
                binding.etSpecialization.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }
            networkViewModel.resetDoctorCategories()
        }
    }

    private fun fetchData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }
        networkViewModel.fetchDoctorDetails(uid)
    }

    private fun showImageSelectionDialog() {
        val dialog = ImageSelectionDialog(
            onCameraClick = { imagePickerHelper.openCamera() },
            onGalleryClick = { imagePickerHelper.openGallery() }
        )
        dialog.show(parentFragmentManager, "ImageSelectionDialog")
    }

    private fun onImageSelected(uri: Uri) {
        selectedImageUri = uri
        // Show preview immediately using local URI before uploading
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.doctor)
            .into(binding.imgProfile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}