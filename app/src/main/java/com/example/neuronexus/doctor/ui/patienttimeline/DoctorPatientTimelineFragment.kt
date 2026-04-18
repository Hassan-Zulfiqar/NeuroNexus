package com.example.neuronexus.doctor.ui.patienttimeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorPatientTimelineBinding
import com.example.neuronexus.doctor.adapters.DoctorAppointmentHistoryAdapter
import com.example.neuronexus.patient.models.DoctorAppointment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.example.neuronexus.common.utils.Constant
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class DoctorPatientTimelineFragment : Fragment() {

    private var _binding: FragmentDoctorPatientTimelineBinding? = null
    private val binding get() = _binding!!

    // Koin ViewModel Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var patientHistoryAdapter: DoctorAppointmentHistoryAdapter
    private var selectedAppointment: DoctorAppointment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorPatientTimelineBinding.inflate(inflater, container, false)
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
        fetchData()
    }

    private fun fetchData() {
        networkViewModel.fetchDoctorAppointments()
    }

    private fun setupUI() {
        // Back Navigation
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Capture selected appointment BEFORE any list interaction overrides it in the SharedViewModel
        if (selectedAppointment == null) {
            selectedAppointment = sharedViewModel.selectedDoctorAppointment.value
        }

        // Set Header Title
        val patientName = selectedAppointment?.patientNameSnapshot ?: "Patient"
        binding.tvPatientName.text = "Timeline for $patientName"

        // Initialize Adapter
        patientHistoryAdapter = DoctorAppointmentHistoryAdapter(
            appointments = emptyList(),
            onViewDetailsClick = { appointment ->
                sharedViewModel.selectDoctorAppointment(appointment)
                findNavController().navigate(R.id.action_doctor_patient_timeline_to_detail)
            }
        )

        // Setup RecyclerView
        binding.rvPatientHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = patientHistoryAdapter
            isNestedScrollingEnabled = true
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun setupObservers() {
        // Observe Appointments Data
        networkViewModel.doctorAppointments.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val allAppointments = it.getOrNull() ?: emptyList()

                    // Filter: Specific patient with this Doctor only
                    val specificPatientAppointments = allAppointments
                        .filter { appointment ->
                            appointment.accountHolderId == selectedAppointment?.accountHolderId &&
                                    appointment.patientProfileId == selectedAppointment?.patientProfileId
                        }

                    // Sort 1: Upcoming (Ascending - Soonest first)
                    val upcoming = specificPatientAppointments
                        .filter { it.status.lowercase() in listOf("pending", "confirmed") }
                        .sortedBy { appointment ->
                            try {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .parse(appointment.appointmentDate)
                            } catch (e: Exception) {
                                null
                            }
                        }

                    // Sort 2: Past (Descending - Most recent first)
                    val past = specificPatientAppointments
                        .filter { it.status.lowercase() in listOf("completed", "cancelled", "no_show", "expired") }
                        .sortedByDescending { appointment ->
                            try {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .parse(appointment.appointmentDate)
                            } catch (e: Exception) {
                                null
                            }
                        }

                    // Combine for the ultimate clinical timeline
                    val patientHistory = upcoming + past

                    // Toggle Visibility
                    if (patientHistory.isEmpty()) {
                        binding.rvPatientHistory.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.rvPatientHistory.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        patientHistoryAdapter.updateList(patientHistory)
                    }
                } else {
                    // Handle Failure
                    binding.rvPatientHistory.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        it.exceptionOrNull()?.message ?: "Failed to load patient timeline",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Observe Loading State for SwipeRefreshLayout
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}