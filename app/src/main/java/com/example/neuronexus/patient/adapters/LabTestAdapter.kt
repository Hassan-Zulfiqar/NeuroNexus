package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemLabTestBinding
import com.example.neuronexus.patient.models.LabTest

class LabTestAdapter(
    private var tests: List<LabTest>,
    private val onTestSelected: (LabTest) -> Unit
) : RecyclerView.Adapter<LabTestAdapter.TestViewHolder>() {

    inner class TestViewHolder(val binding: ItemLabTestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(test: LabTest) {
            binding.tvTestName.text = test.testName
            binding.tvDescription.text = test.description
            binding.tvPrice.text = "RS ${test.price}"

            // Installment Logic
            if (test.installments.equals("yes", ignoreCase = true)) {
                binding.tvInstallmentBadge.visibility = View.VISIBLE
                binding.tvInstallmentBadge.text = "${test.noOfInstallments} Installments Available"
            } else {
                binding.tvInstallmentBadge.visibility = View.GONE
            }

            // Click Listener (Navigate to Test Details)
            binding.root.setOnClickListener {
                onTestSelected(test)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val binding = ItemLabTestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        holder.bind(tests[position])
    }

    override fun getItemCount(): Int = tests.size

    fun updateList(newTests: List<LabTest>) {
        tests = newTests
        notifyDataSetChanged()
    }
}