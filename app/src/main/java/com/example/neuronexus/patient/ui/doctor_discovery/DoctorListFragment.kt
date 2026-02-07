package com.example.neuronexus.patient.ui.doctor_discovery

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentDoctorListBinding
import com.example.neuronexus.models.Doctor
import com.example.neuronexus.patient.adapters.DoctorDiscoveryAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class DoctorListFragment : Fragment() {

    private var _binding: FragmentDoctorListBinding? = null
    private val binding get() = _binding!!

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var doctorAdapter: DoctorDiscoveryAdapter
    private var allDoctors: List<Doctor> = emptyList() // Original list to filter from

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        networkViewModel.fetchAllDoctors()
    }

    private fun setupRecyclerView() {
        // Initialize Adapter with TWO lambdas (Card Click & More Click)
        doctorAdapter = DoctorDiscoveryAdapter(
            doctors = emptyList(),
            onDoctorClick = { selectedDoctor ->
                sharedViewModel.selectDoctor(selectedDoctor)

                // Navigate to Details Fragment
                findNavController().navigate(R.id.action_list_to_details)
            },
            onMoreClick = { doctor, view ->
                // More/3-Dots Click -> Show Popup Menu
                Toast.makeText(requireContext(), "Options for ${doctor.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDoctors.adapter = doctorAdapter
    }

    private fun setupListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            requireActivity().finish() // Close the DoctorDiscoveryActivity
        }

        // Search Bar Listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDoctors(s.toString(), binding.chipGroupFilters.checkedChipId)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter Chip Listeners
        binding.chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
            filterDoctors(binding.etSearch.text.toString(), checkedId)
        }
    }

    private fun setupObservers() {
        // 1. Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 2. Doctor List Data
        networkViewModel.doctorsList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { doctors ->
                allDoctors = doctors // Save original list
                filterDoctors(binding.etSearch.text.toString(), binding.chipGroupFilters.checkedChipId) // Apply current filters
            }
            result.onFailure { error ->
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load doctors")
            }
        }
    }

    // Logic to Filter & Sort List locally
    private fun filterDoctors(query: String, checkedChipId: Int) {
        var filtered = allDoctors

        if (query.isNotEmpty()) {
            val rawQuery = query.lowercase(Locale.ROOT)

            val cleanQuery = rawQuery.replace("dr.", "").replace("dr ", "").trim()

            filtered = filtered.filter { doc ->
                val name = doc.name.lowercase(Locale.ROOT)
                val spec = doc.specialization.lowercase(Locale.ROOT)

                name.contains(cleanQuery) || spec.contains(rawQuery)
            }
        }

        // 2. Apply Sorting/Filtering based on Chips
        // Note: Make sure IDs match your fragment_doctor_list.xml chip IDs
        when (checkedChipId) {
            binding.chipTopRated.id -> {
                filtered = filtered.sortedByDescending { it.rating }
            }
            binding.chipPriceLow.id -> {
                // Parse fee string safely (remove non-digits)
                filtered = filtered.sortedBy { it.consultationFee.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
            }
            binding.chipPriceHigh.id -> {
                filtered = filtered.sortedByDescending { it.consultationFee.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 }
            }
            // Add 'Nearby' logic later
        }

        doctorAdapter.updateList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}