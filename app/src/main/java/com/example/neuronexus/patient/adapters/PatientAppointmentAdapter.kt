package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemPatientScheduleAppointmentBinding
import com.example.neuronexus.patient.models.DoctorAppointment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PatientAppointmentAdapter(
    private var appointmentList: List<DoctorAppointment>,
    private val onActionClick: (DoctorAppointment, String) -> Unit // actionType: "CANCEL" or "RESCHEDULE"
) : RecyclerView.Adapter<PatientAppointmentAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(val binding: ItemPatientScheduleAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemPatientScheduleAppointmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val appointment = appointmentList[position]

        holder.binding.tvName.text = appointment.doctorName
        // If we had specialty in DoctorAppointment, we'd use it. For now, hardcoded or use existing field if added.
        holder.binding.tvSpec.text = if (appointment.doctorSpecialization.isNotEmpty()) appointment.doctorSpecialization else "Specialist"

        // Time Range Calculation
        val timeRange = calculateTimeRange(appointment.appointmentTime)
        holder.binding.tvScheduleDate.text = "$timeRange ${appointment.appointmentDate}"

        // Image Loading with Glide
        if (appointment.doctorImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(appointment.doctorImageUrl)
                .placeholder(R.drawable.doctor)
                .error(R.drawable.doctor)
                .into(holder.binding.imgDoc)
        } else {
            // Fallback for existing appointments or missing URLs
            holder.binding.imgDoc.setImageResource(R.drawable.doctor)
        }

        // Status Logic for Button
        // "created" or "confirmed" -> Upcoming -> CANCEL
        // "cancelled", "completed" -> Past -> RESCHEDULE
        val isUpcoming = appointment.status == "created" || appointment.status == "confirmed"

        if (isUpcoming) {
            holder.binding.btnCancel.text = "CANCEL"
            holder.binding.btnCancel.setTextColor(Color.parseColor("#FF4848")) // Red
            holder.binding.btnCancel.setOnClickListener {
                onActionClick(appointment, "CANCEL")
            }
        } else {
            holder.binding.btnCancel.text = "RESCHEDULE"
            holder.binding.btnCancel.setTextColor(Color.parseColor("#407BFF")) // Blue
            holder.binding.btnCancel.setOnClickListener {
                onActionClick(appointment, "RESCHEDULE")
            }
        }
    }

    override fun getItemCount() = appointmentList.size

    fun updateList(newList: List<DoctorAppointment>) {
        appointmentList = newList
        notifyDataSetChanged()
    }

    private fun calculateTimeRange(startTime: String): String {
        return try {
            // Input format: "08:00 PM"
            val parser = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = parser.parse(startTime) ?: return startTime

            val calendar = Calendar.getInstance()
            calendar.time = date

            // Start Time String
            val startString = parser.format(calendar.time)

            // Add 30 Minutes
            calendar.add(Calendar.MINUTE, 30)
            val endString = parser.format(calendar.time)

            "$startString - $endString"
        } catch (e: Exception) {
            startTime // Fallback
        }
    }
}