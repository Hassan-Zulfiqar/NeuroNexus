package com.example.neuronexus.patient.ui.doctor_discovery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentBookingConfirmationBinding
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.PatientProfile
import com.example.neuronexus.patient.models.Payment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingConfirmationFragment : Fragment() {

    private var _binding: FragmentBookingConfirmationBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by sharedViewModel()
    private val networkViewModel: NetworkViewModel by viewModel()

    private var selectedDoctor: Doctor? = null
    private var selectedDateTimestamp: Long = 0L
    private var selectedTimeSlot: String = ""
    private var selectedPatient: PatientProfile? = null
    private var bookingReason: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        sharedViewModel.selectedDoctor.observe(viewLifecycleOwner) { doctor ->
            selectedDoctor = doctor
            updateUI()
        }

        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            selectedDateTimestamp = date ?: 0L
            updateUI()
        }

        sharedViewModel.selectedTimeSlot.observe(viewLifecycleOwner) { time ->
            selectedTimeSlot = time ?: ""
            updateUI()
        }

        sharedViewModel.selectedPatientProfile.observe(viewLifecycleOwner) { patient ->
            selectedPatient = patient
            updateUI()
        }

        sharedViewModel.bookingReason.observe(viewLifecycleOwner) { reason ->
            bookingReason = reason ?: ""
            updateUI()
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnConfirmBooking.isEnabled = !isLoading
        }

        networkViewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess { bookingId ->
                    showSuccessAndExit(bookingId)
                    networkViewModel.resetBookingState()
                }
                result.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Booking Failed")
                    networkViewModel.resetBookingState()
                }
            }
        }
    }

    private fun updateUI() {
        val doctor = selectedDoctor ?: return

        binding.tvDoctorName.text = "Dr. ${doctor.name}"
        binding.tvSpecialization.text = doctor.specialization

        if (selectedDateTimestamp != 0L && selectedTimeSlot.isNotEmpty()) {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(selectedDateTimestamp))
            binding.tvDateTime.text = "$dateStr • $selectedTimeSlot"
        }

        val patient = selectedPatient
        if (patient != null) {
            val relationText = if (patient.relation.equals("Self", true)) "(Self)" else "(${patient.relation})"
            binding.tvPatientName.text = "${patient.fullName} $relationText"
        }

        binding.tvReason.text = if (bookingReason.isNotEmpty()) "Reason: $bookingReason" else "No reason specified"

        val fee = if (doctor.consultationFee.isNotEmpty()) doctor.consultationFee else "0"
        binding.tvFee.text = "RS $fee"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun confirmBooking() {
        val doctor = selectedDoctor
        val patient = selectedPatient

        if (doctor == null || selectedDateTimestamp == 0L || selectedTimeSlot.isEmpty() || patient == null) {
            AlertUtils.showError(requireContext(), "Incomplete booking details. Please go back and select all fields.")
            return
        }

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date(selectedDateTimestamp))

        val feeAmount = doctor.consultationFee.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

        val calculatedExactTime = try {
            val parseFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
            parseFormat.parse("$formattedDate $selectedTimeSlot")?.time ?: 0L
        } catch (e: Exception) {
            0L
        }

        val booking = DoctorAppointment(
            status = "pending",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),

            patientNameSnapshot = patient.fullName,

            payment = Payment(
                amount = feeAmount,
                paymentStatus = "pending",
                paymentMethod = "PAY_AT_CLINIC",
                transactionDate = System.currentTimeMillis()
            ),

            patientInfo = patient,

            doctorId = doctor.uid,
            doctorName = doctor.name,
            doctorSpecialization = doctor.specialization,
            doctorImageUrl = doctor.profileImageUrl,

            appointmentDate = formattedDate,
            appointmentTime = selectedTimeSlot,
            reasonForVisit = bookingReason,

            // ==========================================
            // Pass the calculated timestamp
            // ==========================================
            exactTimeInMillis = calculatedExactTime,

            // ==========================================
            // Link to previous booking for Rebook
            // ==========================================
            previousBookingId = sharedViewModel.previousBookingId.value
        )

        networkViewModel.bookAppointment(booking)
    }

    private fun showSuccessAndExit(bookingId: String) {
        Toast.makeText(requireContext(), "Booking Confirmed!", Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}