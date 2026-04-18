package com.example.neuronexus.patient.ui.medicalrecords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientProfileSelectorBinding
import com.example.neuronexus.patient.adapters.PatientProfileSelectorAdapter
import com.example.neuronexus.patient.models.PatientProfile
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PatientProfileSelectorFragment : Fragment() {

    private var _binding: FragmentPatientProfileSelectorBinding? = null
    private val binding get() = _binding

    // Koin ViewModel Injection (SharedViewModel only)
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var profileAdapter: PatientProfileSelectorAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPatientProfileSelectorBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // Ensure we always fetch fresh profiles if the user navigates back here
        fetchProfiles()
    }

    private fun fetchProfiles() {
        sharedViewModel.fetchPatientProfiles()
    }

    private fun setupUI() {
        val b = binding ?: return

        profileAdapter = PatientProfileSelectorAdapter(
            profiles = emptyList(),
            onProfileClick = { profile ->
                handleProfileSelected(profile)
            }
        )

        b.rvPatientProfiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = profileAdapter
        }

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun handleProfileSelected(profile: PatientProfile) {
        // Set the active profile
        sharedViewModel.selectPatientProfile(profile)
        // Default to the Prescriptions tab
        sharedViewModel.selectMedicalRecordTab(0)

        // Navigate! (Nav graph action handles popping this selector off the back stack)
        findNavController().navigate(R.id.action_profile_selector_to_medical_records)
    }

    private fun setupObservers() {
        sharedViewModel.patientProfiles.observe(viewLifecycleOwner) { profiles ->
            val b = binding ?: return@observe

            if (profiles.isNullOrEmpty()) {
                // No profiles found
                b.rvPatientProfiles.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "No patient profiles found",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            // Option B: Smart Routing for single-profile accounts
            if (profiles.size == 1) {
                handleProfileSelected(profiles.first())
                return@observe
            }

            // Multiple profiles exist — show selector list
            b.rvPatientProfiles.visibility = View.VISIBLE
            profileAdapter?.updateList(profiles)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}