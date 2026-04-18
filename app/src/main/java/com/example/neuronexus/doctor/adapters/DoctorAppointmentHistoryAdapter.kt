package com.example.neuronexus.doctor.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemDoctorHistoryAppointmentBinding
import com.example.neuronexus.patient.models.DoctorAppointment

class DoctorAppointmentHistoryAdapter(
    private var appointments: List<DoctorAppointment>,
    private val onViewDetailsClick: (DoctorAppointment) -> Unit
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
        val appointment = appointments[position]
        val context = holder.itemView.context

        // Initials Avatar Setup
        holder.binding.tvPatientInitial.text = appointment.patientNameSnapshot
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "?"

        // Safe String Bindings with Fallbacks
        holder.binding.tvPatientName.text = appointment.patientNameSnapshot
            .ifEmpty { "Unknown Patient" }

        holder.binding.tvPurpose.text = appointment.reasonForVisit
            .ifEmpty { "General Consultation" }

        holder.binding.tvDate.text = appointment.appointmentDate
        holder.binding.tvTime.text = appointment.appointmentTime

        // Programmatic Status Chip Styling
        val (statusText, statusColor) = when (appointment.status.lowercase()) {
            "completed" -> Pair("Completed", R.color.success)
            "cancelled" -> Pair("Cancelled", R.color.error)
            "no_show"   -> Pair("No Show", R.color.warning)
            "expired"   -> Pair("Expired", R.color.text_hint)
            else        -> Pair(appointment.status, R.color.warning)
        }

        holder.binding.chipStatus.text = statusText
        holder.binding.chipStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, statusColor)
        )
        holder.binding.chipStatus.setTextColor(
            ContextCompat.getColor(context, android.R.color.white)
        )

        // View Details Button Configuration
        holder.binding.btnViewDetails.isEnabled = true
        holder.binding.btnViewDetails.setOnClickListener {
            onViewDetailsClick(appointment)
        }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateList(newAppointments: List<DoctorAppointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
}