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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
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
import com.example.neuronexus.common.workers.ReminderScheduler

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

    private lateinit var paymentSheet: PaymentSheet
    private var pendingBookingId: String = ""
    private var pendingPaymentIntentId: String = ""
    private var pendingExactTimeInMillis: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Stripe PaymentSheet — must be FIRST
        paymentSheet = PaymentSheet(this) { paymentResult ->
            handlePaymentResult(paymentResult)
        }

        setupObservers()
        setupListeners()
    }

    private fun handlePaymentResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                // Stripe payment confirmed — build Payment and save booking
                val confirmedPayment = Payment(
                    amount = getFeeAmount(),
                    currency = "usd",
                    paymentMethod = "ONLINE",
                    paymentStatus = "paid",
                    transactionDate = System.currentTimeMillis(),
                    stripePaymentIntentId = pendingPaymentIntentId
                )
                saveBookingAfterPayment(confirmedPayment)
            }

            is PaymentSheetResult.Canceled -> {
                Toast.makeText(
                    requireContext(),
                    "Payment cancelled. Your booking was not confirmed.",
                    Toast.LENGTH_LONG
                ).show()
                resetConfirmButton()
                pendingPaymentIntentId = ""
                pendingBookingId = ""
            }

            is PaymentSheetResult.Failed -> {
                Toast.makeText(
                    requireContext(),
                    "Payment failed: ${paymentResult.error.message}",
                    Toast.LENGTH_LONG
                ).show()
                resetConfirmButton()
                pendingPaymentIntentId = ""
                pendingBookingId = ""
            }
        }
    }

    private fun getFeeAmount(): Double {
        val doctor = selectedDoctor ?: return 0.0
        return doctor.consultationFee
            .replace(Regex("[^0-9.]"), "")
            .toDoubleOrNull() ?: 0.0
    }

    private fun resetConfirmButton() {
        binding?.btnConfirmBooking?.isEnabled = true
        binding?.btnConfirmBooking?.text = "Confirm & Book"
    }

    private fun saveBookingAfterPayment(confirmedPayment: Payment) {
        val doctor = selectedDoctor ?: return
        val patient = selectedPatient ?: return

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date(selectedDateTimestamp))

        val calculatedExactTime = try {
            val parseFormat = SimpleDateFormat(
                "dd MMM yyyy hh:mm a",
                Locale.getDefault()
            )
            parseFormat.parse("$formattedDate $selectedTimeSlot")?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
        pendingExactTimeInMillis = calculatedExactTime

        val booking = DoctorAppointment(
            bookingId = if (pendingBookingId.isNotBlank()) pendingBookingId else "",
            status = "pending",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            patientNameSnapshot = patient.fullName,
            payment = confirmedPayment,
            patientInfo = patient,
            doctorId = doctor.uid,
            doctorName = doctor.name,
            doctorSpecialization = doctor.specialization,
            doctorImageUrl = doctor.profileImageUrl,
            appointmentDate = formattedDate,
            appointmentTime = selectedTimeSlot,
            reasonForVisit = bookingReason,
            exactTimeInMillis = calculatedExactTime,
            previousBookingId = sharedViewModel.previousBookingId.value
        )

        networkViewModel.bookAppointment(
            booking = booking,
            confirmedPayment = confirmedPayment
        )
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
                    ReminderScheduler.scheduleAppointmentReminder(
                        context = requireContext().applicationContext,
                        bookingId = bookingId.ifBlank { pendingBookingId },
                        exactTimeInMillis = pendingExactTimeInMillis,
                        title = "Appointment Reminder",
                        message = "Your appointment with Dr. ${selectedDoctor?.name ?: "your doctor"} " +
                                  "is in 30 minutes ($selectedTimeSlot)"
                    )
                    showSuccessAndExit(bookingId)
                    networkViewModel.resetBookingState()
                }
                result.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Booking Failed")
                    networkViewModel.resetBookingState()
                }
            }
        }

        // Payment intent observer — launches Stripe PaymentSheet
        networkViewModel.paymentIntentResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            if (result.isSuccess) {
                val clientSecret = result.getOrNull()?.first ?: ""
                val paymentIntentId = result.getOrNull()?.second ?: ""

                // Store payment intent ID before resetting — needed for handlePaymentResult()
                pendingPaymentIntentId = paymentIntentId

                if (clientSecret.isNotBlank()) {
                    val configuration = PaymentSheet.Configuration(
                        merchantDisplayName = "NuroNexus",
                        allowsDelayedPaymentMethods = false
                    )
                    paymentSheet.presentWithPaymentIntent(
                        paymentIntentClientSecret = clientSecret,
                        configuration = configuration
                    )
                } else {
                    AlertUtils.showError(
                        requireContext(),
                        "Payment setup failed. Please try again."
                    )
                    resetConfirmButton()
                }
            } else {
                AlertUtils.showError(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to initialize payment."
                )
                resetConfirmButton()
            }
            networkViewModel.resetPaymentIntentResult()
        }

        // Payment loading observer
        networkViewModel.paymentLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding?.btnConfirmBooking?.isEnabled = false
                binding?.btnConfirmBooking?.text = "Processing..."
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
        binding?.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding?.btnConfirmBooking?.setOnClickListener {
            confirmBooking()
        }
    }

    private fun confirmBooking() {
        val b = binding ?: return
        val doctor = selectedDoctor
        val patient = selectedPatient

        if (doctor == null || selectedDateTimestamp == 0L ||
            selectedTimeSlot.isEmpty() || patient == null) {
            AlertUtils.showError(
                requireContext(),
                "Incomplete booking details. Please go back and select all fields."
            )
            return
        }

        val isOnlinePayment = b.rbPayOnline.isChecked
        val feeAmount = getFeeAmount()

        b.btnConfirmBooking.isEnabled = false
        b.btnConfirmBooking.text = "Processing..."

        if (isOnlinePayment) {
            // Online payment — fetch payment intent then launch Stripe
            pendingBookingId = java.util.UUID.randomUUID().toString()

            networkViewModel.fetchPaymentIntent(
                amount = feeAmount,
                bookingId = pendingBookingId,
                description = "NuroNexus Doctor Appointment — ${doctor.name}"
            )
            // Observer handles launching PaymentSheet
        } else {
            // Cash at clinic — save directly without Stripe
            val cashPayment = Payment(
                amount = feeAmount,
                currency = "usd",
                paymentMethod = "PAY_AT_CLINIC",
                paymentStatus = "pending_cash",
                transactionDate = System.currentTimeMillis()
            )
            saveBookingAfterPayment(cashPayment)
        }
    }

    private fun showSuccessAndExit(bookingId: String) {
        // Reset button state and pending data
        binding?.btnConfirmBooking?.isEnabled = true
        binding?.btnConfirmBooking?.text = "Confirm & Book"
        pendingPaymentIntentId = ""
        pendingBookingId = ""

        Toast.makeText(
            requireContext(),
            "Booking Confirmed!",
            Toast.LENGTH_LONG
        ).show()

        // Existing navigation — keep as-is
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}