package com.example.neuronexus.patient.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuronexus.R
import com.example.neuronexus.databinding.ItemPatientScheduleAppointmentBinding
import com.example.neuronexus.patient.models.Booking
import com.example.neuronexus.patient.models.DoctorAppointment
import com.example.neuronexus.patient.models.LabTestBooking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PatientAppointmentAdapter(
    private var appointmentList: List<Booking>, // 1. Now accepts the polymorphic parent interface/class
    private val onActionClick: (Booking, String) -> Unit // actionType: "CANCEL" or "RESCHEDULE"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // 2. Changed to generic generic ViewHolder

    // View Type Constants
    companion object {
        private const val VIEW_TYPE_DOCTOR = 1
        private const val VIEW_TYPE_LAB = 2
    }

    // 3. Separate ViewHolders (Currently sharing the exact same XML binding, but logically isolated)
    class DoctorViewHolder(val binding: ItemPatientScheduleAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    class LabViewHolder(val binding: ItemPatientScheduleAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    // 4. Safely determine which type of booking we are looking at
    override fun getItemViewType(position: Int): Int {
        return when (appointmentList[position]) {
            is DoctorAppointment -> VIEW_TYPE_DOCTOR
            is LabTestBooking -> VIEW_TYPE_LAB
            else -> throw IllegalArgumentException("Unknown booking type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // We use the same layout for now. If you make a Lab-specific XML later,
        // you would inflate it under the VIEW_TYPE_LAB branch.
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_DOCTOR -> {
                val binding = ItemPatientScheduleAppointmentBinding.inflate(inflater, parent, false)
                DoctorViewHolder(binding)
            }
            VIEW_TYPE_LAB -> {
                val binding = ItemPatientScheduleAppointmentBinding.inflate(inflater, parent, false)
                LabViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = appointmentList[position]

        holder.itemView.setOnClickListener {
            onActionClick(item, "VIEW_DETAIL")
        }

        // Route the data to the perfectly isolated binding logic
        when (holder) {
            is DoctorViewHolder -> bindDoctor(holder, item as DoctorAppointment)
            is LabViewHolder -> bindLab(holder, item as LabTestBooking)
        }
    }

    // =========================================================================
    // DOCTOR BINDING LOGIC (100% UNTOUCHED & SAFE)
    // =========================================================================
    private fun bindDoctor(holder: DoctorViewHolder, appointment: DoctorAppointment) {
        holder.binding.tvName.text = appointment.doctorName
        holder.binding.tvSpec.text = if (appointment.doctorSpecialization.isNotEmpty()) appointment.doctorSpecialization else "Specialist"

        val timeRange = calculateTimeRange(appointment.appointmentTime)
        holder.binding.tvScheduleDate.text = "$timeRange ${appointment.appointmentDate}"

        if (appointment.doctorImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(appointment.doctorImageUrl)
                .placeholder(R.drawable.doctor)
                .error(R.drawable.doctor)
                .into(holder.binding.imgDoc)
        } else {
            holder.binding.imgDoc.setImageResource(R.drawable.doctor)
        }

        setupActionButton(holder.binding, appointment, appointment.status)
    }

    // =========================================================================
    // LAB BINDING LOGIC (NEWLY ADDED)
    // =========================================================================
    private fun bindLab(holder: LabViewHolder, booking: LabTestBooking) {
        // Map Lab data to the existing UI fields
        holder.binding.tvName.text = booking.labName
        holder.binding.tvSpec.text = booking.testName

        val timeRange = calculateTimeRange(booking.testTime)
        holder.binding.tvScheduleDate.text = "$timeRange ${booking.testDate}"

        // Image Loading with Glide (using doctor drawable as fallback since it's the same UI for now)
        if (booking.labImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(booking.labImageUrl)
                .placeholder(R.drawable.doctor) // Change to R.drawable.lab if you have one
                .error(R.drawable.doctor)
                .into(holder.binding.imgDoc)
        } else {
            holder.binding.imgDoc.setImageResource(R.drawable.doctor)
        }

        setupActionButton(holder.binding, booking, booking.status)
    }

    // =========================================================================
    // SHARED UTILITIES
    // =========================================================================

    // Abstracted button logic so we don't repeat the exact same color/click code twice
    private fun setupActionButton(binding: ItemPatientScheduleAppointmentBinding, item: Booking, status: String) {
        val isUpcoming = status == "pending" || status == "confirmed"

        if (isUpcoming) {
            binding.btnCancel.text = "CANCEL"
            binding.btnCancel.setTextColor(Color.parseColor("#FF4848")) // Red
            binding.btnCancel.setOnClickListener {
                onActionClick(item, "CANCEL")
            }
        } else {
            binding.btnCancel.text = "RESCHEDULE"
            binding.btnCancel.setTextColor(Color.parseColor("#407BFF")) // Blue
            binding.btnCancel.setOnClickListener {
                onActionClick(item, "RESCHEDULE")
            }
        }
    }

    override fun getItemCount() = appointmentList.size

    fun updateList(newList: List<Booking>) {
        appointmentList = newList
        notifyDataSetChanged()
    }

    private fun calculateTimeRange(startTime: String): String {
        return try {
            val parser = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = parser.parse(startTime) ?: return startTime

            val calendar = Calendar.getInstance()
            calendar.time = date

            val startString = parser.format(calendar.time)
            calendar.add(Calendar.MINUTE, 30)
            val endString = parser.format(calendar.time)

            "$startString - $endString"
        } catch (e: Exception) {
            startTime
        }
    }
}