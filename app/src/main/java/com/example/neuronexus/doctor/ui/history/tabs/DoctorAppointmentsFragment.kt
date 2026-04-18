package com.example.neuronexus.doctor.ui.history.tabs

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
import com.example.neuronexus.databinding.FragmentDoctorAppointmentsBinding
import com.example.neuronexus.doctor.adapters.DoctorAppointmentHistoryAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class DoctorAppointmentsFragment : Fragment() {

    private var _binding: FragmentDoctorAppointmentsBinding? = null
    private val binding get() = _binding!!

    // Koin ViewModel Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var historyAdapter: DoctorAppointmentHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
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
        networkViewModel.fetchDoctorAppointments()
    }

    private fun setupUI() {
        // Initialize Adapter with Empty List and Click Listener
        historyAdapter = DoctorAppointmentHistoryAdapter(
            appointments = emptyList(),
            onViewDetailsClick = { appointment ->
                sharedViewModel.selectDoctorAppointment(appointment)
                findNavController().navigate(R.id.action_history_to_detail)
            }
        )

        // Setup RecyclerView
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
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

                    // Filter and Sort Data
                    val historyList = allAppointments
                        .filter { appointment ->
                            appointment.status.lowercase() in listOf(
                                "completed", "cancelled", "no_show", "expired"
                            )
                        }
                        .sortedByDescending { appointment ->
                            try {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .parse(appointment.appointmentDate)
                            } catch (e: Exception) {
                                null
                            }
                        }

                    // Toggle Visibility
                    if (historyList.isEmpty()) {
                        binding.rvAppointments.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.rvAppointments.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        historyAdapter.updateList(historyList)
                    }
                } else {
                    // Handle Failure
                    binding.rvAppointments.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    Toast.makeText(
                        requireContext(),
                        it.exceptionOrNull()?.message ?: "Failed to load history",
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
