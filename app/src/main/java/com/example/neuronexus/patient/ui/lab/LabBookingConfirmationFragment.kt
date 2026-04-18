package com.example.neuronexus.patient.ui.lab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentLabBookingConfirmationBinding
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import com.example.neuronexus.patient.models.LabTestBooking
import com.example.neuronexus.patient.models.PatientProfile
import com.example.neuronexus.patient.models.Payment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LabBookingConfirmationFragment : Fragment() {

    private var _binding: FragmentLabBookingConfirmationBinding? = null
    private val binding get() = _binding!!

    // CRITICAL FIX: Using sharedViewModel() to fetch the existing data from previous fragments
    private val sharedViewModel: SharedViewModel by sharedViewModel()
    private val networkViewModel: NetworkViewModel by viewModel()

    // Local state to hold the real data retrieved from SharedViewModel
    private var selectedLab: Lab? = null
    private var selectedLabTest: LabTest? = null
    private var selectedDateTimestamp: Long = 0L
    private var selectedTimeSlot: String = ""
    private var selectedPatient: PatientProfile? = null

    // For pricing calculations
    private var testPrice: Double = 0.0
    private var isInstallmentsEnabled: Boolean = false
    private var noOfInstallments: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        sharedViewModel.selectedLab.observe(viewLifecycleOwner) { lab ->
            selectedLab = lab
            updateUI()
        }

        sharedViewModel.selectedLabTest.observe(viewLifecycleOwner) { test ->
            selectedLabTest = test
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

        // Observe Network states
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnConfirmBooking.isEnabled = !isLoading
        }

        networkViewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess {
                    showSuccessAndExit()
                    networkViewModel.resetBookingState()
                }
                result.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Lab Booking Failed")
                    networkViewModel.resetBookingState()
                }
            }
        }
    }

    private fun updateUI() {
        val lab = selectedLab ?: return
        val test = selectedLabTest ?: return

        // 1. Populate Lab & Test Texts
        binding.tvLabName.text = lab.name
        binding.tvTestName.text = test.testName

        // Parse price safely
        testPrice = test.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        binding.tvTestPrice.text = "Rs. $testPrice"
        binding.tvTotalTestPrice.text = "Rs. $testPrice"

        // 2. Installment Logic configuration
        isInstallmentsEnabled = (test.installments.equals("yes", ignoreCase = true))
        noOfInstallments = test.noOfInstallments.toIntOrNull() ?: 0

        if (isInstallmentsEnabled && noOfInstallments > 0) {
            binding.badgeInstallment.visibility = View.VISIBLE
            binding.badgeInstallment.text = "$noOfInstallments Installments Available"
            binding.cardPaymentPlan.visibility = View.VISIBLE
        } else {
            binding.badgeInstallment.visibility = View.GONE
            binding.cardPaymentPlan.visibility = View.GONE
            binding.rbPayFull.isChecked = true
        }

        // 3. Date & Time
        if (selectedDateTimestamp != 0L && selectedTimeSlot.isNotEmpty()) {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(selectedDateTimestamp))
            binding.tvTestDate.text = dateStr
            binding.tvTestTime.text = selectedTimeSlot
        } else {
            binding.tvTestDate.text = "Select Date"
            binding.tvTestTime.text = "Select Time"
        }

        // 4. Patient Info
        val patient = selectedPatient
        if (patient != null) {
            val relationText = if (patient.relation.equals("Self", true)) "(Self)" else "(${patient.relation})"
            binding.tvPatientName.text = "${patient.fullName} $relationText"
            binding.tvPatientAgeGender.text = "${patient.age} yrs, ${patient.gender}"
            binding.tvPatientContact.text = patient.contactNumber
        }

        // Recalculate Initial Amount Due Display
        updateAmountDueDisplay(binding.rbPayInstallment.isChecked)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.rgPaymentPlan.setOnCheckedChangeListener { _, checkedId ->
            updateAmountDueDisplay(checkedId == R.id.rbPayInstallment)
        }

        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun updateAmountDueDisplay(isInstallmentSelected: Boolean) {
        if (isInstallmentSelected && isInstallmentsEnabled && noOfInstallments > 0) {
            val installmentAmount = testPrice / noOfInstallments
            binding.tvAmountDue.text = String.format(Locale.getDefault(), "Rs. %.2f", installmentAmount)
        } else {
            binding.tvAmountDue.text = "Rs. $testPrice"
        }
    }

    private fun confirmBooking() {
        val lab = selectedLab
        val test = selectedLabTest
        val patient = selectedPatient

        if (lab == null || test == null || selectedDateTimestamp == 0L || selectedTimeSlot.isEmpty() || patient == null) {
            AlertUtils.showError(requireContext(), "Incomplete booking details. Please go back and select all fields.")
            return
        }

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date(selectedDateTimestamp))

        // Check if user actively selected installments
        val wantsInstallments = binding.rbPayInstallment.isChecked && isInstallmentsEnabled

        // Build the Payment Model (Dummy values as finalized, waiting for future Online Payment integration)
        val payment = Payment(
            amount = testPrice,
            paymentStatus = "pending",
            paymentMethod = if (binding.rbPayOnline.isChecked) "ONLINE" else "PAY_AT_LAB",
            transactionDate = System.currentTimeMillis()
        )

        val calculatedExactTime = try {
            val parseFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
            parseFormat.parse("$formattedDate $selectedTimeSlot")?.time ?: 0L
        } catch (e: Exception) {
            0L
        }

        // Build the booking payload exactly as designed
        val booking = LabTestBooking(
            bookingId = "", // NetworkViewModel will assign a UUID
            labId = lab.uid,
            labName = lab.name,
            labImageUrl = lab.profilePicUrl ?: "",
            testId = test.id,
            testName = test.testName,
            testType = test.category,
            testDate = formattedDate,
            testTime = selectedTimeSlot,
            patientInfo = patient,
            patientNameSnapshot = patient.fullName,
            payment = payment,

            exactTimeInMillis = calculatedExactTime,

            // ==========================================
            // Link to previous booking for Rebook
            // ==========================================
            previousBookingId = sharedViewModel.previousBookingId.value
        )

        // Pass to the function in NetworkViewModel
        networkViewModel.bookLabTest(
            booking = booking,
            isInstallment = wantsInstallments,
            totalAmount = testPrice,
            numInstallments = noOfInstallments
        )
    }

    private fun showSuccessAndExit() {
        Toast.makeText(requireContext(), "Lab Booking Confirmed!", Toast.LENGTH_LONG).show()
        sharedViewModel.clearBookingState()
        findNavController().popBackStack(R.id.navigation_home, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}