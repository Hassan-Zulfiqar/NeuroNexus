package com.example.neuronexus.patient.ui.lab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.viewmodel.SharedViewModel
import com.example.neuronexus.databinding.FragmentLabTestCartBinding
import com.example.neuronexus.patient.adapters.CartTestAdapter
import com.example.neuronexus.patient.models.SelectedTest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class LabTestCartFragment : Fragment() {

    private var _binding: FragmentLabTestCartBinding? = null
    private val binding get() = _binding

    private val sharedViewModel: SharedViewModel by sharedViewModel()

    private var cartAdapter: CartTestAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLabTestCartBinding
            .inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        val b = binding ?: return

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        cartAdapter = CartTestAdapter(
            tests = sharedViewModel.cartTests.value?.toMutableList() ?: mutableListOf(),
            onRemoveClick = { test ->
                sharedViewModel.removeTestFromCart(test.testId)
            }
        )

        b.rvCartTests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }

        // Lab info from SharedViewModel selected lab
        val selectedLab = sharedViewModel.selectedLab.value
        b.tvLabName.text = selectedLab?.name ?: "Selected Lab"
        b.tvLabAddress.text = selectedLab?.address?.ifBlank { "Address not available" }
            ?: "Address not available"

        b.btnProceed.setOnClickListener {
            val cartTests = sharedViewModel.cartTests.value
            if (cartTests.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please add at least one test to continue",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.action_cart_to_patient_selection
            )
        }
    }

    private fun setupObservers() {
        sharedViewModel.cartTests.observe(viewLifecycleOwner) { cartTests ->
            val b = binding ?: return@observe

            val count = cartTests.size
            b.tvCartCount.text = "$count ${if (count == 1) "test" else "tests"}"

            if (count == 0) {
                b.rvCartTests.visibility = View.GONE
                b.layoutEmptyCart.visibility = View.VISIBLE
                b.btnProceed.isEnabled = false
                b.tvTotalAmount.text = "Rs. 0"
            } else {
                b.rvCartTests.visibility = View.VISIBLE
                b.layoutEmptyCart.visibility = View.GONE
                b.btnProceed.isEnabled = true
                cartAdapter?.updateList(cartTests.toMutableList())

                val total = sharedViewModel.getCartTotal()
                b.tvTotalAmount.text = if (total > 0) {
                    "Rs. ${String.format("%,.0f", total)}"
                } else {
                    "Price will be confirmed at lab"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
