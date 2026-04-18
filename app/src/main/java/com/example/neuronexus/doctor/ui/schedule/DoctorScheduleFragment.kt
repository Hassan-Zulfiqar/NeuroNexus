package com.example.neuronexus.doctor.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.Constant
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorScheduleBinding
import com.example.neuronexus.doctor.adapters.DoctorScheduleAdapter
import com.example.neuronexus.patient.models.DoctorAppointment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DoctorScheduleFragment : Fragment() {

    private var _binding: FragmentDoctorScheduleBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var scheduleAdapter: DoctorScheduleAdapter

    // Tracks the appointment currently being acted upon to orchestrate the No-Show count
    private var lastActedAppointment: DoctorAppointment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Constant.isFromNoti = false
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // Ensure data is always fresh when returning to this tab
        fetchData()
    }

    private fun setupUI() {
        // Initialize Adapter with Action Routing
        scheduleAdapter = DoctorScheduleAdapter(
            appointments = emptyList(),
            onActionLeftClick = { appointment ->
                lastActedAppointment = appointment
                when (appointment.status.lowercase()) {
                    "pending" -> showConfirmationDialog(
                        title = "Reject Appointment",
                        message = "Are you sure you want to reject ${appointment.patientNameSnapshot}'s appointment?",
                        onConfirm = { networkViewModel.updateDoctorAppointmentStatus(
                            appointmentId = appointment.bookingId,
                            newStatus = "rejected",
                            accountHolderId = appointment.accountHolderId,
                            patientName = appointment.patientNameSnapshot,
                            doctorName = appointment.doctorName
                        ) }
                    )
                    "confirmed" -> {
                        if (isAppointmentDateInFuture(appointment.appointmentDate)) {
                            showRestrictionDialog("You can only mark a patient as No-Show on or after the appointment date.")
                        } else {
                            showConfirmationDialog(
                                title = "Mark as No-Show",
                                message = "Are you sure you want to mark ${appointment.patientNameSnapshot}'s appointment as No-Show?",
                                onConfirm = { networkViewModel.updateDoctorAppointmentStatus(
                                    appointmentId = appointment.bookingId,
                                    newStatus = "no_show",
                                    accountHolderId = appointment.accountHolderId,
                                    patientName = appointment.patientNameSnapshot,
                                    doctorName = appointment.doctorName
                                ) }
                            )
                        }
                    }
                }
            },
            onActionRightClick = { appointment ->
                lastActedAppointment = appointment
                when (appointment.status.lowercase()) {
                    "pending" -> showConfirmationDialog(
                        title = "Accept Appointment",
                        message = "Are you sure you want to accept ${appointment.patientNameSnapshot}'s appointment?",
                        onConfirm = { networkViewModel.updateDoctorAppointmentStatus(
                            appointmentId = appointment.bookingId,
                            newStatus = "confirmed",
                            accountHolderId = appointment.accountHolderId,
                            patientName = appointment.patientNameSnapshot,
                            doctorName = appointment.doctorName
                        ) }
                    )
                    "confirmed" -> {
                        if (isAppointmentDateInFuture(appointment.appointmentDate)) {
                            showRestrictionDialog("You can only mark a patient as Complete on or after the appointment date.")
                        } else {
                            showConfirmationDialog(
                                title = "Mark as Complete",
                                message = "Are you sure you want to mark ${appointment.patientNameSnapshot}'s appointment as complete?",
                                onConfirm = { networkViewModel.updateDoctorAppointmentStatus(
                                    appointmentId = appointment.bookingId,
                                    newStatus = "completed",
                                    accountHolderId = appointment.accountHolderId,
                                    patientName = appointment.patientNameSnapshot,
                                    doctorName = appointment.doctorName
                                ) }
                            )
                        }
                    }
                }
            },
            onCardClick = { appointment ->
                sharedViewModel.selectDoctorAppointment(appointment)
                findNavController().navigate(R.id.action_schedule_to_detail)
            }
        )

        // Setup RecyclerView
        binding.rvSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun setupObservers() {
        // Observe Appointment List
        networkViewModel.doctorAppointments.observe(viewLifecycleOwner) { result ->
            result.onSuccess { appointments ->
                // Filter only Active (Pending & Confirmed) appointments
                val activeAppointments = appointments.filter {
                    it.status == "pending" || it.status == "confirmed"
                }

                // SORT LOGIC START
                val sortedAppointments = activeAppointments.sortedWith { a1, a2 ->
                    try {
                        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

                        val date1 = dateFormat.parse(a1.appointmentDate)
                        val date2 = dateFormat.parse(a2.appointmentDate)

                        val dateComparison = date1?.compareTo(date2) ?: 0
                        if (dateComparison != 0) {
                            dateComparison
                        } else {
                            // If dates are the same, sort by time
                            val time1 = timeFormat.parse(a1.appointmentTime)
                            val time2 = timeFormat.parse(a2.appointmentTime)
                            time1?.compareTo(time2) ?: 0
                        }
                    } catch (e: Exception) {
                        0 // Graceful fallback: Keep original order if parsing fails
                    }
                }
                // SORT LOGIC END

                if (sortedAppointments.isEmpty()) {
                    binding.rvSchedule.visibility = View.GONE
                    binding.layoutEmptySchedule.visibility = View.VISIBLE
                } else {
                    binding.rvSchedule.visibility = View.VISIBLE
                    binding.layoutEmptySchedule.visibility = View.GONE
                    scheduleAdapter.updateList(sortedAppointments) // Pass the sorted list
                }
            }.onFailure { error ->
                binding.rvSchedule.visibility = View.GONE
                binding.layoutEmptySchedule.visibility = View.VISIBLE
                Toast.makeText(requireContext(), error.message ?: "Failed to load schedule", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe Loading State for Pull-to-Refresh
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observe Status Update Results
        networkViewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                result.onSuccess { status ->
                    when (status) {
                        "completed" -> {
                            networkViewModel.resetBookingState()
                            lastActedAppointment?.let { appointment ->
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Appointment Completed")
                                    .setMessage(
                                        "Would you like to add a prescription for " +
                                                "${appointment.patientNameSnapshot} now?"
                                    )
                                    .setPositiveButton("Add Prescription") { _, _ ->
                                        // Sync local status with Firebase — appointment is now completed
                                        val completedAppointment = appointment.copy(status = "completed")
                                        sharedViewModel.selectDoctorAppointment(completedAppointment)
                                        findNavController().navigate(R.id.action_schedule_to_add_prescription)
                                    }
                                    .setNegativeButton("Skip") { _, _ ->
                                        Toast.makeText(
                                            requireContext(),
                                            "Appointment marked as complete",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .setCancelable(false)
                                    .show()
                            } ?: Toast.makeText(
                                requireContext(),
                                "Appointment marked as complete",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        "confirmed" -> {
                            Toast.makeText(
                                requireContext(),
                                "Appointment accepted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                        }
                        "rejected" -> {
                            Toast.makeText(
                                requireContext(),
                                "Appointment rejected",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                        }
                        "no_show" -> {
                            lastActedAppointment?.let {
                                networkViewModel.incrementNoShowCount(it.accountHolderId)
                            }
                            Toast.makeText(
                                requireContext(),
                                "Marked as no-show",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                        }
                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Status updated",
                                Toast.LENGTH_SHORT
                            ).show()
                            networkViewModel.resetBookingState()
                        }
                    }
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.message ?: "Update failed", Toast.LENGTH_SHORT).show()
                    networkViewModel.resetBookingState()
                }
            }
        }
    }

    private fun fetchData() {
        networkViewModel.fetchDoctorAppointments()
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun isAppointmentDateInFuture(dateString: String): Boolean {
        if (dateString.isEmpty()) return false
        return try {
            val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val appointmentDate = format.parse(dateString) ?: return false

            // Get today's date at midnight for accurate "day-only" comparison
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            appointmentDate.after(today)
        } catch (e: Exception) {
            false // Graceful fallback: allow action if parsing fails
        }
    }

    private fun showRestrictionDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Action Not Allowed")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}