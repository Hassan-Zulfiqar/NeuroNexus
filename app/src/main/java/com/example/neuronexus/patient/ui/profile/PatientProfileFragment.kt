package com.example.neuronexus.patient.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.databinding.FragmentPatientProfileBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class PatientProfileFragment : Fragment() {

    private var _binding: FragmentPatientProfileBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers()

        networkViewModel.fetchPatientDetails()
    }

    private fun setupObservers() {
        networkViewModel.patientDetails.observe(viewLifecycleOwner) { result ->
            result.onSuccess { patient ->
                binding.tvName.text = patient.name
                binding.tvEmail.text = patient.email
                binding.tvPhone.text = patient.phone
                binding.tvCnic.text = patient.cnic

                // Load Profile Image
                Glide.with(this)
                    .load(patient.profileImageUrl)
                    .placeholder(R.drawable.doctor) // Placeholder
                    .error(R.drawable.doctor) // Fallback
                    .into(binding.imgProfile)

            }.onFailure { error ->
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load profile")
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_edit_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}