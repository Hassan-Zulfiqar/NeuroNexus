package com.example.neuronexus.doctor.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.databinding.FragmentDoctorProfileBinding

class DoctorProfileFragment : Fragment() {

    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DoctorProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DoctorProfileViewModel::class.java)
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Observe all fields
        viewModel.name.observe(viewLifecycleOwner) { binding.tvName.text = it }
        viewModel.specialization.observe(viewLifecycleOwner) { binding.tvSpecialization.text = it }
        viewModel.email.observe(viewLifecycleOwner) { binding.tvEmail.text = it }
        viewModel.phone.observe(viewLifecycleOwner) { binding.tvPhone.text = it }
        viewModel.address.observe(viewLifecycleOwner) { binding.tvAddress.text = it }
        viewModel.qualification.observe(viewLifecycleOwner) { binding.tvQualification.text = it }
        viewModel.fee.observe(viewLifecycleOwner) { binding.tvFee.text = it }
        viewModel.license.observe(viewLifecycleOwner) { binding.tvLicense.text = it }
        viewModel.schedule.observe(viewLifecycleOwner) { binding.tvSchedule.text = it }

        // 2. Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_doctor_edit_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}