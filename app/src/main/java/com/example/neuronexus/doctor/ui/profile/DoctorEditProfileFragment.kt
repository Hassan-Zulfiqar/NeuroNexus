package com.example.neuronexus.doctor.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.databinding.FragmentDoctorEditProfileBinding

class DoctorEditProfileFragment : Fragment() {

    private var _binding: FragmentDoctorEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DoctorEditProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DoctorEditProfileViewModel::class.java)
        _binding = FragmentDoctorEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etName.setText("Dr. Ahmad")
        binding.etEmail.setText("doctor@gmail.com")
        binding.etPhone.setText("0334-1245367")
        binding.etAddress.setText("ABC, 124")
        binding.etQualification.setText("MBBS, FCPS")
        binding.etFee.setText("3500")
        binding.etSchedule.setText("Mon-Wed: 5pm-9pm\nSat-Sun: 6pm-8pm")
        binding.etLicense.setText("1234567")
        binding.etPassword.setText("1234567")

        // 2. Handle Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            // Gather all data
            val name = binding.etName.text.toString()
            val phone = binding.etPhone.text.toString()
            val address = binding.etAddress.text.toString()
            val qual = binding.etQualification.text.toString()
            val fee = binding.etFee.text.toString()
            val schedule = binding.etSchedule.text.toString()
            val pass = binding.etPassword.text.toString()

            // Pass to ViewModel
            viewModel.saveChanges(name, phone, address, qual, fee, schedule, pass)
        }

        // 4. Handle Camera
        binding.btnChangePhoto.setOnClickListener {
            Toast.makeText(context, "Open Gallery...", Toast.LENGTH_SHORT).show()
        }

        // 5. Observe Results
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.saveStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}