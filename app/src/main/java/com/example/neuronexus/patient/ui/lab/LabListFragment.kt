package com.example.neuronexus.patient.ui.lab

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import org.koin.androidx.viewmodel.ext.android.viewModel

class LabListFragment : Fragment() {

    private var _binding: FragmentLabListBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()
    private val sharedViewModel: SharedViewModel by viewModel()

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
        networkViewModel.fetchAllLabs()
    }

    private fun setupUI() {
        // Initialize Adapter
        adapter = LabListAdapter(emptyList()) { selectedLab ->
            onLabSelected(selectedLab)
        }

        binding.rvLabs.layoutManager = LinearLayoutManager(context)
        binding.rvLabs.adapter = adapter

        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Search Listener
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterList()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Filter Chip Listener
        binding.chipGroupFilters.setOnCheckedChangeListener { _, _ ->
            filterList()
        }
    }

    private fun setupObservers() {
        // Observe Lab List
        networkViewModel.labsList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { labs ->
                allLabs = labs
                filterList() // Initial filter (shows all)
                binding.progressBar.isVisible = false
            }.onFailure { error ->
                binding.progressBar.isVisible = false
                AlertUtils.showError(requireContext(), error.message ?: "Failed to load labs")
            }
        }

        // Loading State
        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    private fun filterList() {
        val query = binding.etSearch.text.toString().trim().lowercase()
        val isTopRatedOnly = binding.chipTopRated.isChecked
        // 'Nearby' logic would require Location permission & GeoFire, skipping for now (showing all if checked or not)

        val filteredLabs = allLabs.filter { lab ->
            val matchesSearch = lab.name.lowercase().contains(query) ||
                    lab.city.lowercase().contains(query) ||
                    lab.address.lowercase().contains(query)

            val matchesRating = if (isTopRatedOnly) lab.rating >= 4.5 else true

            matchesSearch && matchesRating
        }.sortedByDescending { if (isTopRatedOnly) it.rating else 0.0 } // Sort by rating if filter active

        adapter.updateList(filteredLabs)

        // Optional: Show "No Results" view if list is empty
        // binding.tvNoResults.isVisible = filteredLabs.isEmpty()
    }

    private fun onLabSelected(lab: Lab) {
        // 1. Pass data to SharedViewModel
        sharedViewModel.selectLab(lab)
        Toast.makeText(context, "Navigating to ${lab.name}", Toast.LENGTH_SHORT).show()
        // 2. Navigate to Details
        // Note: Ensure this ID exists in your mobile_navigation.xml
//        try {
//            findNavController().navigate(R.id.action_labList_to_labDetails)
//        } catch (e: Exception) {
//            // Fallback for development if nav graph isn't updated yet
//            //Toast.makeText(context, "Navigating to ${lab.name}", Toast.LENGTH_SHORT).show()
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}