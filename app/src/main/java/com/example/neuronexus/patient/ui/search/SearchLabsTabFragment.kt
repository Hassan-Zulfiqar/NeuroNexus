package com.example.neuronexus.patient.ui.search

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
import com.example.neuronexus.databinding.FragmentSearchLabsTabBinding
import com.example.neuronexus.patient.adapters.LabListAdapter
import com.example.neuronexus.patient.models.Lab
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class SearchLabsTabFragment : Fragment() {

    private var _binding: FragmentSearchLabsTabBinding? = null
    private val binding get() = _binding!!

    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var adapter: LabListAdapter
    private var allLabs: List<Lab> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchLabsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()

        // Fetch labs if not already loaded
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

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterAndSortLabs() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.chipGroupFilters.setOnCheckedChangeListener { _, _ ->
            filterAndSortLabs()
            applyChipStyles()
        }
    }

    private fun setupObservers() {
        networkViewModel.labsList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { labs ->
                allLabs = labs
                filterAndSortLabs()
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

    private fun filterAndSortLabs() {
        val query = binding.etSearch.text.toString().trim().lowercase(Locale.ROOT)

        var filteredLabs = allLabs.filter { lab ->
            val matchesSearch = lab.name.lowercase().contains(query) ||
                    lab.city.lowercase().contains(query) ||
                    lab.address.lowercase().contains(query)

            matchesSearch
        }

        // Apply sorting based on selected chip
        filteredLabs = when (binding.chipGroupFilters.checkedChipId) {
            binding.chipRatingHigh.id -> {
                filteredLabs.sortedByDescending { it.rating }
            }
            binding.chipRatingLow.id -> {
                filteredLabs.sortedBy { it.rating }
            }
            else -> filteredLabs
        }

        // Show empty state if no results
        if (filteredLabs.isEmpty() && query.isNotEmpty()) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.rvLabs.visibility = View.GONE
        } else {
            binding.tvNoResults.visibility = View.GONE
            binding.rvLabs.visibility = View.VISIBLE
        }

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
        sharedViewModel.selectLab(lab)
        try {
            findNavController().navigate(R.id.action_search_to_lab_details)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
