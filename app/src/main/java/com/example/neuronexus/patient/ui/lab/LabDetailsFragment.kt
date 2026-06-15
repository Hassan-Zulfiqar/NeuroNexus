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
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.common.utils.AlertUtils
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentLabDetailsBinding
import com.example.neuronexus.patient.adapters.LabTestAdapter
import com.example.neuronexus.patient.models.Lab
import com.example.neuronexus.patient.models.LabTest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LabDetailsFragment : Fragment() {

    private var _binding: FragmentLabDetailsBinding? = null
    private val binding get() = _binding!!

    // Koin Injection
    private val networkViewModel: NetworkViewModel by viewModel()

    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private lateinit var adapter: LabTestAdapter
    private var allTests: List<LabTest> = emptyList()
    private var currentLab: Lab? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        // 1. Observe Shared Data (Triggered immediately if data exists)
        sharedViewModel.selectedLab.observe(viewLifecycleOwner) { lab ->
            if (lab != null) {
                currentLab = lab
                setupHeader(lab)
                // Fetch tests for this lab
                networkViewModel.fetchTestsForLab(lab.uid)
            } else {
                AlertUtils.showError(requireContext(), "Lab information missing. Please try again.")
                findNavController().popBackStack()
            }
        }

        setupObservers()
    }

    private fun setupHeader(lab: Lab) {
        binding.tvLabName.text = lab.name
        binding.tvLabAddress.text = lab.address
        binding.tvLabRating.text = String.format("%.1f (%d Reviews)", lab.rating, lab.reviewCount)
        binding.tvLabTiming.text = if (lab.labTiming.isNotEmpty()) lab.labTiming else "Timing not available"

        Glide.with(this)
            .load(lab.profilePicUrl.ifEmpty { lab.logo })
            .placeholder(R.drawable.doctor)
            .error(R.drawable.doctor)
            .into(binding.ivLabProfile)
    }

    private fun setupUI() {
        adapter = LabTestAdapter(emptyList(), { selectedTest ->
            onTestSelected(selectedTest)
        }, { test ->
            onAddToCartClick(test)
        })

        binding.rvLabTests.layoutManager = LinearLayoutManager(context)
        binding.rvLabTests.adapter = adapter

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnViewCart?.setOnClickListener {
            findNavController().navigate(R.id.action_lab_details_to_cart)
        }

        binding.etSearchTests.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterTests(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupObservers() {
        // Observe Tests from Network
        networkViewModel.labTests.observe(viewLifecycleOwner) { result ->
            result.onSuccess { tests ->
                allTests = tests
                filterTests("") // Show all initially
                binding.progressBar.isVisible = false
                binding.cardProgress.isVisible = false
            }.onFailure { error ->
                binding.progressBar.isVisible = false
                binding.cardProgress.isVisible = false
            }
        }

        networkViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (allTests.isEmpty()) {
                binding.cardProgress.isVisible = isLoading
            }
        }

        // Observe cart changes to update adapter state and badge
        sharedViewModel.cartTests.observe(viewLifecycleOwner) { cartTests ->
            val cartIds = cartTests.map { it.testId }.toSet()
            adapter.updateCartState(cartIds)

            // Show/hide cart badge and View Cart button
            val count = cartTests.size
            if (count > 0) {
                binding.tvCartBadge?.text = count.toString()
                binding.tvCartBadge?.visibility = android.view.View.VISIBLE
                binding.btnViewCart?.visibility = android.view.View.VISIBLE
                binding.btnViewCart?.text = "View Cart ($count)"
            } else {
                binding.tvCartBadge?.visibility = android.view.View.GONE
                binding.btnViewCart?.visibility = android.view.View.GONE
            }
        }
    }

    private fun filterTests(query: String) {
        val filtered = if (query.isEmpty()) {
            allTests
        } else {
            allTests.filter {
                it.testName.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            }
        }
        adapter.updateList(filtered)
    }

    private fun onTestSelected(test: LabTest) {
        if (currentLab == null) {
            AlertUtils.showError(requireContext(), "Lab details missing. Please try again.")
            return
        }

        sharedViewModel.selectLabTest(test)

        try {
            findNavController().navigate(R.id.action_labDetails_to_testDetails)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onAddToCartClick(test: LabTest) {
        val isInCart = sharedViewModel.cartTests.value
            ?.any { it.testId == test.id } == true

        if (isInCart) {
            sharedViewModel.removeTestFromCart(test.id)
        } else {
            // Convert LabTest to SelectedTest for cart
            val selectedTest = com.example.neuronexus.patient.models.SelectedTest(
                testId = test.id,
                testName = test.testName,
                testType = test.category,
                sampleType = test.sampleType,
                price = test.price.toDoubleOrNull() ?: 0.0,
                duration = test.duration
            )
            sharedViewModel.addTestToCart(selectedTest)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}