package com.example.neuronexus.doctor.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemDoctorHistoryScanBinding
import com.example.neuronexus.doctor.models.DoctorScanHistoryItem

class DoctorScanHistoryAdapter(
    private val list: List<DoctorScanHistoryItem>,
    private val onItemClick: (DoctorScanHistoryItem) -> Unit
) : RecyclerView.Adapter<DoctorScanHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDoctorHistoryScanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoctorHistoryScanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.tvPrediction.text = item.prediction
        holder.binding.tvPatientName.text = "Patient: ${item.patientName}"
        holder.binding.tvDate.text = item.date
        holder.binding.chipConfidence.text = item.confidence

        holder.binding.imgScanThumbnail.setImageResource(item.imageResId)

        val confValue = item.confidence.replace("%", "").toIntOrNull() ?: 0
        if (confValue > 90) {
            holder.binding.chipConfidence.setTextColor(Color.parseColor("#2E7D32"))
            holder.binding.chipConfidence.background.setTint(Color.parseColor("#E8F5E9"))
        } else {
            holder.binding.chipConfidence.setTextColor(Color.parseColor("#F57C00"))
            holder.binding.chipConfidence.background.setTint(Color.parseColor("#FFF3E0"))
        }

        holder.binding.btnViewScan.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = list.size
}

