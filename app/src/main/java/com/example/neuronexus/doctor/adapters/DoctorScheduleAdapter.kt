package com.example.neuronexus.doctor.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemDoctorScheduleAppointmentBinding
import com.example.neuronexus.patient.models.DoctorAppointment

class DoctorScheduleAdapter(
    private var appointments: List<DoctorAppointment>,
    private val onActionLeftClick: (DoctorAppointment) -> Unit,
    private val onActionRightClick: (DoctorAppointment) -> Unit,
    private val onCardClick: (DoctorAppointment) -> Unit
) : RecyclerView.Adapter<DoctorScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(val binding: ItemDoctorScheduleAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemDoctorScheduleAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val appointment = appointments[position]
        val context = holder.itemView.context

        // 1. Bind exact fields with strict null-safe fallbacks
        holder.binding.tvPatientName.text = appointment.patientNameSnapshot.ifEmpty { "Unknown Patient" }
        holder.binding.tvPurpose.text = appointment.reasonForVisit.ifEmpty { "General Consultation" }
        holder.binding.tvDate.text = appointment.appointmentDate.ifEmpty { "" }
        holder.binding.tvTime.text = appointment.appointmentTime.ifEmpty { "" }

        // 2. Initials Avatar (Replacing Glide)
        val patientName = appointment.patientNameSnapshot.trim()
        val initial = if (patientName.isNotEmpty()) patientName.substring(0, 1).uppercase() else "?"
        holder.binding.tvPatientInitial.text = initial

        // 3. Dynamic Status Configuration using exact Color Resources
        when (appointment.status.lowercase()) {
            "pending" -> {
                // Left Action (Reject)
                holder.binding.btnActionLeft.text = "Reject"
                holder.binding.btnActionLeft.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.red))

                // Right Action (Accept)
                holder.binding.btnActionRight.text = "Accept"
                holder.binding.btnActionRight.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.success))

                // Status Chip (Pending)
                holder.binding.chipStatus.text = "Pending"
                holder.binding.chipStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_white))
                holder.binding.chipStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.yellow))
            }
            "confirmed" -> {
                // Left Action (No-Show)
                holder.binding.btnActionLeft.text = "No-Show"
                holder.binding.btnActionLeft.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.warning))

                // Right Action (Complete)
                holder.binding.btnActionRight.text = "Complete"
                holder.binding.btnActionRight.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.primary_blue))

                // Status Chip (Confirmed)
                holder.binding.chipStatus.text = "Confirmed"
                holder.binding.chipStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_white))
                holder.binding.chipStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.success))
            }
        }

        // 4. Click Listeners
        holder.itemView.setOnClickListener { onCardClick(appointment) }
        holder.binding.btnActionLeft.setOnClickListener { onActionLeftClick(appointment) }
        holder.binding.btnActionRight.setOnClickListener { onActionRightClick(appointment) }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateList(newAppointments: List<DoctorAppointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }
}