package com.example.neuronexus.doctor.ui.prescription

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentAddPrescriptionBinding
import com.example.neuronexus.patient.models.Prescription
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddPrescriptionFragment : Fragment() {

    private var _binding: FragmentAddPrescriptionBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    // Local State
    private var selectedFollowUpDate: Long? = null
    private var currentAppointment: DoctorAppointment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPrescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // 1. Back Button
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 2. Load Appointment Data
        currentAppointment = sharedViewModel.selectedDoctorAppointment.value
        currentAppointment?.let { appointment ->
            val dateStr = appointment.appointmentDate.ifEmpty { "Unknown Date" }
            binding.tvPatientSubtitle.text = "For ${appointment.patientNameSnapshot} • $dateStr"
        } ?: run {
            Toast.makeText(requireContext(), "Appointment data missing", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // 3. Text Watchers for Validation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }
        binding.etDiagnosis.addTextChangedListener(textWatcher)
        binding.etMedications.addTextChangedListener(textWatcher)

        // 4. Follow Up Date Picker
        val showPicker = View.OnClickListener { showDatePickerDialog() }
        binding.etFollowUpDate.setOnClickListener(showPicker)
        binding.tilFollowUpDate.setEndIconOnClickListener(showPicker)

        // 5. Save Button
        binding.btnSavePrescription.setOnClickListener {
            savePrescription()
        }
    }

    private fun validateForm() {
        val diagnosisFilled = binding.etDiagnosis.text?.toString()?.isNotBlank() == true
        val medicationsFilled = binding.etMedications.text?.toString()?.isNotBlank() == true
        val isLoading = networkViewModel.loading.value == true

        binding.btnSavePrescription.isEnabled = diagnosisFilled && medicationsFilled && !isLoading
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedFollowUpDate = selectedCalendar.timeInMillis

                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(selectedCalendar.time)
                binding.etFollowUpDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Follow up cannot be in the past
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun savePrescription() {
        val appointment = currentAppointment ?: run {
            Toast.makeText(requireContext(), "Appointment data missing", Toast.LENGTH_SHORT).show()
            return
        }

        val diagnosis = binding.etDiagnosis.text?.toString()?.trim() ?: ""
        val medicationsRaw = binding.etMedications.text?.toString() ?: ""

        // Clean and format medications list
        val medications = medicationsRaw
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val instructions = binding.etInstructions.text?.toString()?.trim() ?: ""

        // Strict UI Validations before Network Call
        var isValid = true
        if (diagnosis.isBlank()) {
            binding.tilDiagnosis.error = "Diagnosis is required"
            isValid = false
        } else {
            binding.tilDiagnosis.error = null
        }

        if (medications.isEmpty()) {
            binding.tilMedications.error = "At least one medication is required"
            isValid = false
        } else {
            binding.tilMedications.error = null
        }

        if (!isValid) return

        // Build Payload
        val prescription = Prescription(
            bookingId = appointment.bookingId,
            patientProfileId = appointment.patientProfileId,
            diagnosis = diagnosis,
            medications = medications,
            instructions = instructions,
            followUpDate = selectedFollowUpDate,
            issuedDate = System.currentTimeMillis(),
            doctorId = appointment.doctorId,
            doctorName = appointment.doctorName
        )

        // Lock UI and Execute
        binding.btnSavePrescription.isEnabled = false
        binding.btnSavePrescription.text = "Saving..."
        networkViewModel.savePrescription(prescription)
    }

    private fun setupObservers() {
        networkViewModel.savePrescriptionResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val prescriptionId = it.getOrNull() ?: ""

                    // Update SharedViewModel with fresh appointment data including new prescriptionId
                    currentAppointment?.let { apt ->
                        val updatedAppointment = apt.copy(prescriptionId = prescriptionId)
                        sharedViewModel.selectDoctorAppointment(updatedAppointment)
                    }

                    Toast.makeText(requireContext(), "Prescription saved successfully", Toast.LENGTH_SHORT).show()
                    networkViewModel.resetSavePrescriptionState()
                    // Navigate back to Detail — popUpToInclusive clears this fragment
                    findNavController().navigate(R.id.action_add_prescription_to_detail)
                } else {
                    Toast.makeText(
                        requireContext(),
                        it.exceptionOrNull()?.message ?: "Failed to save prescription",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnSavePrescription.text = "Save Prescription"
                    validateForm() // Re-evaluate button state
                    networkViewModel.resetSavePrescriptionState()
                }
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.btnSavePrescription.text = "Save Prescription"
            }
            validateForm()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}