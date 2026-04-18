package com.example.neuronexus.doctor.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.databinding.FragmentDoctorProfileBinding
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.viewModel

class DoctorProfileFragment : Fragment() {

    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!

    // Correct ViewModel injection using Koin
    private val networkViewModel: NetworkViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // Fetch data onResume so the profile automatically refreshes
        // when returning from the Edit Profile screen.
        fetchData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_doctor_edit_profile)
        }
    }

    private fun setupObservers() {
        // Observe Doctor Details
        networkViewModel.doctorDetails.observe(viewLifecycleOwner) { result ->
            result.onSuccess { doctor ->
                // Safely bind all fields with a fallback string
                binding.tvName.text = doctor.name.ifEmpty { "Not provided" }
                binding.tvSpecialization.text = doctor.specialization.ifEmpty { "Not provided" }
                binding.tvEmail.text = doctor.email.ifEmpty { "Not provided" }
                binding.tvPhone.text = doctor.phone.ifEmpty { "Not provided" }
                binding.tvAddress.text = doctor.clinicAddress.ifEmpty { "Not provided" }
                binding.tvQualification.text = doctor.qualification.ifEmpty { "Not provided" }
                binding.tvLicense.text = doctor.licenseNumber.ifEmpty { "Not provided" }
                binding.tvSchedule.text = doctor.schedule.ifEmpty { "Not provided" }

                // Safely format Consultation Fee
                binding.tvFee.text = if (doctor.consultationFee.isNotEmpty()) {
                    "Rs. ${doctor.consultationFee}"
                } else {
                    "Not provided"
                }

                // Safely load Profile Image
                if (doctor.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(doctor.profileImageUrl)
                        .placeholder(R.drawable.doctor)
                        .error(R.drawable.doctor)
                        .into(binding.imgProfile)
                } else {
                    binding.imgProfile.setImageResource(R.drawable.doctor)
                }

            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Disable the Edit Profile button while fetching data to prevent interaction
            binding.btnEditProfile.isEnabled = !isLoading
            binding.btnEditProfile.alpha = if (isLoading) 0.5f else 1.0f
        }
    }

    private fun fetchData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        networkViewModel.fetchDoctorDetails(uid)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}