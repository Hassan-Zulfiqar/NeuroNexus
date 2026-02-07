package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemTimeSlotBinding
import com.example.neuronexus.patient.model.TimeSlot

class TimeSlotAdapter(
    private var timeSlots: List<TimeSlot>,
    private val onTimeSelected: (TimeSlot) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    inner class TimeSlotViewHolder(val binding: ItemTimeSlotBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val slot = timeSlots[position]

        with(holder.binding.chipTimeSlot) {
            text = slot.timeLabel

            // 1. Availability: Disable if booked
            isEnabled = slot.isAvailable
            alpha = if (slot.isAvailable) 1.0f else 0.5f // Visual cue for disabled slots

            // 2. Selection: Check if this slot is selected
            isChecked = slot.isSelected

            // 3. Click Listener
            setOnClickListener {
                if (slot.isAvailable) {
                    // Reset all other selections
                    timeSlots.forEach { it.isSelected = false }
                    // Select current
                    slot.isSelected = true

                    // Refresh UI to show change
                    notifyDataSetChanged()

                    // Callback to Fragment
                    onTimeSelected(slot)
                }
            }
        }
    }

    override fun getItemCount() = timeSlots.size

    // Helper to update list from Fragment
    fun updateList(newSlots: List<TimeSlot>) {
        timeSlots = newSlots
        notifyDataSetChanged()
    }
}