package com.example.neuronexus.doctor.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemDoctorAppointmentCardBinding
import com.example.neuronexus.patient.models.DoctorAppointment
import java.text.SimpleDateFormat
import java.util.Locale

class DoctorAppointmentAdapter(
    private val appointmentList: List<DoctorAppointment>,
    private val onAppointmentClick: (DoctorAppointment) -> Unit
) : RecyclerView.Adapter<DoctorAppointmentAdapter.AppointmentViewHolder>() {

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

        // 1. Bind direct fields mapped from the real model
        holder.binding.tvPatientName.text = appointment.patientNameSnapshot
        holder.binding.tvTime.text = appointment.appointmentTime

        // 2. Safely parse the appointmentDate string to extract dayOfWeek and dayOfMonth
        try {
            val inputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(appointment.appointmentDate)

            if (date != null) {
                // "EEE" gives short day (e.g., Tue). Uppercase it to match fake UI design (TUE).
                val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val dayOfMonthFormat = SimpleDateFormat("dd", Locale.getDefault())

                holder.binding.tvDayOfWeek.text = dayOfWeekFormat.format(date).uppercase(Locale.getDefault())
                holder.binding.tvDayOfMonth.text = dayOfMonthFormat.format(date)
            } else {
                holder.binding.tvDayOfWeek.text = ""
                holder.binding.tvDayOfMonth.text = ""
            }
        } catch (e: Exception) {
            // Graceful fallback if the database contains a malformed date string
            holder.binding.tvDayOfWeek.text = ""
            holder.binding.tvDayOfMonth.text = ""
        }

        // 3. Wire up the item click listener using the lambda callback
        holder.itemView.setOnClickListener {
            onAppointmentClick(appointment)
        }
    }

    override fun getItemCount(): Int {
        return appointmentList.size
    }
}