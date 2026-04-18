package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemPrescriptionCardBinding
import com.example.neuronexus.patient.models.Prescription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrescriptionAdapter(
    private var prescriptions: List<Prescription>,
    private val onPrescriptionClick: (Prescription) -> Unit
) : RecyclerView.Adapter<PrescriptionAdapter.PrescriptionViewHolder>() {

    class PrescriptionViewHolder(val binding: ItemPrescriptionCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
        val binding = ItemPrescriptionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrescriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
        val prescription = prescriptions[position]

        // Text Bindings
        holder.binding.tvDoctorName.text = "Dr. ${prescription.doctorName.ifBlank { "Unknown Doctor" }}"
        holder.binding.tvDiagnosis.text = "Diagnosis: ${prescription.diagnosis.ifBlank { "Not specified" }}"
        holder.binding.tvIssuedDate.text = "Issued: ${formatTimestamp(prescription.issuedDate)}"

        // Medication Count Logic
        val count = prescription.medications.filter { it.isNotBlank() }.size
        holder.binding.tvMedicationCount.text = when (count) {
            0 -> "No medications listed"
            1 -> "1 medication prescribed"
            else -> "$count medications prescribed"
        }

        // Action Click
        holder.itemView.setOnClickListener {
            onPrescriptionClick(prescription)
        }
    }

    override fun getItemCount(): Int = prescriptions.size

    fun updateList(newList: List<Prescription>) {
        prescriptions = newList
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        return if (timestamp == 0L) {
            "Date not available"
        } else {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}