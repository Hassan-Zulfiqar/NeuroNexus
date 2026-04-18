package com.example.neuronexus.doctor.ui.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.activities.NotificationsActivity
import com.example.neuronexus.common.utils.Constant
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorAppointmentDetailBinding
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.Prescription
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DoctorAppointmentDetailFragment : Fragment() {

    private var _binding: FragmentDoctorAppointmentDetailBinding? = null
    private val binding get() = _binding!!

    // Koin ViewModel Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    // Local State
    private var currentAppointment: DoctorAppointment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorAppointmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Header Back Button
        binding.btnBack.setOnClickListener {
            backPressHandling()
        }

        // SwipeRefreshLayout Wiring
        binding.swipeRefreshLayout.setOnRefreshListener {
            sharedViewModel.selectedDoctorAppointment.value?.let { _ ->
                // Full single-appointment fetch from Firebase deferred to future phase.
                // For now, quickly stop the loader.
                binding.swipeRefreshLayout.isRefreshing = false
            } ?: run {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // View Patient History Button
        binding.btnViewPatientTimeline.setOnClickListener {
            currentAppointment?.let {
                findNavController().navigate(R.id.action_detail_to_doctor_patient_timeline)
            }
        }
    }

    private fun setupObservers() {
        // Observe Selected Appointment
        sharedViewModel.selectedDoctorAppointment.observe(viewLifecycleOwner) { appointment ->
            appointment?.let {
                currentAppointment = it
                bindPatientInfo(it)
                bindAppointmentInfo(it)
                setupMode(it)
            }
        }

        // Observe Booking Actions Result
        networkViewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val newStatus = it.getOrNull() ?: ""
                    when (newStatus) {
                        "completed" -> {
                            networkViewModel.resetBookingState()
                            currentAppointment?.let { appointment ->
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Appointment Completed")
                                    .setMessage(
                                        "Would you like to add a prescription for " +
                                                "${appointment.patientNameSnapshot} now?"
                                    )
                                    .setPositiveButton("Add Prescription") { _, _ ->
                                        // Update status in memory before navigating
                                        // Firebase already has "completed" — sync local object
                                        val completedAppointment = appointment.copy(status = "completed")
                                        currentAppointment = completedAppointment
                                        sharedViewModel.selectDoctorAppointment(completedAppointment)
                                        findNavController().navigate(R.id.action_detail_to_add_prescription)
                                    }
                                    .setNegativeButton("Skip") { _, _ ->
                                        Toast.makeText(
                                            requireContext(),
                                            "Appointment marked as complete",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        findNavController().popBackStack()
                                    }
                                    .setCancelable(false)
                                    .show()
                            } ?: run {
                                Toast.makeText(
                                    requireContext(),
                                    "Appointment marked as complete",
                                    Toast.LENGTH_SHORT
                                ).show()
                                backPressHandling()
                            }
                        }
                        "confirmed" -> {
                            Toast.makeText(
                                requireContext(),
                                "Appointment accepted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                            backPressHandling()
                        }
                        "rejected" -> {
                            Toast.makeText(
                                requireContext(),
                                "Appointment rejected",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                            backPressHandling()
                        }
                        "no_show" -> {
                            currentAppointment?.let { apt ->
                                networkViewModel.incrementNoShowCount(apt.accountHolderId)
                            }
                            Toast.makeText(
                                requireContext(),
                                "Marked as no-show",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                            backPressHandling()
                        }
                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Status updated",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                            backPressHandling()
                        }
                    }
                }
                else
                {
                    Toast.makeText(
                        requireContext(),
                        it.exceptionOrNull()?.message ?: "Action failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    networkViewModel.resetBookingState()
                }
            }
        }

        // Observe Prescription Fetch Result
        networkViewModel.prescriptionResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val prescription = it.getOrNull()
                    if (prescription != null) {
                        bindPrescriptionData(prescription)
                    } else {
                        // Prescription fetch returned null — show empty state
                        binding.tvPrescriptionEmpty.visibility = View.VISIBLE
                        binding.btnAddPrescription.visibility = View.VISIBLE
                        binding.layoutPrescriptionDetails.visibility = View.GONE
                    }
                    networkViewModel.resetPrescriptionState()
                } else {
                    // Fetch failed — show error and empty state
                    binding.tvPrescriptionEmpty.visibility = View.VISIBLE
                    binding.btnAddPrescription.visibility = View.GONE
                    binding.layoutPrescriptionDetails.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        it.exceptionOrNull()?.message ?: "Failed to load prescription",
                        Toast.LENGTH_SHORT
                    ).show()
                    networkViewModel.resetPrescriptionState()
                }
            }
        }

        // Observe Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }
    }

    private fun bindPatientInfo(appointment: DoctorAppointment) {
        val patientInfo = appointment.patientInfo

        // Safe extraction & fallbacks
        val fullName = patientInfo.fullName.ifEmpty { "Unknown Patient" }
        val ageGender = if (patientInfo.age.isNotEmpty() && patientInfo.gender.isNotEmpty()) {
            "${patientInfo.age} years • ${patientInfo.gender}"
        } else {
            "Age/Gender not provided"
        }
        val contactNumber = patientInfo.contactNumber.ifEmpty { "No contact number" }

        val initial = if (appointment.patientNameSnapshot.trim().isNotEmpty()) {
            appointment.patientNameSnapshot.trim().substring(0, 1).uppercase(Locale.getDefault())
        } else "?"

        // Bind text
        binding.tvPatientInitial.text = initial
        binding.tvPatientName.text = fullName
        binding.tvPatientAgeGender.text = ageGender
        binding.tvContactNumber.text = contactNumber

        // Accessibility Content Descriptions
        binding.tvPatientInitial.contentDescription = "Patient initial $initial"
        binding.tvPatientName.contentDescription = "Patient name: $fullName"
        binding.tvPatientAgeGender.contentDescription = "Age and gender: $ageGender"
        binding.tvContactNumber.contentDescription = "Contact number: $contactNumber"
    }

    private fun bindAppointmentInfo(appointment: DoctorAppointment) {
        // Safe extraction & fallbacks
        val dateStr = appointment.appointmentDate.ifEmpty { "Date not available" }
        val timeStr = appointment.appointmentTime.ifEmpty { "Time not available" }
        val reasonStr = appointment.reasonForVisit.ifEmpty { "Not specified" }

        // Bind text
        binding.tvAppointmentDate.text = dateStr
        binding.tvAppointmentTime.text = timeStr
        binding.tvReasonForVisit.text = "Reason: $reasonStr"

        // Accessibility Content Descriptions
        binding.tvAppointmentDate.contentDescription = "Appointment date: $dateStr"
        binding.tvAppointmentTime.contentDescription = "Appointment time: $timeStr"
        binding.tvReasonForVisit.contentDescription = "Reason for visit: $reasonStr"

        // Chip Status Logic
        val chipText: String
        val chipColorRes: Int

        when (appointment.status.lowercase()) {
            "pending" -> {
                chipText = "Pending"
                chipColorRes = R.color.yellow
            }
            "confirmed" -> {
                chipText = "Confirmed"
                chipColorRes = R.color.success
            }
            "completed" -> {
                chipText = "Completed"
                chipColorRes = R.color.primary_blue
            }
            "cancelled" -> {
                chipText = "Cancelled"
                chipColorRes = R.color.red
            }
            "no_show" -> {
                chipText = "No Show"
                chipColorRes = R.color.red
            }
            "expired" -> {
                chipText = "Expired"
                chipColorRes = R.color.warning
            }
            else -> {
                chipText = appointment.status
                chipColorRes = R.color.warning
            }
        }

        binding.chipStatus.text = chipText
        binding.chipStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        binding.chipStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), chipColorRes)
        )
        binding.chipStatus.contentDescription = "Appointment status is $chipText"
    }

    private fun setupMode(appointment: DoctorAppointment) {
        when (appointment.status.lowercase()) {
            "pending", "confirmed" -> setupActiveMode(appointment)
            "completed"            -> setupReadOnlyMode(appointment) // prescription shown
            else                   -> setupClosedMode(appointment)   // no prescription
        }
    }

    private fun setupActiveMode(appointment: DoctorAppointment) {
        binding.layoutActionButtons.visibility = View.VISIBLE
        binding.dividerActions.visibility = View.VISIBLE
        binding.layoutPrescription.visibility = View.GONE

        when (appointment.status.lowercase()) {
            "pending" -> {
                binding.btnActionLeft.text = "Reject"
                binding.btnActionLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                binding.btnActionRight.text = "Accept"
                binding.btnActionRight.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.success)
                )
            }
            "confirmed" -> {
                binding.btnActionLeft.text = "No-Show"
                binding.btnActionLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                binding.btnActionRight.text = "Complete"
                binding.btnActionRight.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.primary_blue)
                )
            }
        }

        binding.btnActionLeft.setOnClickListener { handleActionLeft(appointment) }
        binding.btnActionRight.setOnClickListener { handleActionRight(appointment) }

        // Hide patient history button if already inside patient history flow
        if (isOpenedFromPatientTimeline()) {
            binding.btnViewPatientTimeline.visibility = View.GONE
        } else {
            binding.btnViewPatientTimeline.visibility = View.VISIBLE
        }
    }

    private fun setupReadOnlyMode(appointment: DoctorAppointment) {
        // Hide action buttons
        binding.layoutActionButtons.visibility = View.GONE
        binding.dividerActions.visibility = View.GONE

        // Show prescription section
        binding.layoutPrescription.visibility = View.VISIBLE

        if (!appointment.prescriptionId.isNullOrEmpty()) {
            // Prescription exists — fetch it
            binding.tvPrescriptionEmpty.visibility = View.GONE
            binding.btnAddPrescription.visibility = View.GONE
            binding.layoutPrescriptionDetails.visibility = View.GONE // hidden until fetch completes

            // Fetch prescription data
            networkViewModel.fetchPrescription(
                appointment.patientProfileId,
                appointment.prescriptionId!!
            )
        } else {
            // No prescription yet — show empty state and add button
            binding.tvPrescriptionEmpty.visibility = View.VISIBLE
            binding.btnAddPrescription.visibility = View.VISIBLE
            binding.layoutPrescriptionDetails.visibility = View.GONE

            // Ensure Add Prescription button is wired here as well
            binding.btnAddPrescription.setOnClickListener {
                currentAppointment?.let {
                    findNavController().navigate(R.id.action_detail_to_add_prescription)
                }
            }
        }

        // Timeline button visibility
        if (isOpenedFromPatientTimeline()) {
            binding.btnViewPatientTimeline.visibility = View.GONE
        } else {
            binding.btnViewPatientTimeline.visibility = View.VISIBLE
        }
    }

    private fun setupClosedMode(appointment: DoctorAppointment) {
        // Hide action buttons — same as read only
        binding.layoutActionButtons.visibility = View.GONE
        binding.dividerActions.visibility = View.GONE

        // Hide prescription section entirely — no prescription for cancelled/no_show/expired
        binding.layoutPrescription.visibility = View.GONE

        // Hide patient history button if already inside patient history flow
        if (isOpenedFromPatientTimeline()) {
            binding.btnViewPatientTimeline.visibility = View.GONE
        } else {
            binding.btnViewPatientTimeline.visibility = View.VISIBLE
        }
    }

    private fun bindPrescriptionData(prescription: Prescription) {
        binding.layoutPrescriptionDetails.visibility = View.VISIBLE
        binding.tvPrescriptionEmpty.visibility = View.GONE
        binding.btnAddPrescription.visibility = View.GONE

        // Diagnosis
        binding.tvDiagnosis.text = "Diagnosis: ${prescription.diagnosis}"

        // Medications — List<String> joined with bullet points
        val medicationsFormatted = prescription.medications
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
        binding.tvMedications.text = "Medications:\n$medicationsFormatted"

        // Instructions — optional
        if (prescription.instructions.isNotBlank()) {
            binding.tvInstructions.visibility = View.VISIBLE
            binding.tvInstructions.text = "Instructions: ${prescription.instructions}"
        } else {
            binding.tvInstructions.visibility = View.GONE
        }

        // Follow up date — optional Long? formatted to readable string
        if (prescription.followUpDate != null) {
            binding.tvFollowUpDate.visibility = View.VISIBLE
            val formatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(java.util.Date(prescription.followUpDate))
            binding.tvFollowUpDate.text = "Follow-up: $formatted"
        } else {
            binding.tvFollowUpDate.visibility = View.GONE
        }
    }

    private fun handleActionLeft(appointment: DoctorAppointment) {
        when (appointment.status.lowercase()) {
            "pending" -> showConfirmationDialog(
                title = "Reject Appointment",
                message = "Are you sure you want to reject ${appointment.patientNameSnapshot}'s appointment?",
                onConfirm = {
                    currentAppointment = appointment
                    networkViewModel.updateDoctorAppointmentStatus(
                        appointmentId = appointment.bookingId,
                        newStatus = "rejected",
                        accountHolderId = appointment.accountHolderId,
                        patientName = appointment.patientNameSnapshot,
                        doctorName = appointment.doctorName
                    )
                }
            )
            "confirmed" -> {
                if (isAppointmentInFuture(appointment.appointmentDate)) {
                    showBlockingDialog("You can only mark a patient as No-Show on or after the appointment date.")
                } else {
                    showConfirmationDialog(
                        title = "Mark as No-Show",
                        message = "Are you sure you want to mark ${appointment.patientNameSnapshot} as No-Show?",
                        onConfirm = {
                            currentAppointment = appointment
                            networkViewModel.updateDoctorAppointmentStatus(
                                appointmentId = appointment.bookingId,
                                newStatus = "no_show",
                                accountHolderId = appointment.accountHolderId,
                                patientName = appointment.patientNameSnapshot,
                                doctorName = appointment.doctorName
                            )
                        }
                    )
                }
            }
        }
    }

    private fun isOpenedFromPatientTimeline(): Boolean {
        val navController = findNavController()
        val previousDestination = navController.previousBackStackEntry?.destination?.id
        return previousDestination == R.id.navigation_doctor_patient_timeline
    }

    private fun handleActionRight(appointment: DoctorAppointment) {
        when (appointment.status.lowercase()) {
            "pending" -> showConfirmationDialog(
                title = "Accept Appointment",
                message = "Are you sure you want to accept ${appointment.patientNameSnapshot}'s appointment?",
                onConfirm = {
                    currentAppointment = appointment
                    networkViewModel.updateDoctorAppointmentStatus(
                        appointmentId = appointment.bookingId,
                        newStatus = "confirmed",
                        accountHolderId = appointment.accountHolderId,
                        patientName = appointment.patientNameSnapshot,
                        doctorName = appointment.doctorName
                    )
                }
            )
            "confirmed" -> {
                if (isAppointmentInFuture(appointment.appointmentDate)) {
                    showBlockingDialog("You can only mark a patient as Complete on or after the appointment date.")
                } else {
                    showConfirmationDialog(
                        title = "Mark as Complete",
                        message = "Are you sure you want to mark ${appointment.patientNameSnapshot}'s appointment as complete?",
                        onConfirm = {
                            currentAppointment = appointment
                            networkViewModel.updateDoctorAppointmentStatus(
                                appointmentId = appointment.bookingId,
                                newStatus = "completed",
                                accountHolderId = appointment.accountHolderId,
                                patientName = appointment.patientNameSnapshot,
                                doctorName = appointment.doctorName
                            )
                        }
                    )
                }
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBlockingDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Action Not Allowed")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun isAppointmentInFuture(appointmentDate: String): Boolean {
        if (appointmentDate.isEmpty()) return false
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = sdf.parse(appointmentDate)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            date?.after(today) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun backPressHandling() {
        if (Constant.isFromNoti) {
            Constant.isFromNoti = false
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        } else {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check the review status every time we return to this screen
        currentAppointment?.let { appointment ->
            setupMode(appointment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}