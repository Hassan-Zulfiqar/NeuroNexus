package com.example.neuronexus.patient.ui.doctor_discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientSelectionBinding
import com.example.neuronexus.patient.models.PatientProfile
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.UUID

class PatientSelectionFragment : Fragment() {

    private var _binding: FragmentPatientSelectionBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by sharedViewModel()
    private var selectedProfileOption: String = "Self"
    private var loadedProfiles: List<PatientProfile> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenderDropdown()
        setupListeners()
        setupObservers()
        sharedViewModel.fetchPatientProfiles()
    }

    private fun setupObservers() {
        sharedViewModel.patientProfiles.observe(viewLifecycleOwner) { profiles ->
            loadedProfiles = profiles
            updateDropdown(profiles)
        }
    }

    private fun updateDropdown(profiles: List<PatientProfile>) {
        val displayList = mutableListOf<String>()
        var selfProfileExists = false

        profiles.forEach {
            if (it.relation.equals("Self", ignoreCase = true)) selfProfileExists = true
        }

        if (!selfProfileExists) displayList.add("Self")

        profiles.forEach {
            displayList.add("${it.relation}: ${it.fullName}")
        }

        displayList.add("Add New Patient")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayList)
        binding.actvSelectPatient.setAdapter(adapter)

        binding.actvSelectPatient.setOnItemClickListener { _, _, position, _ ->
            val selection = displayList[position]

            if (selection == "Add New Patient") {
                handleNewPatientSelection()
            } else if (selection == "Self" && !selfProfileExists) {
                handleSelfSelection()
            } else {
                val listIndex = if (!selfProfileExists) position - 1 else position
                if (listIndex >= 0 && listIndex < profiles.size) {
                    val selectedProfile = profiles[listIndex]
                    handleExistingProfileSelection(selectedProfile)
                }
            }
        }
    }

    private fun setupGenderDropdown() {
        val genders = listOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.actvGender.setAdapter(adapter)
    }

    private fun handleNewPatientSelection() {
        selectedProfileOption = "New"
        clearForm()
        // New patient: All fields editable and visible
        binding.inputRelation.visibility = View.VISIBLE
        setFormEnabled(true)
        binding.etName.requestFocus()
    }

    private fun handleSelfSelection() {
        selectedProfileOption = "Self"
        clearForm()
        // Manual Self: All fields editable (since no DB record yet)
        binding.etName.setText("My Name")
        binding.inputRelation.visibility = View.GONE
        binding.etRelation.setText("Self")
        setFormEnabled(true)
    }

    private fun handleExistingProfileSelection(profile: PatientProfile) {
        selectedProfileOption = "Existing"

        binding.etName.setText(profile.fullName)
        binding.etAge.setText(profile.age)
        binding.actvGender.setText(profile.gender, false)
        binding.etContact.setText(profile.contactNumber)
        binding.etRelation.setText(profile.relation)

        // Existing Profile: Personal details are READ-ONLY
        setFormEnabled(false)

        // Logic check: If existing profile is "Self", hide relation input
        binding.inputRelation.visibility = if (profile.relation.equals("Self", ignoreCase = true)) View.GONE else View.VISIBLE
    }

    private fun setFormEnabled(enabled: Boolean) {
        // Personal details lock/unlock
        binding.inputName.isEnabled = enabled
        binding.inputAge.isEnabled = enabled
        binding.menuGender.isEnabled = enabled
        binding.inputContact.isEnabled = enabled
        binding.inputRelation.isEnabled = enabled

        // BUG FIX: Reason MUST always be enabled and visible
        binding.inputReason.isEnabled = true
        binding.inputReason.visibility = View.VISIBLE
    }

    private fun clearForm() {
        binding.etName.text?.clear()
        binding.etAge.text?.clear()
        binding.actvGender.text?.clear()
        binding.etContact.text?.clear()
        binding.etRelation.text?.clear()
        binding.etReason.text?.clear()
        // Reset to enabled for new entry
        setFormEnabled(true)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnConfirm.setOnClickListener { validateAndContinue() }
    }

    private fun validateAndContinue() {
        val name = binding.etName.text.toString().trim()
        val age = binding.etAge.text.toString().trim()
        val gender = binding.actvGender.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()
        val reason = binding.etReason.text.toString().trim()
        val relation = binding.etRelation.text.toString().trim()

        // Validation
        if (name.isEmpty()) { binding.inputName.error = "Required"; return }
        if (age.isEmpty()) { binding.inputAge.error = "Required"; return }
        if (gender.isEmpty()) { binding.menuGender.error = "Required"; return }
        if (contact.isEmpty()) { binding.inputContact.error = "Required"; return }
        if (reason.isEmpty()) { binding.inputReason.error = "Required"; return }

        if (binding.inputRelation.visibility == View.VISIBLE && relation.isEmpty()) {
            binding.inputRelation.error = "Required"; return
        }

        val finalProfile = PatientProfile(
            fullName = name,
            age = age,
            gender = gender,
            contactNumber = contact,
            relation = relation.ifEmpty { "Self" }
        )

        // Save to SharedViewModel
        if (selectedProfileOption == "Existing") {
            sharedViewModel.selectPatientProfile(finalProfile)
        } else {
            sharedViewModel.setDraftPatientProfile(finalProfile)
        }

        // Save Reason
        sharedViewModel.setBookingReason(reason)

        // Navigate to Booking Confirmation
        findNavController().navigate(R.id.action_patient_to_booking_confirmation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}