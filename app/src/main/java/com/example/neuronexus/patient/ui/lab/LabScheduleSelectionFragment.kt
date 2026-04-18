package com.example.neuronexus.patient.ui.lab

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
import com.example.neuronexus.databinding.FragmentLabScheduleSelectionBinding
import com.example.neuronexus.patient.adapters.TimeSlotAdapter
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LabScheduleSelectionFragment : Fragment() {

    private var _binding: FragmentLabScheduleSelectionBinding? = null
    private val binding get() = _binding!!

    // SharedViewModel to access selected Lab and Test, and generate time slots
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var currentLab: Lab? = null
    private var currentTest: LabTest? = null

    private lateinit var timeSlotAdapter: TimeSlotAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabScheduleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Adapter with empty list first
        setupTimeSlotsAdapter()

        // Restore previously selected date (if available) or set to today
        val existingDate = sharedViewModel.selectedDate.value
        if (existingDate != null) {
            binding.calendarView.date = existingDate
            updateSelectedDate(existingDate)
            sharedViewModel.selectDate(existingDate)
        } else {
            val calendar = Calendar.getInstance()
            updateSelectedDate(calendar.timeInMillis)
            sharedViewModel.selectDate(calendar.timeInMillis)
        }
        android.util.Log.d("LAB_DEBUG", "selectDate called. selectedLab: ${sharedViewModel.selectedLab.value?.name}, selectedLabTest: ${sharedViewModel.selectedLabTest.value?.testName}")

        setupObservers()
        setupCalendar()
        setupListeners()
    }

    private fun setupObservers() {
        // 1. Lab Info Guard
        sharedViewModel.selectedLab.observe(viewLifecycleOwner) { lab ->
            if (lab != null) {
                currentLab = lab
            } else {
                AlertUtils.showError(requireContext(), "Lab information missing. Please try again.")
                findNavController().popBackStack()
            }
        }

        sharedViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.cardProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            date?.let {
                binding.calendarView.date = it
                updateSelectedDate(it)
            }
        }

        sharedViewModel.selectedTimeSlot.observe(viewLifecycleOwner) { time ->
            selectedTime = time ?: ""
            timeSlotAdapter.setSelectedTimeSlot(time)
        }

        // 2. Lab Test Info Guard
        sharedViewModel.selectedLabTest.observe(viewLifecycleOwner) { test ->
            if (test != null) {
                currentTest = test
            } else {
                AlertUtils.showError(requireContext(), "Test information missing. Please select a test.")
                findNavController().popBackStack()
            }
        }

        // 3. Available Slots (Reusing SharedViewModel logic)
        sharedViewModel.availableTimeSlots.observe(viewLifecycleOwner) { slots ->
            val isLoading = sharedViewModel.loading.value ?: false
            if (slots.isNullOrEmpty()) {
                timeSlotAdapter.updateList(emptyList())
                if (!isLoading) {
                    Toast.makeText(requireContext(), "No slots available for this date", Toast.LENGTH_SHORT).show()
                }
            } else {
                timeSlotAdapter.updateList(slots, sharedViewModel.selectedTimeSlot.value)
            }
        }
    }

    private fun setupCalendar() {
        // Prevent booking dates in the past
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

        binding.btnConfirmSchedule.setOnClickListener {
            if (validateSelection()) {
                findNavController().navigate(R.id.action_lab_schedule_to_patient_selection)
            }
        }
    }

    private fun validateSelection(): Boolean {
        if (currentLab == null || currentTest == null) {
            AlertUtils.showError(requireContext(), "Booking information is missing. Please restart the process.")
            return false
        }

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