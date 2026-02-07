package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemLabBinding
import com.example.neuronexus.patient.models.Lab

class LabListAdapter(
    private var labs: List<Lab>,
    private val onLabSelected: (Lab) -> Unit
) : RecyclerView.Adapter<LabListAdapter.LabViewHolder>() {

    inner class LabViewHolder(val binding: ItemLabBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lab: Lab) {
            binding.tvName.text = lab.name
            binding.tvLabTiming.text = if (lab.labTiming.isNotEmpty()) lab.labTiming else "Timing not available"
            binding.tvLocation.text = lab.city.ifEmpty { lab.address }

            // Rating Logic
            binding.tvRating.text = String.format("%.1f", lab.rating)
            binding.tvReviews.text = "(${lab.reviewCount})"

            // Image Loading
            Glide.with(binding.root.context)
                .load(lab.profilePicUrl.ifEmpty { lab.logo }) // Fallback to logo if profile pic empty
                .placeholder(R.drawable.doctor) // Use generic placeholder for now
                .error(R.drawable.doctor)
                .into(binding.imageLab)

            // Click Listener
            binding.root.setOnClickListener {
                onLabSelected(lab)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabViewHolder {
        val binding = ItemLabBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabViewHolder, position: Int) {
        holder.bind(labs[position])
    }

    override fun getItemCount(): Int = labs.size

    fun updateList(newLabs: List<Lab>) {
        labs = newLabs
        notifyDataSetChanged()
    }
}