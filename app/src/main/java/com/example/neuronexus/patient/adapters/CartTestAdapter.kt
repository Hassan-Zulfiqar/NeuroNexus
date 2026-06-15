package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemCartTestBinding
import com.example.neuronexus.patient.models.SelectedTest

class CartTestAdapter(
    private var tests: MutableList<SelectedTest>,
    private val onRemoveClick: (SelectedTest) -> Unit
) : RecyclerView.Adapter<CartTestAdapter.CartTestViewHolder>() {

    inner class CartTestViewHolder(
        val binding: ItemCartTestBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartTestViewHolder {
        val binding = ItemCartTestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartTestViewHolder(binding)
    }

    override fun getItemCount(): Int = tests.size

    override fun onBindViewHolder(
        holder: CartTestViewHolder,
        position: Int
    ) {
        val test = tests[position]
        val b = holder.binding

        b.tvCartTestName.text = test.testName.ifBlank { "Unknown Test" }
        b.tvCartTestType.text = test.testType.ifBlank { "" }

        b.tvCartSampleType.text = if (test.sampleType.isNotBlank()) {
            "Sample: ${test.sampleType}"
        } else {
            ""
        }

        b.tvCartTestPrice.text = if (test.price > 0) {
            "Rs. ${String.format("%,.0f", test.price)}"
        } else {
            "Price: TBD"
        }

        b.btnRemoveTest.setOnClickListener {
            onRemoveClick(test)
        }
    }

    fun updateList(newTests: MutableList<SelectedTest>) {
        tests = newTests
        notifyDataSetChanged()
    }
}
