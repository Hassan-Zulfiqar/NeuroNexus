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
import com.example.neuronexus.databinding.FragmentHistoryLabsBinding
import com.example.neuronexus.patient.adapters.PatientHistoryLabAdapter
import com.example.neuronexus.patient.models.LabTestBooking
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryLabsFragment : Fragment() {

    private var _binding: FragmentHistoryLabsBinding? = null
    private val binding get() = _binding

    // Koin ViewModel Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var labAdapter: PatientHistoryLabAdapter? = null

    private var lastLabBaseList: List<LabTestBooking> = emptyList()
    private var hasReceivedLabHistoryResult = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryLabsBinding.inflate(inflater, container, false)
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

        labAdapter = PatientHistoryLabAdapter(
            bookings = emptyList(),

            onItemClick = { booking ->
                sharedViewModel.selectPatientBooking(booking)
                findNavController().navigate(
                    R.id.action_global_to_patient_appointment_detail
                )
            }
        )

        b.rvLabs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = labAdapter
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
                val labHistory = allBookings
                    .filterIsInstance<LabTestBooking>()
                    .filter {
                        it.status.lowercase() in listOf("completed", "cancelled", "no_show", "expired", "rejected")
                    }
                    .sortedByDescending { booking ->
                        try {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .parse(booking.testDate) // Use testDate for Labs
                        } catch (e: Exception) {
                            null
                        }
                    }

                hasReceivedLabHistoryResult = true
                lastLabBaseList = labHistory
                applyHistoryStatusFilterToLabs()
            } else {
                hasReceivedLabHistoryResult = true
                lastLabBaseList = emptyList()
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    result.exceptionOrNull()?.message ?: "Failed to load lab history",
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
            applyHistoryStatusFilterToLabs()
        }
    }

    private fun applyHistoryStatusFilterToLabs() {
        if (!hasReceivedLabHistoryResult) return
        val selected =
            sharedViewModel.patientHistoryStatusFilter.value?.lowercase(Locale.getDefault()) ?: "all"
        val filtered = if (selected == "all") {
            lastLabBaseList
        } else {
            lastLabBaseList.filter { it.status.equals(selected, ignoreCase = true) }
        }
        updateUI(filtered)
    }

    private fun updateUI(labs: List<LabTestBooking>) {
        val b = binding ?: return
        if (labs.isEmpty()) {
            showEmptyState()
        } else {
            b.rvLabs.visibility = View.VISIBLE
            b.layoutEmptyState.visibility = View.GONE
            labAdapter?.updateList(labs)
        }
    }

    private fun showEmptyState() {
        val b = binding ?: return
        b.rvLabs.visibility = View.GONE
        b.layoutEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}