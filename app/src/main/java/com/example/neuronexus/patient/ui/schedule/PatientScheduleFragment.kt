package com.example.neuronexus.patient.ui.schedule

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientScheduleBinding
import com.example.neuronexus.patient.adapters.PatientAppointmentAdapter
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.LabTestBooking
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class PatientScheduleFragment : Fragment() {

    private var _binding: FragmentPatientScheduleBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var adapter: PatientAppointmentAdapter
    // 1. Changed type to unified Booking interface/parent
    private var allAppointments: List<Booking> = emptyList()
    private var currentTab = "UPCOMING" // Track current selection
    private var currentStatusFilter = "all"

    private var pendingRebook: Booking? = null
    private var loadingDialog: android.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        networkViewModel.fetchMyAppointments() // Fetch fresh data
    }

    private fun setupUI() {
        // Initialize Adapter with empty list and click listener
        adapter = PatientAppointmentAdapter(emptyList()) { appointment, action ->
            handleAppointmentAction(appointment, action)
        }

        binding.rvSchedule.layoutManager = LinearLayoutManager(context)
        binding.rvSchedule.adapter = adapter

        // Tab Listeners
        binding.tabUpcoming.setOnClickListener {
            if (currentTab != "UPCOMING") {
                currentTab = "UPCOMING"
                currentStatusFilter = "all"
                updateTabsUI()
                refreshStatusFilterChips()
                filterAndLoadList()
            }
        }

        binding.tabPast.setOnClickListener {
            if (currentTab != "PAST") {
                currentTab = "PAST"
                currentStatusFilter = "all"
                updateTabsUI()
                refreshStatusFilterChips()
                filterAndLoadList()
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        updateTabsUI()
        refreshStatusFilterChips()
    }

    private fun setupObservers() {
        networkViewModel.myAppointments.observe(viewLifecycleOwner) { result ->
            result.onSuccess { bookings ->
                // 2. Remove the Doctor-only filter. We now accept all unified bookings!
                allAppointments = bookings
                filterAndLoadList() // Refresh list based on current tab
            }.onFailure { error ->
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load appointments")
            }
        }

        // ==========================================
        // ADDED: Observe booking cancellation result
        // ==========================================
        networkViewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess {
                    AlertUtils.showInfo(requireContext(), "Booking cancelled successfully")
                    networkViewModel.fetchMyAppointments() // Refresh the list automatically
                    networkViewModel.resetBookingState()
                }
                result.onFailure { error ->
                    AlertUtils.showError(requireContext(), error.message ?: "Failed to cancel booking")
                    networkViewModel.resetBookingState()
                }
            }
        }

        networkViewModel.labDetails.observe(viewLifecycleOwner) { result ->
            val booking = pendingRebook ?: return@observe
            if (booking !is LabTestBooking) return@observe // Safety check

            result.onSuccess { liveLab ->
                sharedViewModel.selectLab(liveLab)
                networkViewModel.fetchLabTestDetails(booking.testId)

            }.onFailure { error ->
                loadingDialog?.dismiss()
                pendingRebook = null
                AlertUtils.showError(requireContext(), error.message ?: "Failed to fetch current lab details")
            }
        }

        networkViewModel.labTestDetails.observe(viewLifecycleOwner) { result ->
            val booking = pendingRebook ?: return@observe
            if (booking !is LabTestBooking) return@observe // Safety check

            loadingDialog?.dismiss() // Finally dismiss the loading dialog

            result.onSuccess { liveLabTest ->
                // Populate SharedViewModel with the FULL fetched LabTest
                sharedViewModel.selectLabTest(liveLabTest)

                // setPreviousBookingId now safely executes AFTER clearBookingState resolved
                sharedViewModel.setPreviousBookingId(booking.bookingId)

                pendingRebook = null // Consume the event

                // Navigate to Lab Schedule (exists in the same nav graph, so ID routing works)
                findNavController(this).navigate(R.id.navigation_lab_schedule)
            }.onFailure { error ->
                pendingRebook = null
                AlertUtils.showError(requireContext(), error.message ?: "Failed to fetch lab test details")
            }
        }
    }

    // 3. Helper to safely extract common fields regardless of the specific Booking type
    private fun extractBookingDetails(booking: Booking): Triple<String, String, String> {
        return when (booking) {
            is DoctorAppointment -> Triple(booking.appointmentDate, booking.appointmentTime, booking.status)
            is LabTestBooking -> Triple(booking.testDate, booking.testTime, booking.status)
            else -> Triple("", "", "") // Fallback for unknown types
        }
    }

    private fun filterAndLoadList() {
        val currentTime = System.currentTimeMillis()
        val selectedStatus = currentStatusFilter.lowercase(Locale.getDefault())

        val filteredList = if (currentTab == "UPCOMING") {
            // UPCOMING Logic:
            allAppointments.filter { booking ->
                val (date, time, status) = extractBookingDetails(booking)
                val timestamp = getAppointmentTimestamp(date, time)
                val isFuture = timestamp > currentTime
                val isActive = status == "pending" || status == "confirmed"
                val isStatusMatch = selectedStatus == "all" || status.equals(selectedStatus, ignoreCase = true)

                isFuture && isActive && isStatusMatch
            }.sortedBy {
                val (date, time, _) = extractBookingDetails(it)
                getAppointmentTimestamp(date, time)
            } // Nearest first
        } else {
            // PAST Logic:
            allAppointments.filter { booking ->
                val (date, time, status) = extractBookingDetails(booking)
                val timestamp = getAppointmentTimestamp(date, time)
                val isPast = timestamp < currentTime
                val isInactive = status != "pending" && status != "confirmed"
                val isStatusMatch = selectedStatus == "all" || status.equals(selectedStatus, ignoreCase = true)

                (isPast || isInactive) && isStatusMatch
            }.sortedByDescending {
                val (date, time, _) = extractBookingDetails(it)
                getAppointmentTimestamp(date, time)
            } // Newest first
        }

        adapter.updateList(filteredList)
    }

    private fun getStatusOptionsForCurrentTab(): List<Pair<String, String>> {
        return if (currentTab == "UPCOMING") {
            listOf(
                "All" to "all",
                "Confirmed" to "confirmed",
                "Pending" to "pending"
            )
        } else {
            listOf(
                "All" to "all",
                "Completed" to "completed",
                "Cancelled" to "cancelled",
                "Expired" to "expired",
                "Rejected" to "rejected"
            )
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    private fun refreshStatusFilterChips() {
        val ctx = requireContext()
        binding.chipGroupStatusFilter.removeAllViews()
        val options = getStatusOptionsForCurrentTab()
        options.forEach { (label, value) ->
            val chip = Chip(ctx).apply {
                text = label
                tag = value
                isCheckable = false
                isCloseIconVisible = false
                chipStartPadding = dpToPx(14f).toFloat()
                chipEndPadding = dpToPx(14f).toFloat()
                textStartPadding = 0f
                textEndPadding = 0f
                chipMinHeight = dpToPx(36f).toFloat()
                chipCornerRadius = dpToPx(18f).toFloat()
                textSize = 13f
                ResourcesCompat.getFont(ctx, R.font.poppins)?.let { typeface = it }
                setOnClickListener {
                    if (currentStatusFilter.equals(value, ignoreCase = true)) return@setOnClickListener
                    currentStatusFilter = value
                    applyStatusChipStyles()
                    filterAndLoadList()
                }
            }
            binding.chipGroupStatusFilter.addView(chip)
        }
        applyStatusChipStyles()
    }

    private fun applyStatusChipStyles() {
        val ctx = requireContext()
        val blue = ContextCompat.getColor(ctx, R.color.primary_blue)
        val white = ContextCompat.getColor(ctx, R.color.text_white)
        val inactiveBg = ContextCompat.getColor(ctx, R.color.colorCard)
        val inactiveText = ContextCompat.getColor(ctx, R.color.textSecondary)
        val strokeColor = ContextCompat.getColor(ctx, R.color.gray_2)
        val strokeW = dpToPx(1f).toFloat()

        for (i in 0 until binding.chipGroupStatusFilter.childCount) {
            val chip = binding.chipGroupStatusFilter.getChildAt(i) as Chip
            val value = chip.tag as? String ?: continue
            val selected = value.equals(currentStatusFilter, ignoreCase = true)
            if (selected) {
                chip.chipBackgroundColor = ColorStateList.valueOf(blue)
                chip.setTextColor(white)
                chip.chipStrokeWidth = 0f
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(inactiveBg)
                chip.setTextColor(inactiveText)
                chip.chipStrokeWidth = strokeW
                chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
            }
        }
    }

    private fun updateTabsUI() {
        val blue = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        val white = ContextCompat.getColor(requireContext(), R.color.text_white)
        val grey = ContextCompat.getColor(requireContext(), R.color.textSecondary)

        if (currentTab == "UPCOMING") {
            // Upcoming Active
            binding.tabUpcoming.setBackgroundResource(R.drawable.bg_white_rounded)
            binding.tabUpcoming.backgroundTintList = ColorStateList.valueOf(blue)
            binding.tabUpcoming.setTextColor(white)

            // Past Inactive
            binding.tabPast.setBackgroundResource(android.R.color.transparent)
            binding.tabPast.backgroundTintList = null
            binding.tabPast.setTextColor(grey)
        } else {
            // Past Active
            binding.tabPast.setBackgroundResource(R.drawable.bg_white_rounded)
            binding.tabPast.backgroundTintList = ColorStateList.valueOf(blue)
            binding.tabPast.setTextColor(white)

            // Upcoming Inactive
            binding.tabUpcoming.setBackgroundResource(android.R.color.transparent)
            binding.tabUpcoming.backgroundTintList = null
            binding.tabUpcoming.setTextColor(grey)
        }
    }

    // 4. Handle unified Booking action clicks safely
    private fun handleAppointmentAction(booking: Booking, action: String) {
        val targetName = when (booking) {
            is DoctorAppointment -> booking.doctorName
            is LabTestBooking -> booking.labName
            else -> "Unknown Provider"
        }

        when (action) {
            "VIEW_DETAIL" -> {
                // Set the exact booking that was tapped
                sharedViewModel.selectPatientBooking(booking)

                // Navigate safely to the new Detail Fragment
                findNavController(this).navigate(
                    R.id.action_global_to_patient_appointment_detail
                )
            }
            "CANCEL" -> {
                // ==========================================
                // Existing 24-Hour Check and Cancellation Logic
                // ==========================================
                val currentTime = System.currentTimeMillis()
                val timeDiff = booking.exactTimeInMillis - currentTime
                val twentyFourHoursInMillis = 24L * 60 * 60 * 1000

                // If exactTimeInMillis is 0L, it's a legacy booking. We fail-safe to allow cancellation.
                // Otherwise, we enforce the 24-hour rule.
                if (booking.exactTimeInMillis != 0L && timeDiff < twentyFourHoursInMillis) {
                    AlertUtils.showError(
                        requireContext(),
                        "Appointments cannot be cancelled within 24 hours of the scheduled time."
                    )
                } else {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cancel Appointment")
                        .setMessage("Are you sure you want to cancel your appointment with $targetName?")
                        .setPositiveButton("Yes") { dialog, _ ->
                            networkViewModel.cancelBooking(booking)
                            dialog.dismiss()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
            "RESCHEDULE" -> {
                // Existing Reschedule logic...
                sharedViewModel.clearBookingState()

                when (booking) {
                    is DoctorAppointment -> {
                        // Immediate explicit Intent to DoctorDiscoveryActivity
                        val intent = android.content.Intent(requireContext(), com.example.neuronexus.patient.activities.DoctorDiscoveryActivity::class.java).apply {
                            putExtra("NAVIGATE_TO", "SCHEDULE")
                            putExtra("DOCTOR_ID", booking.doctorId)
                            putExtra("PREVIOUS_BOOKING_ID", booking.bookingId)
                        }
                        startActivity(intent)
                    }
                    is LabTestBooking -> {
                        pendingRebook = booking
                        android.util.Log.d("LAB_DEBUG", "1. Reschedule clicked. labId: ${booking.labId}, testId: '${booking.testId}'")
                        loadingDialog = android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Fetching Details")
                            .setMessage("Please wait while we retrieve current availability...")
                            .setCancelable(false)
                            .show()

                        networkViewModel.fetchLabDetails(booking.labId)
                    }
                }
            }
        }
    }

    private fun getAppointmentTimestamp(date: String, time: String): Long {
        return try {
            val format = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
            val dateObj = format.parse("$date $time")
            dateObj?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}