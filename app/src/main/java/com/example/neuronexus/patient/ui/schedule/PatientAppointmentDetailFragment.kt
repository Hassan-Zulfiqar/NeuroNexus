package com.example.neuronexus.patient.ui.schedule

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
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.activities.NotificationsActivity
import com.example.neuronexus.common.utils.Constant
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientAppointmentDetailBinding
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.LabTestBooking
import com.example.neuronexus.patient.models.Prescription
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientAppointmentDetailFragment : Fragment() {

    private var _binding: FragmentPatientAppointmentDetailBinding? = null
    private val binding get() = _binding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var currentBooking: Booking? = null
    private var currentLabReportUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPatientAppointmentDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding?.btnBack?.setOnClickListener {
           backPressHandling()
        }
        binding?.btnReportIssue?.setOnClickListener {
            // Context is naturally preserved in SharedViewModel
            findNavController().navigate(R.id.action_global_to_submit_complaint)
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

    private fun setupObservers() {
        // Observe the selected booking to populate the screen
        sharedViewModel.selectedPatientBooking.observe(viewLifecycleOwner) { booking ->
            booking ?: return@observe
            currentBooking = booking

            bindCommonInfo(booking)

            // Smart-casting based on the Booking type
            when (booking) {
                is DoctorAppointment -> setupDoctorMode(booking)
                is LabTestBooking -> setupLabMode(booking)
            }

            setupActionSection(booking)
        }

        // Observe cancellation results
        networkViewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    "Appointment cancelled successfully",
                    Toast.LENGTH_SHORT
                ).show()
                networkViewModel.resetBookingState()
                backPressHandling()
            } else {
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to cancel",
                    Toast.LENGTH_SHORT
                ).show()
                networkViewModel.resetBookingState()
            }
        }

        // Observe fetched prescription data
        networkViewModel.prescriptionResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            if (result.isSuccess) {
                val prescription = result.getOrNull()
                if (prescription != null) {
                    bindPrescriptionData(prescription)
                } else {
                    b.tvNoPrescription.visibility = View.VISIBLE
                    b.layoutPrescriptionDetails.visibility = View.GONE
                }
            } else {
                b.tvNoPrescription.visibility = View.VISIBLE
                b.layoutPrescriptionDetails.visibility = View.GONE
            }
            // Mandatory reset after consumption
            networkViewModel.resetPrescriptionState()
        }

        networkViewModel.doctorDetails.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            if (result.isSuccess) {
                val doctor = result.getOrNull()
                b.tvDoctorAddress.text = doctor?.clinicAddress?.ifBlank { "Address not available" }
                    ?: "Address not available"
            } else {
                b.tvDoctorAddress.text = "Address not available"
            }
        }

        // Lab details observer — unpacks Result<Lab>
        networkViewModel.labDetails.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            if (result.isSuccess) {
                val lab = result.getOrNull()
                b.tvLabAddress.text = lab?.address?.ifBlank { "Address not available" }
                    ?: "Address not available"
            } else {
                b.tvLabAddress.text = "Address not available"
            }
        }

        // Observe Single Lab Report Result
        networkViewModel.labReportResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            if (result.isSuccess) {
                val report = result.getOrNull()
                if (report != null) {
                    // Save URL locally so the button click can use it instantly
                    currentLabReportUrl = report.fileUrl

                    // Show result summary if available
                    if (report.resultSummary.isNotBlank()) {
                        b.tvResultSummary.visibility = View.VISIBLE
                        b.tvResultSummary.text = report.resultSummary
                    } else {
                        b.tvResultSummary.visibility = View.GONE
                    }

                    // Show or hide view report button based on fileUrl
                    if (report.fileUrl.isNotBlank()) {
                        b.btnViewReport.visibility = View.VISIBLE
                    } else {
                        b.btnViewReport.visibility = View.GONE
                        b.tvNoLabReport.visibility = View.VISIBLE
                        b.tvNoLabReport.text =
                            "Lab report details available but file not uploaded yet"
                        b.layoutLabReportDetails.visibility = View.GONE
                    }
                } else {
                    b.tvNoLabReport.visibility = View.VISIBLE
                    b.layoutLabReportDetails.visibility = View.GONE
                }
            } else {
                b.tvNoLabReport.visibility = View.VISIBLE
                b.layoutLabReportDetails.visibility = View.GONE
            }
            networkViewModel.resetLabReportResult() // Mandatory Reset
        }

        // 1. Observe if the user has already submitted a review
        networkViewModel.reviewExistsResult.observe(viewLifecycleOwner) { exists ->
            exists ?: return@observe
            val b = binding ?: return@observe
            val booking = currentBooking ?: return@observe

            if (exists) {
                // Review already submitted — fetch and show it
                val entityId = when (booking) {
                    is DoctorAppointment -> booking.doctorId
                    is LabTestBooking -> booking.labId
                    else -> ""
                }
                if (entityId.isNotEmpty()) {
                    networkViewModel.fetchEntityReviews(entityId)
                }
                b.btnWriteReview.visibility = View.GONE
                b.layoutExistingReview.visibility = View.VISIBLE
            } else {
                // No review yet — show write review button
                b.btnWriteReview.visibility = View.VISIBLE
                b.layoutExistingReview.visibility = View.GONE
                b.btnWriteReview.setOnClickListener {
                    findNavController().navigate(R.id.action_global_to_write_review)
                }
            }
            networkViewModel.resetReviewExistsResult()
        }

        // 2. Fetch and display the specific review if it exists
        networkViewModel.entityReviews.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe
            val booking = currentBooking ?: return@observe
            val currentUid =
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

            if (result.isSuccess) {
                val reviews = result.getOrNull() ?: emptyList()
                // Find review for THIS specific appointment by this patient
                val myReview = reviews.find {
                    it.appointmentId == booking.bookingId && it.reviewerId == currentUid
                }

                myReview?.let { review ->
                    b.ratingBarDisplay.rating = review.rating
                    if (review.comment.isNotBlank()) {
                        b.tvExistingComment.visibility = View.VISIBLE
                        b.tvExistingComment.text = "\"${review.comment}\""
                    } else {
                        b.tvExistingComment.visibility = View.GONE
                    }
                }
            }
            networkViewModel.resetEntityReviews()
        }
    }

    private fun bindCommonInfo(booking: Booking) {
        val b = binding ?: return

        // Dynamic Status chip configuration
        val (statusText, statusColor) = when (booking.status.lowercase()) {
            "pending" -> Pair("Pending", R.color.warning)
            "confirmed" -> Pair("Confirmed", R.color.success)
            "completed" -> Pair("Completed", R.color.primary_blue)
            "cancelled" -> Pair("Cancelled", R.color.error)
            "no_show" -> Pair("No Show", R.color.warning)
            "expired" -> Pair("Expired", R.color.text_hint)
            "rejected" -> Pair("Rejected", R.color.error)
            else -> Pair(booking.status, R.color.warning)
        }

        b.chipStatus.text = statusText
        b.chipStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), statusColor)
        )
        b.chipStatus.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )
    }

    private fun setupDoctorMode(appointment: DoctorAppointment) {
        val b = binding ?: return

        // Show Doctor card — hide Lab card
        b.cardDoctorInfo.visibility = View.VISIBLE
        b.cardLabInfo.visibility = View.GONE

        // Bind Doctor info
        Glide.with(this)
            .load(appointment.doctorImageUrl.takeIf { it.isNotBlank() })
            .placeholder(R.drawable.doctor)
            .error(R.drawable.doctor)
            .into(b.imgDoctor)

        b.tvDoctorName.text = appointment.doctorName.ifBlank { "Unknown Doctor" }
        b.tvDoctorSpecialization.text =
            appointment.doctorSpecialization.ifBlank { "General Physician" }
        b.tvDoctorAddress.visibility = View.VISIBLE
        b.tvDoctorAddress.text = "Loading address..."
        networkViewModel.fetchDoctorDetails(appointment.doctorId)

        // Bind appointment info
        b.tvDate.text = appointment.appointmentDate
        b.tvTime.text = appointment.appointmentTime
        b.tvReasonOrType.text = appointment.reasonForVisit.ifBlank { "General Consultation" }

        // Prescription section — only for completed appointments
        if (appointment.status.lowercase() == "completed") {
            b.layoutPrescription.visibility = View.VISIBLE
            b.layoutLabReport.visibility = View.GONE

            if (!appointment.prescriptionId.isNullOrEmpty()) {
                // Safe force unwrap because of the isNullOrEmpty check above
                networkViewModel.fetchPrescription(
                    appointment.patientProfileId,
                    appointment.prescriptionId!!
                )
            } else {
                b.tvNoPrescription.visibility = View.VISIBLE
                b.layoutPrescriptionDetails.visibility = View.GONE
            }
        } else {
            b.layoutPrescription.visibility = View.GONE
            b.layoutLabReport.visibility = View.GONE
        }
    }

    private fun setupLabMode(booking: LabTestBooking) {
        val b = binding ?: return

        // Show Lab card — hide Doctor card
        b.cardLabInfo.visibility = View.VISIBLE
        b.cardDoctorInfo.visibility = View.GONE

        // Bind Lab info
        b.tvTestName.text = booking.testName.ifBlank { "Lab Test" }
        b.tvLabName.text = booking.labName.ifBlank { "Unknown Lab" }
        b.tvTestType.text = booking.testType.ifBlank { "" }
        b.tvLabAddress.visibility = View.VISIBLE
        b.tvLabAddress.text = "Loading address..."
        networkViewModel.fetchLabDetails(booking.labId)

        // Bind appointment info
        b.tvDate.text = booking.testDate
        b.tvTime.text = booking.testTime
        b.tvReasonOrType.text = booking.testName.ifBlank { "Lab Test" }

        // Lab report section — only for completed
        if (booking.status.lowercase() == "completed") {
            b.layoutLabReport.visibility = View.VISIBLE
            b.layoutPrescription.visibility = View.GONE

            if (!booking.reportId.isNullOrEmpty()) {
                b.tvNoLabReport.visibility = View.GONE
                b.layoutLabReportDetails.visibility = View.VISIBLE

                // NEW: Trigger the fetch to get summary & URL
                networkViewModel.fetchLabReport(
                    booking.patientProfileId,
                    booking.reportId!!
                )

                b.btnViewReport.setOnClickListener {
                    openReportFile(booking)
                }
            } else {
                b.tvNoLabReport.visibility = View.VISIBLE
                b.layoutLabReportDetails.visibility = View.GONE
            }
        } else {
            b.layoutLabReport.visibility = View.GONE
            b.layoutPrescription.visibility = View.GONE
        }
    }

    // Intent launch using the cached URL
    private fun openReportFile(booking: LabTestBooking) {
        val fileUrl = currentLabReportUrl

        if (!fileUrl.isNullOrBlank()) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(fileUrl)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "No PDF/Image viewer found on this device",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(requireContext(), "Report file not uploaded yet", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun setupActionSection(booking: Booking) {
        val b = binding ?: return
        val status = booking.status.lowercase()

        when {
            status == "pending" || status == "confirmed" -> {
                // Show cancel — hide review
                b.dividerAction.visibility = View.VISIBLE
                b.btnCancel.visibility = View.VISIBLE
                b.layoutReviewAction.visibility = View.GONE
                b.btnCancel.setOnClickListener {
                    handleCancelBooking(booking)
                }
            }

            status == "completed" -> {
                // Show review section — hide cancel (Supports both Doctor and Lab)
                b.dividerAction.visibility = View.VISIBLE
                b.btnCancel.visibility = View.GONE
                b.layoutReviewAction.visibility = View.VISIBLE

                val currentUid =
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val entityId = when (booking) {
                    is DoctorAppointment -> booking.doctorId
                    is LabTestBooking -> booking.labId
                    else -> ""
                }

                if (entityId.isNotEmpty()) {
                    networkViewModel.checkReviewExists(booking.bookingId, entityId, currentUid)
                }
            }

            else -> {
                // Cancelled, expired, no_show, rejected — hide everything
                b.dividerAction.visibility = View.GONE
                b.btnCancel.visibility = View.GONE
                b.layoutReviewAction.visibility = View.GONE
            }
        }
    }

    private fun handleCancelBooking(booking: Booking) {
        // 24 hour window check exactly matching PatientScheduleFragment logic
        val currentTime = System.currentTimeMillis()
        val timeDiff = booking.exactTimeInMillis - currentTime
        val twentyFourHoursInMillis = 24L * 60L * 60L * 1000L

        if (booking.exactTimeInMillis != 0L && timeDiff < twentyFourHoursInMillis) {
            AlertDialog.Builder(requireContext())
                .setTitle("Cannot Cancel")
                .setMessage("Appointments cannot be cancelled within 24 hours of the scheduled time.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Show confirmation dialog for allowed cancellations
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                networkViewModel.cancelBooking(booking)
            }
            .setNegativeButton("Keep", null)
            .setCancelable(false)
            .show()
    }

    private fun bindPrescriptionData(prescription: Prescription) {
        val b = binding ?: return

        b.tvNoPrescription.visibility = View.GONE
        b.layoutPrescriptionDetails.visibility = View.VISIBLE

        // Issued date
        b.tvPrescriptionIssuedDate.text = if (prescription.issuedDate == 0L) {
            "Date not available"
        } else {
            "Issued: ${
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                ).format(Date(prescription.issuedDate))
            }"
        }

        // Diagnosis
        b.tvPrescriptionDiagnosis.text = prescription.diagnosis.ifBlank { "Not specified" }

        // Medications — bullet list
        val medicationsFormatted = prescription.medications
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
        b.tvPrescriptionMedications.text = medicationsFormatted.ifBlank { "No medications listed" }

        // Instructions
        if (prescription.instructions.isBlank()) {
            b.tvPrescriptionInstructions.visibility = View.GONE
        } else {
            b.tvPrescriptionInstructions.visibility = View.VISIBLE
            b.tvPrescriptionInstructions.text = "Instructions: ${prescription.instructions}"
        }

        // Follow up date
        val followUp = prescription.followUpDate
        if (followUp == null) {
            b.tvFollowUpDate.visibility = View.GONE
        } else {
            b.tvFollowUpDate.visibility = View.VISIBLE
            b.tvFollowUpDate.text = "Follow-up: ${
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(followUp))
            }"
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check the review status every time we return to this screen
        currentBooking?.let { booking ->
            setupActionSection(booking)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}