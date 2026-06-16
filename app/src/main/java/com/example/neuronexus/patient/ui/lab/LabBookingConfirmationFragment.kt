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
import com.example.neuronexus.patient.adapters.CartTestAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import androidx.appcompat.app.AlertDialog
import com.example.neuronexus.common.workers.ReminderScheduler

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

    // Lab-level installment state — replaces test-level variables
    private var labOffersInstallments: Boolean = false
    private var labMaxInstallments: Int = 0
    private var selectedInstallmentsCount: Int = 0  // 0 = full payment

    // Stripe PaymentSheet integration
    private lateinit var paymentSheet: PaymentSheet
    private var pendingBookingId: String = ""
    private var stripePaymentIntentId: String = ""  // Store for later use in handlePaymentResult()
    private var pendingExactTimeInMillis: Long = 0L
    private var pendingPlanStartTime: Long = 0L
    private var pendingInstallmentAmount: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabBookingConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Stripe PaymentSheet — must be done in onViewCreated
        paymentSheet = PaymentSheet(this) { paymentResult ->
            handlePaymentResult(paymentResult)
        }

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
                    val cartTests = sharedViewModel.cartTests.value ?: emptyList()
                    val testsSummary = if (cartTests.size > 1) {
                        "${cartTests.size} tests"
                    } else {
                        cartTests.firstOrNull()?.testName
                            ?: selectedLabTest?.testName
                            ?: "your lab test"
                    }
                    ReminderScheduler.scheduleAppointmentReminder(
                        context = requireContext().applicationContext,
                        bookingId = pendingBookingId,
                        exactTimeInMillis = pendingExactTimeInMillis,
                        title = "Lab Test Reminder",
                        message = "You have $testsSummary at ${selectedLab?.name ?: "the lab"} " +
                                  "in 30 minutes ($selectedTimeSlot)"
                    )

                    // Schedule 1-day-before reminders for installments #2 onwards (#1 is pre-paid)
                    val isInstallment = binding?.rbPayInstallment?.isChecked == true && labOffersInstallments
                    if (isInstallment && selectedInstallmentsCount > 1) {
                        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                        val labDisplayName = selectedLab?.name ?: "the lab"

                        for (i in 2..selectedInstallmentsCount) {
                            val dueDate = pendingPlanStartTime + ((i - 1) * thirtyDaysMs)
                            android.util.Log.d(
                                "InstallmentReminder",
                                "Installment $i — due: ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(dueDate))}" +
                                ", reminder: ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(dueDate - 24 * 60 * 60 * 1000))}" +
                                ", delay hours: ${(dueDate - 24 * 60 * 60 * 1000 - System.currentTimeMillis()) / (1000 * 60 * 60)}"
                            )
                            ReminderScheduler.scheduleInstallmentReminder(
                                context = requireContext().applicationContext,
                                bookingId = pendingBookingId,
                                installmentNumber = i,
                                dueDateMillis = dueDate,
                                title = "Installment Payment Due Tomorrow",
                                message = "Installment $i of $selectedInstallmentsCount " +
                                          "($${String.format("%.2f", pendingInstallmentAmount)}) " +
                                          "for your $labDisplayName booking is due tomorrow"
                            )
                        }
                    }

                    showSuccessAndExit()
                    networkViewModel.resetBookingState()
                }
                result.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Lab Booking Failed")
                    networkViewModel.resetBookingState()
                }
            }
        }

        // Payment intent observer — launches Stripe PaymentSheet
        networkViewModel.paymentIntentResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val b = binding ?: return@observe

            if (result.isSuccess) {
                val pair = result.getOrNull()
                val clientSecret = pair?.first ?: ""
                // Store payment intent ID before resetting — needed by handlePaymentResult() later
                stripePaymentIntentId = pair?.second ?: ""

                if (clientSecret.isNotBlank()) {
                    // Launch Stripe PaymentSheet
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
                    b.btnConfirmBooking.isEnabled = true
                    b.btnConfirmBooking.text = "Confirm & Book"
                }
            } else {
                AlertUtils.showError(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to initialize payment."
                )
                b.btnConfirmBooking.isEnabled = true
                b.btnConfirmBooking.text = "Confirm & Book"
            }
            networkViewModel.resetPaymentIntentResult()
        }

        // Payment loading observer
        networkViewModel.paymentLoading.observe(viewLifecycleOwner) { isLoading ->
            val b = binding ?: return@observe
            if (isLoading) {
                b.btnConfirmBooking.isEnabled = false
                b.btnConfirmBooking.text = "Processing..."
            }
        }
    }

    private fun updateUI() {
        val lab = selectedLab ?: return

        // Bind lab name
        binding.tvLabName.text = lab.name

        // Handle cart tests vs single test
        val cartTests = sharedViewModel.cartTests.value ?: emptyList()

        if (cartTests.size > 1) {
            // Multiple tests — show RecyclerView
            binding.rvConfirmationTests.visibility = View.VISIBLE
            binding.tvTestName.visibility = View.GONE
            binding.tvTestPrice.visibility = View.GONE

            val confirmAdapter = CartTestAdapter(
                tests = cartTests.toMutableList(),
                onRemoveClick = { /* no-op — read only in confirmation */ }
            )
            binding.rvConfirmationTests.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = confirmAdapter
            }

            val total = sharedViewModel.getCartTotal()
            binding.tvTotalTestPrice.text = if (total > 0) {
                "Rs. ${String.format("%,.0f", total)}"
            } else {
                "To be confirmed"
            }

            // Setup installment UI for multiple tests
            setupInstallmentUI()

        } else if (cartTests.size == 1) {
            // Single test from cart — use cart data
            binding.rvConfirmationTests.visibility = View.GONE
            binding.tvTestName.visibility = View.VISIBLE
            binding.tvTestPrice.visibility = View.VISIBLE
            binding.tvTestName.text = cartTests.first().testName
            val price = cartTests.first().price
            binding.tvTestPrice.text = if (price > 0) {
                "Rs. ${String.format("%,.0f", price)}"
            } else {
                "Price: TBD"
            }
            binding.tvTotalTestPrice.text = binding.tvTestPrice.text

            // Handle installments for single test
            testPrice = price
            setupInstallmentUI()

        } else {
            // No cart tests — fallback to old selectedLabTest flow
            binding.rvConfirmationTests.visibility = View.GONE
            binding.tvTestName.visibility = View.VISIBLE
            binding.tvTestPrice.visibility = View.VISIBLE

            val test = selectedLabTest ?: return

            binding.tvTestName.text = test.testName

            // Parse price safely
            testPrice = test.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            binding.tvTestPrice.text = "Rs. $testPrice"
            binding.tvTotalTestPrice.text = "Rs. $testPrice"

            // Setup installment UI for single test
            setupInstallmentUI()
        }

        // 3. Date & Time (keep existing logic)
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

    private fun setupInstallmentUI() {
        val b = binding ?: return
        val lab = sharedViewModel.selectedLab.value
        val totalAmount = sharedViewModel.getCartTotal()

        // Read lab-level installment settings
        labOffersInstallments = lab?.offersInstallments ?: false
        labMaxInstallments = lab?.maxInstallments ?: 0
        val minBillForInstallment = lab?.minBillForInstallment ?: 0.0

        // Installment available only if:
        // 1. Lab offers installments
        // 2. Max installments > 0
        // 3. Total amount meets minimum threshold
        val canUseInstallment = labOffersInstallments
            && labMaxInstallments > 0
            && (minBillForInstallment <= 0.0 || totalAmount >= minBillForInstallment)

        if (canUseInstallment) {
            b.badgeInstallment.visibility = View.VISIBLE
            b.badgeInstallment.text = "Up to $labMaxInstallments installments available"
            b.cardPaymentPlan.visibility = View.VISIBLE

            // Default to full payment
            b.rbPayFull.isChecked = true
            selectedInstallmentsCount = 0

        } else {
            b.badgeInstallment.visibility = View.GONE
            b.cardPaymentPlan.visibility = View.GONE
            b.rbPayFull.isChecked = true
            selectedInstallmentsCount = 0
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.rgPaymentPlan.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPayInstallment) {
                if (labMaxInstallments >= 2) {
                    showInstallmentCountPicker()
                } else {
                    // Lab only offers 1 — treat as full
                    binding.rbPayFull.isChecked = true
                    selectedInstallmentsCount = 0
                }
            } else {
                selectedInstallmentsCount = 0
                binding.rbPayInstallment.text = "Pay in Installments"
                updateAmountDueDisplay(false)
            }
        }

        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun showInstallmentCountPicker() {
        val b = binding ?: return

        // Build number range 2 to labMaxInstallments
        val installmentOptions = (2..labMaxInstallments).map { "$it installments" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Number of Installments")
            .setItems(installmentOptions) { _, index ->
                // index 0 = 2 installments, index 1 = 3 installments, etc.
                selectedInstallmentsCount = index + 2
                val totalAmount = sharedViewModel.getCartTotal()
                val perInstallment = totalAmount / selectedInstallmentsCount
                b.rbPayInstallment.text =
                    "Pay in $selectedInstallmentsCount installments " +
                    "(Rs. ${String.format("%.2f", perInstallment)} each)"
                updateAmountDueDisplay(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // User cancelled — revert to full payment
                b.rbPayFull.isChecked = true
                selectedInstallmentsCount = 0
            }
            .show()
    }

    private fun handlePaymentResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                // Payment succeeded — save booking with Stripe payment data
                // Use the stripePaymentIntentId stored in observer before reset

                val totalAmount = sharedViewModel.getCartTotal().let {
                    if (it > 0) it else selectedLabTest?.price
                        ?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
                }

                val isInstallment = binding?.rbPayInstallment?.isChecked == true
                val amountPaid = if (isInstallment && selectedInstallmentsCount > 0) {
                    totalAmount / selectedInstallmentsCount
                } else {
                    totalAmount
                }

                val confirmedPayment = Payment(
                    amount = amountPaid,
                    currency = "usd",
                    paymentMethod = "ONLINE",
                    paymentStatus = "paid",
                    transactionDate = System.currentTimeMillis(),
                    stripePaymentIntentId = stripePaymentIntentId  // Use stored value
                )

                saveBookingAfterPayment(confirmedPayment)
            }

            is PaymentSheetResult.Canceled -> {
                Toast.makeText(
                    requireContext(),
                    "Payment cancelled. Your booking was not confirmed.",
                    Toast.LENGTH_LONG
                ).show()
                binding?.btnConfirmBooking?.text = "Confirm & Book"
                binding?.btnConfirmBooking?.isEnabled = true
            }

            is PaymentSheetResult.Failed -> {
                Toast.makeText(
                    requireContext(),
                    "Payment failed: ${paymentResult.error.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding?.btnConfirmBooking?.text = "Confirm & Book"
                binding?.btnConfirmBooking?.isEnabled = true
            }
        }
    }

    private fun updateAmountDueDisplay(isInstallmentSelected: Boolean) {
        if (isInstallmentSelected && labOffersInstallments && selectedInstallmentsCount > 0) {
            val totalAmount = sharedViewModel.getCartTotal()
            val installmentAmount = totalAmount / selectedInstallmentsCount
            binding.tvAmountDue.text = String.format(Locale.getDefault(), "Rs. %.2f", installmentAmount)
        } else {
            val totalAmount = sharedViewModel.getCartTotal()
            binding.tvAmountDue.text = "Rs. $totalAmount"
        }
    }

    private fun confirmBooking() {
        val b = binding ?: return
        val lab = selectedLab
        val patient = selectedPatient

        if (lab == null || selectedDateTimestamp == 0L ||
            selectedTimeSlot.isEmpty() || patient == null) {
            AlertUtils.showError(
                requireContext(),
                "Incomplete booking details. Please go back and select all fields."
            )
            return
        }

        val isOnlinePayment = b.rbPayOnline.isChecked
        val isInstallment = b.rbPayInstallment.isChecked && labOffersInstallments

        // Validate installment count selected if installment chosen
        if (isInstallment && selectedInstallmentsCount < 2) {
            AlertUtils.showError(
                requireContext(),
                "Please select a valid number of installments."
            )
            return
        }

        val totalAmount = sharedViewModel.getCartTotal().let {
            if (it > 0) it else selectedLabTest?.price
                ?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
        }

        if (isOnlinePayment) {
            // Online payment — fetch payment intent first then launch Stripe
            val amountToCharge = if (isInstallment && selectedInstallmentsCount > 0) {
                totalAmount / selectedInstallmentsCount  // First installment only
            } else {
                totalAmount
            }

            // Generate booking ID now — used for payment intent reference
            pendingBookingId = java.util.UUID.randomUUID().toString()

            b.btnConfirmBooking.isEnabled = false
            b.btnConfirmBooking.text = "Processing Payment..."

            networkViewModel.fetchPaymentIntent(
                amount = amountToCharge,
                bookingId = pendingBookingId,
                description = if (isInstallment) {
                    "NuroNexus Lab Booking - Installment 1 of $selectedInstallmentsCount"
                } else {
                    "NuroNexus Lab Booking"
                }
            )
            // Observer in setupObservers() handles launching PaymentSheet
        } else {
            // Cash payment — save directly without Stripe
            val cashPayment = Payment(
                amount = totalAmount,
                currency = "usd",
                paymentMethod = "PAY_AT_LAB",
                paymentStatus = "pending_cash",
                transactionDate = System.currentTimeMillis()
            )
            saveBookingAfterPayment(cashPayment)
        }
    }

    private fun saveBookingAfterPayment(confirmedPayment: Payment) {
        val b = binding ?: return
        val lab = selectedLab ?: return
        val patient = selectedPatient ?: return

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date(selectedDateTimestamp))

        val cartTests = sharedViewModel.cartTests.value ?: emptyList()
        val hasMultipleTests = cartTests.isNotEmpty()

        val totalAmount = sharedViewModel.getCartTotal().let {
            if (it > 0) it else selectedLabTest?.price
                ?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0
        }

        val isInstallment = b.rbPayInstallment.isChecked && labOffersInstallments

        val calculatedExactTime = try {
            val parseFormat = SimpleDateFormat(
                "dd MMM yyyy hh:mm a", Locale.getDefault()
            )
            parseFormat.parse("$formattedDate $selectedTimeSlot")?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
        pendingExactTimeInMillis = calculatedExactTime
        pendingPlanStartTime = System.currentTimeMillis()
        pendingInstallmentAmount = if (selectedInstallmentsCount > 0 && totalAmount > 0) {
            totalAmount / selectedInstallmentsCount
        } else {
            0.0
        }

        val booking = LabTestBooking(
            bookingId = if (pendingBookingId.isNotBlank()) pendingBookingId
                        else "",
            labId = lab.uid,
            labName = lab.name,
            labImageUrl = lab.profilePicUrl ?: "",
            testId = if (hasMultipleTests) cartTests.firstOrNull()?.testId ?: ""
                     else selectedLabTest?.id ?: "",
            testName = if (hasMultipleTests && cartTests.size > 1)
                           "${cartTests.size} Tests"
                       else if (hasMultipleTests)
                           cartTests.firstOrNull()?.testName ?: ""
                       else selectedLabTest?.testName ?: "",
            testType = if (hasMultipleTests)
                           cartTests.firstOrNull()?.testType ?: ""
                       else selectedLabTest?.category ?: "",
            testDate = formattedDate,
            testTime = selectedTimeSlot,
            patientInfo = patient,
            patientNameSnapshot = patient.fullName,
            payment = confirmedPayment,
            exactTimeInMillis = calculatedExactTime,
            previousBookingId = sharedViewModel.previousBookingId.value,
            tests = cartTests,
            totalAmount = totalAmount,
            offersInstallments = labOffersInstallments,
            maxInstallments = labMaxInstallments,
            selectedInstallments = selectedInstallmentsCount,
            installmentAmount = if (selectedInstallmentsCount > 0 && totalAmount > 0) {
                totalAmount / selectedInstallmentsCount
            } else {
                0.0
            }
        )

        networkViewModel.bookLabTest(
            booking = booking,
            isInstallment = isInstallment,
            totalAmount = totalAmount,
            numInstallments = selectedInstallmentsCount
        )
    }

    private fun showSuccessAndExit() {
        val b = binding ?: return

        // Show installment schedule if applicable
        if (selectedInstallmentsCount > 0 && labOffersInstallments) {
            showInstallmentScheduleDialog {
                // After dialog dismissed — navigate home
                navigateHome()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Lab Booking Confirmed!",
                Toast.LENGTH_LONG
            ).show()
            navigateHome()
        }
    }

    private fun navigateHome() {
        sharedViewModel.clearCart()
        sharedViewModel.clearBookingState()
        // Clear payment state
        stripePaymentIntentId = ""
        pendingBookingId = ""
        findNavController().popBackStack(R.id.navigation_home, false)
    }

    private fun showInstallmentScheduleDialog(onDismiss: () -> Unit) {
        val totalAmount = sharedViewModel.getCartTotal()
        val installmentAmount = if (selectedInstallmentsCount > 0) {
            totalAmount / selectedInstallmentsCount
        } else 0.0

        val currentTime = System.currentTimeMillis()
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

        // Build schedule message
        val scheduleBuilder = StringBuilder()
        scheduleBuilder.append("Your installment plan:\n\n")

        for (i in 1..selectedInstallmentsCount) {
            val dueDate = currentTime + ((i - 1) * thirtyDaysMillis)
            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(dueDate))
            val status = if (i == 1) "✓ Paid" else "Pending"
            scheduleBuilder.append(
                "Installment $i: Rs. ${String.format("%.2f", installmentAmount)}" +
                " — $formattedDate — $status\n"
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Booking Confirmed!")
            .setMessage(scheduleBuilder.toString())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}