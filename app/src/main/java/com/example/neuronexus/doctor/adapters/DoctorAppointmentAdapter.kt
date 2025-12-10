package com.example.neuronexus.doctor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemDoctorAppointmentCardBinding
import com.example.neuronexus.doctor.models.DoctorAppointmentItem

class DoctorAppointmentAdapter(private val appointmentList: List<DoctorAppointmentItem>) :
    RecyclerView.Adapter<DoctorAppointmentAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(val binding: ItemDoctorAppointmentCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemDoctorAppointmentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointmentList[position]

        holder.binding.tvPatientName.text = appointment.patientName
        holder.binding.tvTime.text = appointment.time

        holder.binding.tvDayOfWeek.text = appointment.dayOfWeek
        holder.binding.tvDayOfMonth.text = appointment.dayOfMonth

        holder.itemView.setOnClickListener {
        }
    }

    override fun getItemCount(): Int {
        return appointmentList.size
    }
}

