package com.example.neuronexus.patient.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.databinding.ItemPatientDashboardServiceBinding
import com.example.neuronexus.models.PatientDashboardService

class PatientServiceAdapter(private val serviceList: List<PatientDashboardService>) :
    RecyclerView.Adapter<PatientServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(val binding: ItemPatientDashboardServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemPatientDashboardServiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = serviceList[position]
        holder.binding.tvServiceName.text = service.title
        holder.binding.imgServiceIcon.setImageResource(service.iconResId)
    }

    override fun getItemCount(): Int {
        return serviceList.size
    }
}

