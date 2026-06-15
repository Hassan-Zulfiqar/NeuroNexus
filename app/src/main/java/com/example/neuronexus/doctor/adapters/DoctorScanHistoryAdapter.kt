package com.example.neuronexus.doctor.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemDoctorHistoryScanBinding
import com.example.neuronexus.doctor.models.TumorReport

class DoctorScanHistoryAdapter(
    private var list: List<TumorReport>,
    private val onItemClick: (TumorReport) -> Unit
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
        val b = holder.binding

        b.tvPrediction.text = item.prediction.ifBlank {
            item.location.ifBlank { "Unknown" }
        }
        b.tvPatientName.text = "Patient: ${item.patientName.ifBlank { "Not specified" }}"
        b.tvDate.text = item.date.ifBlank { "Unknown date" }

        val confidencePct = (item.confidence * 100).toInt()
        b.chipConfidence.text = "$confidencePct%"

        if (confidencePct > 90) {
            b.chipConfidence.setTextColor(Color.parseColor("#2E7D32"))
            b.chipConfidence.background.setTint(Color.parseColor("#E8F5E9"))
        } else {
            b.chipConfidence.setTextColor(Color.parseColor("#F57C00"))
            b.chipConfidence.background.setTint(Color.parseColor("#FFF3E0"))
        }

        val thumbnailUrl = item.overlayImageUrl.ifBlank { item.originalImageUrl }
        if (thumbnailUrl.isNotBlank()) {
            Glide.with(b.imgScanThumbnail.context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_mri)
                .error(R.drawable.ic_mri)
                .into(b.imgScanThumbnail)
        } else {
            b.imgScanThumbnail.setImageResource(R.drawable.ic_mri)
        }

        b.btnViewScan.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<TumorReport>) {
        list = newList
        notifyDataSetChanged()
    }
}
