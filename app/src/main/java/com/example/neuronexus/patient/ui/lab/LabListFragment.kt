package com.example.neuronexus.patient.ui.lab

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentLabListBinding
import com.example.neuronexus.patient.adapters.LabListAdapter
import com.example.neuronexus.patient.models.Lab
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LabListFragment : Fragment() {

    private var _binding: FragmentLabListBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()

    // FIX: Use sharedViewModel() to share data with Details Fragment
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var adapter: LabListAdapter
    private var allLabs: List<Lab> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        sharedViewModel.clearBookingState()

        // Fetch only if empty to avoid reloading on back press
        if (networkViewModel.labsList.value == null) {
            networkViewModel.fetchAllLabs()
        }
    }

    private fun setupUI() {
        adapter = LabListAdapter(emptyList()) { selectedLab ->
            onLabSelected(selectedLab)
        }

        binding.rvLabs.layoutManager = LinearLayoutManager(context)
        binding.rvLabs.adapter = adapter

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterList() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chipGroupFilters.setOnCheckedChangeListener { _, _ ->
            filterList()
            applyChipStyles()
        }
    }

    private fun setupObservers() {
        networkViewModel.labsList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { labs ->
                allLabs = labs
                filterList()
                binding.cardProgress.isVisible = false
            }.onFailure { error ->
                binding.cardProgress.isVisible = false
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load labs")
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Only show loader if list is empty
            if (allLabs.isEmpty()) {
                binding.cardProgress.isVisible = isLoading
            }
        }
    }

    private fun filterList() {
        val query = binding.etSearch.text.toString().trim().lowercase()
        val isTopRatedOnly = binding.chipTopRated.isChecked

        val filteredLabs = allLabs.filter { lab ->
            val matchesSearch = lab.name.lowercase().contains(query) ||
                    lab.city.lowercase().contains(query) ||
                    lab.address.lowercase().contains(query)

            val matchesRating = if (isTopRatedOnly) lab.rating >= 4.5 else true

            matchesSearch && matchesRating
        }.sortedByDescending { if (isTopRatedOnly) it.rating else 0.0 }

        adapter.updateList(filteredLabs)
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

    private fun onLabSelected(lab: Lab) {
        // 1. Pass data to SharedViewModel (Scoped to Activity)
        sharedViewModel.selectLab(lab)

        // 2. Navigate to Details
        try {
            findNavController().navigate(R.id.action_labList_to_labDetails)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}