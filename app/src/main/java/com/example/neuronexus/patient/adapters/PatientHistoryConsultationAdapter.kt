package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemHistoryConsultationBinding
import com.example.neuronexus.patient.models.HistoryConsultationItem
import com.example.neuronexus.R

class PatientHistoryConsultationAdapter(
    private val consultationList: List<HistoryConsultationItem>,
    private val onPrescriptionClick: (HistoryConsultationItem) -> Unit
) : RecyclerView.Adapter<PatientHistoryConsultationAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemHistoryConsultationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryConsultationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = consultationList[position]

        holder.binding.tvDoctorName.text = item.doctorName
        holder.binding.tvSpecialty.text = item.specialty
        holder.binding.tvDate.text = item.date
        holder.binding.tvTime.text = item.time
        holder.binding.imgDoctor.setImageResource(item.imageResId)

        holder.binding.chipStatus.text = item.status

        holder.binding.btnViewPrescription.visibility = android.view.View.VISIBLE
        holder.binding.divider.visibility = android.view.View.VISIBLE

        if (item.status == "Cancelled") {
            holder.binding.chipStatus.setTextColor(Color.parseColor("#C62828"))
            holder.binding.chipStatus.background.setTint(Color.parseColor("#FFEBEE"))

            holder.binding.btnViewPrescription.isEnabled = false
            holder.binding.btnViewPrescription.setTextColor(Color.parseColor("#BDBDBD"))
            holder.binding.btnViewPrescription.setOnClickListener(null)

        } else {
            holder.binding.chipStatus.setTextColor(Color.parseColor("#2E7D32"))
            holder.binding.chipStatus.background.setTint(Color.parseColor("#E8F5E9"))

            holder.binding.btnViewPrescription.isEnabled = true
            holder.binding.btnViewPrescription.setTextColor(holder.itemView.context.getColor(R.color.primary_blue))

            holder.binding.btnViewPrescription.setOnClickListener {
                onPrescriptionClick(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return consultationList.size
    }
}

