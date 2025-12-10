package com.example.neuronexus.patient.ui.home

import androidx.lifecycle.ViewModel
import com.example.neuronexus.patient.models.PatientDashboardService
import com.example.neuronexus.R

class PatientHomeViewModel : ViewModel() {

    fun getServiceList(): List<PatientDashboardService> {
        return listOf(
            PatientDashboardService("Heart Surgeon", R.drawable.heart),
            PatientDashboardService("Dentistry", R.drawable.tooth),
            PatientDashboardService("Neurology", R.drawable.brain),
            PatientDashboardService("Orthopedic", R.drawable.orthopedics)
        )
    }
}

