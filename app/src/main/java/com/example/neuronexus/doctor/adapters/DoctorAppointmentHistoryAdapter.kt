package com.example.neuronexus.doctor.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemDoctorHistoryAppointmentBinding
import com.example.neuronexus.models.DoctorAppointmentHistoryItem

class DoctorAppointmentHistoryAdapter(
    private val list: List<DoctorAppointmentHistoryItem>,
    private val onItemClick: (DoctorAppointmentHistoryItem) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDoctorHistoryAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoctorHistoryAppointmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.tvPatientName.text = item.patientName
        holder.binding.tvPurpose.text = item.purpose
        holder.binding.tvDate.text = item.date
        holder.binding.tvTime.text = item.time
        holder.binding.chipStatus.text = item.status

        if (item.status == "Cancelled") {

            holder.binding.chipStatus.setTextColor(Color.parseColor("#C62828"))
            holder.binding.chipStatus.background.setTint(Color.parseColor("#FFEBEE"))

            holder.binding.btnViewDetails.isEnabled = false
            holder.binding.btnViewDetails.setTextColor(Color.parseColor("#BDBDBD"))
            holder.binding.btnViewDetails.setOnClickListener(null)
        } else {
            holder.binding.chipStatus.setTextColor(Color.parseColor("#2E7D32"))
            holder.binding.chipStatus.background.setTint(Color.parseColor("#E8F5E9"))

            holder.binding.btnViewDetails.isEnabled = true
            holder.binding.btnViewDetails.setTextColor(holder.itemView.context.getColor(R.color.primary_blue))
            holder.binding.btnViewDetails.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = list.size
}

