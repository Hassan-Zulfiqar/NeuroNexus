package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemHistoryLabBinding
import com.example.neuronexus.patient.models.LabTestBooking
import java.util.Locale

class PatientHistoryLabAdapter(
    private var bookings: List<LabTestBooking>,
    private val onItemClick: (LabTestBooking) -> Unit
) : RecyclerView.Adapter<PatientHistoryLabAdapter.LabViewHolder>() {

    class LabViewHolder(val binding: ItemHistoryLabBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabViewHolder {
        val binding = ItemHistoryLabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabViewHolder, position: Int) {
        val booking = bookings[position]

        // Text Bindings
        holder.binding.tvTestName.text = booking.testName.ifBlank { "Lab Test" }
        holder.binding.tvLabName.text = booking.labName.ifBlank { "Unknown Lab" }
        holder.binding.tvDate.text = booking.testDate

//        // Image Binding (Option A with Circle Crop and Safe Blank Check)
//        Glide.with(holder.itemView.context)
//            .load(booking.labImageUrl.takeIf { it.isNotBlank() })
//            .placeholder(R.drawable.research).centerCrop()
//            .error(R.drawable.research)
//            .into(holder.binding.imgLabIcon)

        // Status Chip Formatting
        val statusLower = booking.status.lowercase(Locale.getDefault())
        when (statusLower) {
            "completed" -> {
                holder.binding.chipStatus.text = "Completed"
                holder.binding.chipStatus.setTextColor(Color.parseColor("#2E7D32"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#E8F5E9"))
            }
            "cancelled" -> {
                holder.binding.chipStatus.text = "Cancelled"
                holder.binding.chipStatus.setTextColor(Color.parseColor("#C62828"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#FFEBEE"))
            }
            "no_show" -> {
                holder.binding.chipStatus.text = "No Show"
                holder.binding.chipStatus.setTextColor(Color.parseColor("#E65100"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#FFF3E0"))
            }
            "rejected" -> {
                holder.binding.chipStatus.text = "Rejected"
                holder.binding.chipStatus.setTextColor(Color.parseColor("#C62828"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#FFEBEE"))
            }
            "expired" -> {
                holder.binding.chipStatus.text = "Expired"
                holder.binding.chipStatus.setTextColor(Color.parseColor("#757575"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#F5F5F5"))
            }
            else -> {
                holder.binding.chipStatus.text = booking.status.replaceFirstChar { it.uppercase() }
                holder.binding.chipStatus.setTextColor(Color.parseColor("#E65100"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#FFF3E0"))
            }
        }

        // Action Button
        holder.binding.btnViewReport.setOnClickListener {
            onItemClick(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateList(newList: List<LabTestBooking>) {
        bookings = newList
        notifyDataSetChanged()
    }
}