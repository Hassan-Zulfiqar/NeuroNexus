package com.example.neuronexus.common.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentSubmitComplaintBinding
import com.example.neuronexus.patient.models.Complaint
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.LabTestBooking
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SubmitComplaintFragment : Fragment() {

    private var _binding: FragmentSubmitComplaintBinding? = null
    private val binding get() = _binding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var prefilledAppointmentId: String? = null
    private var prefilledAgainstUserId: String? = null
    private var prefilledAgainstUserRole: String? = null
    private var selectedCategory: String = ""

    private val categoryMap = mapOf(
        "Booking Issue"       to "booking_issue",
        "Payment Issue"       to "payment_issue",
        "Doctor Behavior"     to "doctor_behavior",
        "Lab Report Issue"    to "lab_report_issue",
        "No-Show Abuse"       to "no_show_abuse",
        "Technical Issue"     to "technical",
        "Other"               to "other"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubmitComplaintBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        val b = binding ?: return

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 2. Dynamic Dropdown Setup based on entry point
        val categories = if (prefilledAppointmentId != null) {
            when (prefilledAgainstUserRole) {
                "doctors" -> listOf("Doctor Behavior", "Booking Issue", "Payment Issue", "Technical Issue", "Other")
                "labs" -> listOf("Lab Report Issue", "Booking Issue", "Payment Issue", "Technical Issue", "Other")
                else -> categoryMap.keys.toList()
            }
        } else {
            categoryMap.keys.toList() // General mode gets all categories
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        b.actvCategory.setAdapter(adapter)
        b.actvCategory.setOnItemClickListener { _, _, position, _ ->
            val displayName = categories[position]
            selectedCategory = categoryMap[displayName] ?: ""
            validateForm()
        }

        // Text Watchers for validation
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) { validateForm() }
        }
        b.etSubject.addTextChangedListener(watcher)
        b.etDescription.addTextChangedListener(watcher)

        // 1. Context check (Opened from Appointment vs More Menu)
        val booking = sharedViewModel.selectedPatientBooking.value
        if (booking != null) {
            prefilledAppointmentId = booking.bookingId

            // Dynamic Title & Description for "Report an Issue"
            b.tvTitle.text = "Report an Issue"
            b.tvDescription.text = "Describe the issue with this appointment. Our team will review it and respond within 24 hours."

            when (booking) {
                is DoctorAppointment -> {
                    prefilledAgainstUserId = booking.doctorId
                    prefilledAgainstUserRole = "doctors"
                    b.cardAppointmentContext.visibility = View.VISIBLE
                    b.tvAppointmentContext.text = "${booking.doctorName} — ${booking.appointmentDate}"
                }
                is LabTestBooking -> {
                    prefilledAgainstUserId = booking.labId
                    prefilledAgainstUserRole = "labs"
                    b.cardAppointmentContext.visibility = View.VISIBLE
                    b.tvAppointmentContext.text = "${booking.labName} — ${booking.testDate}"
                }
            }
        } else {
            // Dynamic Title & Description for General "Help & Support"
            prefilledAppointmentId = null
            b.tvTitle.text = "Help & Support"
            b.tvDescription.text = "We are here to help. Describe your issue and our team will respond within 24 hours."
            b.cardAppointmentContext.visibility = View.GONE
        }

        b.btnSubmitComplaint.setOnClickListener {
            submitComplaint()
        }
    }

    private fun validateForm() {
        val b = binding ?: return
        val subjectFilled = b.etSubject.text?.isNotBlank() == true
        val descriptionFilled = b.etDescription.text?.isNotBlank() == true
        val categorySelected = selectedCategory.isNotBlank()

        b.btnSubmitComplaint.isEnabled = subjectFilled && descriptionFilled && categorySelected
    }

    private fun submitComplaint() {
        val b = binding ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val subject = b.etSubject.text?.toString()?.trim() ?: ""
        val description = b.etDescription.text?.toString()?.trim() ?: ""

        if (subject.isBlank() || description.isBlank() || selectedCategory.isBlank()) {
            return
        }

        val submitterName = when (val booking = sharedViewModel.selectedPatientBooking.value) {
            is DoctorAppointment -> booking.patientNameSnapshot
            is LabTestBooking    -> booking.patientNameSnapshot
            else -> currentUser.displayName ?: "User"
        }

        val role = requireContext()
            .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("user_role", "patients") ?: "patients"

        val complaint = Complaint(
            submittedByUid = currentUser.uid,
            submittedByRole = role,
            submittedByName = submitterName,
            category = selectedCategory,
            subject = subject,
            description = description,
            appointmentId = prefilledAppointmentId,
            againstUserId = prefilledAgainstUserId,
            againstUserRole = prefilledAgainstUserRole
        )

        networkViewModel.saveComplaint(complaint)
    }

    private fun setupObservers() {
        networkViewModel.saveComplaintResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    "Complaint submitted successfully. We will review it shortly.",
                    Toast.LENGTH_LONG
                ).show()
                networkViewModel.resetSaveComplaintResult()
                findNavController().popBackStack()
            } else {
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to submit complaint",
                    Toast.LENGTH_SHORT
                ).show()
                networkViewModel.resetSaveComplaintResult()
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            val b = binding ?: return@observe
            b.btnSubmitComplaint.isEnabled = !isLoading &&
                    b.etSubject.text?.isNotBlank() == true &&
                    b.etDescription.text?.isNotBlank() == true &&
                    selectedCategory.isNotBlank()

            b.btnSubmitComplaint.text = if (isLoading) "Submitting..." else "Submit Complaint"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}