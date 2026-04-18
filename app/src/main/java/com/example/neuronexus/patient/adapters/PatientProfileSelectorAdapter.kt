package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemPatientProfileSelectorBinding
import com.example.neuronexus.patient.models.PatientProfile

class PatientProfileSelectorAdapter(
    private var profiles: List<PatientProfile>,
    private val onProfileClick: (PatientProfile) -> Unit
) : RecyclerView.Adapter<PatientProfileSelectorAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(val binding: ItemPatientProfileSelectorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemPatientProfileSelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val profile = profiles[position]

        // Extract Initial Safely
        holder.binding.tvPatientInitial.text = profile.fullName
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "?"

        // Text Bindings
        holder.binding.tvPatientName.text = profile.fullName.ifBlank { "Unknown" }
        holder.binding.tvPatientRelation.text = profile.relation.ifBlank { "Patient" }

        // Action Click
        holder.itemView.setOnClickListener {
            onProfileClick(profile)
        }
    }

    override fun getItemCount(): Int = profiles.size

    fun updateList(newList: List<PatientProfile>) {
        profiles = newList
        notifyDataSetChanged()
    }
}