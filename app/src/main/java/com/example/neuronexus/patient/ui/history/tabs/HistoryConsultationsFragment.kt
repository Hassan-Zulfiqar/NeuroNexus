package com.example.neuronexus.patient.ui.history.tabs

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
import com.example.neuronexus.databinding.FragmentHistoryConsultationsBinding
import com.example.neuronexus.patient.adapters.PatientHistoryConsultationAdapter
import com.example.neuronexus.patient.models.DoctorAppointment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryConsultationsFragment : Fragment() {

    private var _binding: FragmentHistoryConsultationsBinding? = null
    private val binding get() = _binding

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var consultationAdapter: PatientHistoryConsultationAdapter? = null

    private var lastConsultationBaseList: List<DoctorAppointment> = emptyList()
    private var hasReceivedConsultationHistoryResult = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryConsultationsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    private fun fetchData() {
        networkViewModel.fetchMyAppointments()
    }

    private fun setupUI() {
        val b = binding ?: return

        consultationAdapter = PatientHistoryConsultationAdapter(
            appointments = emptyList(),
            onItemClick = { appointment ->
                // Phase 3b — Medical Records navigation stub
                sharedViewModel.selectPatientBooking(appointment)
                findNavController().navigate(
                    R.id.action_global_to_patient_appointment_detail
                )
            }
        )

        b.rvConsultations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = consultationAdapter
            isNestedScrollingEnabled = true
        }

        b.swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
    }

    private fun setupObservers() {
        networkViewModel.myAppointments.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val allBookings = result.getOrNull() ?: emptyList()

                // Filter and Sort Logic
                val consultationHistory = allBookings
                    .filterIsInstance<DoctorAppointment>()
                    .filter {
                        it.status.lowercase() in listOf("completed", "cancelled", "no_show", "expired", "rejected")
                    }
                    .sortedByDescending { booking ->
                        try {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .parse(booking.appointmentDate)
                        } catch (e: Exception) {
                            null
                        }
                    }

                hasReceivedConsultationHistoryResult = true
                lastConsultationBaseList = consultationHistory
                applyHistoryStatusFilterToConsultations()
            } else {
                hasReceivedConsultationHistoryResult = true
                lastConsultationBaseList = emptyList()
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to load consultations",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding?.swipeRefreshLayout?.isRefreshing = false
            }
        }

        sharedViewModel.patientHistoryStatusFilter.observe(viewLifecycleOwner) {
            applyHistoryStatusFilterToConsultations()
        }
    }

    private fun applyHistoryStatusFilterToConsultations() {
        if (!hasReceivedConsultationHistoryResult) return
        val selected =
            sharedViewModel.patientHistoryStatusFilter.value?.lowercase(Locale.getDefault()) ?: "all"
        val filtered = if (selected == "all") {
            lastConsultationBaseList
        } else {
            lastConsultationBaseList.filter { it.status.equals(selected, ignoreCase = true) }
        }
        updateUI(filtered)
    }

    private fun updateUI(consultations: List<DoctorAppointment>) {
        val b = binding ?: return
        if (consultations.isEmpty()) {
            showEmptyState()
        } else {
            b.rvConsultations.visibility = View.VISIBLE
            b.layoutEmptyState.visibility = View.GONE
            consultationAdapter?.updateList(consultations)
        }
    }

    private fun showEmptyState() {
        val b = binding ?: return
        b.rvConsultations.visibility = View.GONE
        b.layoutEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}