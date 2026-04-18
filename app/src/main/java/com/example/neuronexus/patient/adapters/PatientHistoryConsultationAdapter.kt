package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemHistoryConsultationBinding
import com.example.neuronexus.patient.models.DoctorAppointment
import java.util.Locale

class PatientHistoryConsultationAdapter(
    private var appointments: List<DoctorAppointment>,
    private val onItemClick: (DoctorAppointment) -> Unit
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
        val appointment = appointments[position]

        // Text Bindings
        holder.binding.tvDoctorName.text = "Dr. ${appointment.doctorName}".trim()
        holder.binding.tvSpecialty.text = appointment.doctorSpecialization.ifBlank { "General Physician" }
        holder.binding.tvDate.text = appointment.appointmentDate
        holder.binding.tvTime.text = appointment.appointmentTime

        // Image Binding (Option A)
        Glide.with(holder.itemView.context)
            .load(appointment.doctorImageUrl)
            .placeholder(R.drawable.doctor)
            .error(R.drawable.doctor)
            .into(holder.binding.imgDoctor)

        // Status Chip Formatting
        val statusLower = appointment.status.lowercase(Locale.getDefault())
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
                holder.binding.chipStatus.text = appointment.status.replaceFirstChar { it.uppercase() }
                holder.binding.chipStatus.setTextColor(Color.parseColor("#E65100"))
                holder.binding.chipStatus.background.setTint(Color.parseColor("#FFF3E0"))
            }
        }

        // Action Button
        holder.binding.btnViewPrescription.setOnClickListener {
            onItemClick(appointment)
        }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateList(newList: List<DoctorAppointment>) {
        appointments = newList
        notifyDataSetChanged()
    }
}