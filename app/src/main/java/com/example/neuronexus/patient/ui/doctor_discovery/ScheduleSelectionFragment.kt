package com.example.neuronexus.patient.ui.doctor_discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentScheduleSelectionBinding
import com.example.neuronexus.models.Doctor
import com.example.neuronexus.patient.adapters.TimeSlotAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleSelectionFragment : Fragment() {

    private var _binding: FragmentScheduleSelectionBinding? = null
    private val binding get() = _binding!!

    // SharedViewModel to get the selected doctor
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var currentDoctor: Doctor? = null

    private lateinit var timeSlotAdapter: TimeSlotAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Adapter with empty list first
        setupTimeSlotsAdapter()

        // Set initial date to today
        val calendar = Calendar.getInstance()
        updateSelectedDate(calendar.timeInMillis)

        // Trigger initial load for today
        sharedViewModel.selectDate(calendar.timeInMillis)

        setupObservers()
        setupCalendar()
        setupListeners()
    }

    private fun setupObservers() {
        // 1. Doctor Info
        sharedViewModel.selectedDoctor.observe(viewLifecycleOwner) { doctor ->
            if (doctor != null) {
                currentDoctor = doctor
            } else {
                AlertUtils.showError(requireContext(), "Doctor information missing.")
                findNavController().popBackStack()
            }
        }

        // 2. Available Slots (Generated from ScheduleParser)
        sharedViewModel.availableTimeSlots.observe(viewLifecycleOwner) { slots ->
            if (slots.isNullOrEmpty()) {
                timeSlotAdapter.updateList(emptyList())
                Toast.makeText(requireContext(), "No slots available for this date", Toast.LENGTH_SHORT).show()
            } else {
                timeSlotAdapter.updateList(slots)
            }
        }
    }

    private fun setupCalendar() {
        binding.calendarView.minDate = System.currentTimeMillis()

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            val timestamp = calendar.timeInMillis
            updateSelectedDate(timestamp)

            // Trigger ViewModel to generate slots for this new date
            sharedViewModel.selectDate(timestamp)
        }
    }

    private fun updateSelectedDate(timestamp: Long) {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        selectedDate = formatter.format(timestamp)
    }

    private fun setupTimeSlotsAdapter() {
        timeSlotAdapter = TimeSlotAdapter(emptyList()) { slot ->
            selectedTime = slot.timeLabel
            sharedViewModel.selectTimeSlot(slot.timeLabel)
        }

        binding.rvTimeSlots.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvTimeSlots.adapter = timeSlotAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirmBooking.setOnClickListener {
            if (validateSelection()) {
                findNavController().navigate(R.id.action_booking_to_patient_selection)
            }
        }
    }

    private fun validateSelection(): Boolean {
        if (selectedDate.isEmpty()) {
            AlertUtils.showError(requireContext(), "Please select a date.")
            return false
        }
        if (selectedTime.isEmpty()) {
            AlertUtils.showError(requireContext(), "Please select a time slot.")
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}