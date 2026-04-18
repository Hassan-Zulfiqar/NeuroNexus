package com.example.neuronexus.patient.ui.lab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentTestDetailsBinding
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TestDetailsFragment : Fragment() {

    private var _binding: FragmentTestDetailsBinding? = null
    private val binding get() = _binding!!

    // Use sharedViewModel to get the selected test passed from LabDetails
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var currentTest: LabTest? = null
    private var currentLab: Lab? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe Data
        sharedViewModel.selectedLabTest.observe(viewLifecycleOwner) { test ->
            if (test != null) {
                currentTest = test
                bindTestDetails(test)
            } else {
                AlertUtils.showError(requireContext(), "Error: No Test Selected")
                findNavController().popBackStack()
            }
        }

        sharedViewModel.selectedLab.observe(viewLifecycleOwner) { lab ->
            if (lab != null) {
                currentLab = lab
                binding.tvLabName.text = "Provided by ${lab.name}"
            } else {
                AlertUtils.showError(requireContext(), "Error: Lab information missing")
                findNavController().popBackStack()
            }
        }

        setupListeners()
    }

    private fun bindTestDetails(test: LabTest) {
        binding.tvTestName.text = test.testName
        binding.tvCategory.text = test.category
        binding.tvDescription.text = test.description
        binding.tvPrice.text = test.price // "0.00"

        // Installments Logic
        if (test.installments.equals("yes", ignoreCase = true)) {
            binding.tvInstallments.visibility = View.VISIBLE
            binding.cardInstallments.visibility = View.VISIBLE // Ensure container is visible
            binding.tvInstallments.text = "${test.noOfInstallments} Installments Available"
        } else {
            binding.tvInstallments.visibility = View.GONE
            binding.cardInstallments.visibility = View.GONE
        }

        // Sample Report Logic
        if (test.sampleReportImageUrl.isNotEmpty()) {
            binding.layoutSampleReport.visibility = View.VISIBLE
            Glide.with(this)
                .load(test.sampleReportImageUrl)
                .placeholder(R.drawable.placeholder_report) // Ensure this drawable exists or use generic
                .into(binding.ivSampleReport)
        } else {
            binding.layoutSampleReport.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnBookAppointment.setOnClickListener {
            if (currentTest != null && currentLab != null) {
               sharedViewModel.selectDate(0L)
                sharedViewModel.selectTimeSlot("")
                sharedViewModel.setBookingReason("")

                findNavController().navigate(R.id.action_testDetails_to_labSchedule)
            } else {
                AlertUtils.showError(requireContext(), "Please wait for test details to load.")
            }
        }

        binding.cardSampleReport.setOnClickListener {
            Toast.makeText(requireContext(), "Opening PDF Preview...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}