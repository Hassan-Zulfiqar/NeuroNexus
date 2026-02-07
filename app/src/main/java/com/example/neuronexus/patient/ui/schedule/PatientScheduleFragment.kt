package com.example.neuronexus.patient.ui.schedule

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentPatientScheduleBinding
import com.example.neuronexus.patient.adapters.PatientAppointmentAdapter
import com.example.neuronexus.patient.models.DoctorAppointment
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class PatientScheduleFragment : Fragment() {

    private var _binding: FragmentPatientScheduleBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by viewModel()

    private lateinit var adapter: PatientAppointmentAdapter
    private var allAppointments: List<DoctorAppointment> = emptyList()
    private var currentTab = "UPCOMING" // Track current selection

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
                updateTabsUI()
                filterAndLoadList()
            }
        }

        binding.tabPast.setOnClickListener {
            if (currentTab != "PAST") {
                currentTab = "PAST"
                updateTabsUI()
                filterAndLoadList()
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupObservers() {
        networkViewModel.myAppointments.observe(viewLifecycleOwner) { result ->
            result.onSuccess { bookings ->
                // Filter only DoctorAppointments and store them
                allAppointments = bookings.filterIsInstance<DoctorAppointment>()
                filterAndLoadList() // Refresh list based on current tab
            }.onFailure { error ->
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load appointments")
            }
        }
    }

    private fun filterAndLoadList() {
        val currentTime = System.currentTimeMillis()
        val filteredList = if (currentTab == "UPCOMING") {
            // UPCOMING Logic:
            // 1. Timestamp must be in future (> Now)
            // 2. Status must NOT be cancelled or completed
            allAppointments.filter { appointment ->
                val timestamp = getAppointmentTimestamp(appointment.appointmentDate, appointment.appointmentTime)
                val isFuture = timestamp > currentTime
                val isActive = appointment.status != "cancelled" && appointment.status != "completed"

                isFuture && isActive
            }.sortedBy { getAppointmentTimestamp(it.appointmentDate, it.appointmentTime) } // Nearest first
        } else {
            // PAST Logic:
            // 1. Timestamp is in past (< Now)
            // OR
            // 2. Status is cancelled or completed (regardless of time)
            allAppointments.filter { appointment ->
                val timestamp = getAppointmentTimestamp(appointment.appointmentDate, appointment.appointmentTime)
                val isPast = timestamp < currentTime
                val isInactive = appointment.status == "cancelled" || appointment.status == "completed"

                isPast || isInactive
            }.sortedByDescending { getAppointmentTimestamp(it.appointmentDate, it.appointmentTime) } // Newest first
        }

        adapter.updateList(filteredList)
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

    private fun handleAppointmentAction(appointment: DoctorAppointment, action: String) {
        if (action == "CANCEL") {
            AlertUtils.showInfo(requireContext(), "Cancel requested for ${appointment.doctorName}. (Logic pending)")
        } else if (action == "RESCHEDULE") {
            AlertUtils.showInfo(requireContext(), "Reschedule requested for ${appointment.doctorName}. (Logic pending)")
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