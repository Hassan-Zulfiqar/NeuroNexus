package com.example.neuronexus.patient.ui.search

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentSearchDoctorsTabBinding
import com.example.neuronexus.doctor.models.Doctor
import com.example.neuronexus.patient.adapters.DoctorDiscoveryAdapter
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class SearchDoctorsTabFragment : Fragment() {

    private var _binding: FragmentSearchDoctorsTabBinding? = null
    private val binding get() = _binding!!

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var doctorAdapter: DoctorDiscoveryAdapter
    private var allDoctors: List<Doctor> = emptyList()
    private var currentSortOrder: String = "rating_desc" // Default sort order

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchDoctorsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupObservers()
        
        // Fetch doctors if not already loaded
        if (networkViewModel.doctorsList.value == null) {
            networkViewModel.fetchAllDoctors()
        }
    }

    private fun setupRecyclerView() {
        doctorAdapter = DoctorDiscoveryAdapter(
            doctors = emptyList(),
            onDoctorClick = { selectedDoctor ->
                onDoctorSelected(selectedDoctor)
            },
            onMoreClick = { doctor, _ ->
                Toast.makeText(requireContext(), "Options for ${doctor.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDoctors.adapter = doctorAdapter
    }

    private fun setupListeners() {
        // Search Bar Listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndSortDoctors()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter Chip Listeners
        binding.chipGroupFilters.setOnCheckedChangeListener { _, _ ->
            filterAndSortDoctors()
            applyChipStyles()
        }
    }

    private fun setupObservers() {
        // Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.cardProgress.visibility = if (isLoading && allDoctors.isEmpty()) View.VISIBLE else View.GONE
        }

        // Doctor List Data
        networkViewModel.doctorsList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { doctors ->
                allDoctors = doctors
                filterAndSortDoctors()
            }
            result.onFailure { error ->
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load doctors")
            }
        }
    }

    private fun filterAndSortDoctors() {
        val query = binding.etSearch.text.toString().lowercase(Locale.ROOT)
        val cleanQuery = query.replace(Regex("\\bdr\\.?\\s?"), "").trim()

        // Filter by search query
        var filtered = allDoctors.filter { doctor ->
            val name = doctor.name.lowercase(Locale.ROOT)
            val spec = doctor.specialization.lowercase(Locale.ROOT)
            val location = doctor.clinicAddress.lowercase(Locale.ROOT)

            name.contains(cleanQuery) || 
            spec.contains(query) || 
            location.contains(cleanQuery)
        }

        // Apply sorting based on selected chip
        filtered = when (binding.chipGroupFilters.checkedChipId) {
            binding.chipRatingHigh.id -> {
                currentSortOrder = "rating_desc"
                filtered.sortedByDescending { it.rating }
            }
            binding.chipRatingLow.id -> {
                currentSortOrder = "rating_asc"
                filtered.sortedBy { it.rating }
            }
            binding.chipFeeHigh.id -> {
                currentSortOrder = "fee_desc"
                filtered.sortedByDescending { 
                    it.consultationFee.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 
                }
            }
            binding.chipFeeLow.id -> {
                currentSortOrder = "fee_asc"
                filtered.sortedBy { 
                    it.consultationFee.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0 
                }
            }
            else -> filtered
        }

        // Show empty state if no results
        if (filtered.isEmpty() && query.isNotEmpty()) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.rvDoctors.visibility = View.GONE
        } else {
            binding.tvNoResults.visibility = View.GONE
            binding.rvDoctors.visibility = View.VISIBLE
        }

        doctorAdapter.updateList(filtered)
    }

    private fun applyChipStyles() {
        for (i in 0 until binding.chipGroupFilters.childCount) {
            val chip = binding.chipGroupFilters.getChildAt(i) as Chip
            if (chip.isChecked) {
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setTextColor(Color.BLACK)
            }
        }
    }

    private fun onDoctorSelected(doctor: Doctor) {
        sharedViewModel.selectDoctor(doctor)
        
        // Start DoctorDiscoveryActivity which has the complete booking flow
        val intent = Intent(requireContext(), Class.forName(
            "com.example.neuronexus.patient.activities.DoctorDiscoveryActivity"
        )).apply {
            putExtra("doctorId", doctor.uid)
            putExtra("fromSearch", true)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
