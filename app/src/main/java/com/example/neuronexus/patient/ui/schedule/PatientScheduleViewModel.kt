package com.example.neuronexus.patient.ui.schedule

import androidx.lifecycle.ViewModel
import com.example.neuronexus.R
import com.example.neuronexus.patient.models.PatientAppointmentItem

class PatientScheduleViewModel : ViewModel() {

    fun getUpcomingList(): List<PatientAppointmentItem> {
        return listOf(
            PatientAppointmentItem("Dr. Tamim Ikraim", "Dermatologist", "17 Oct, 2022", "8:00 - 8:30 am", R.drawable.doctor, "UPCOMING"),
            PatientAppointmentItem("Dr. Tasnim Mridha", "Cardiologist", "18 Oct, 2022", "9:00 - 9:30 am", R.drawable.doctor, "UPCOMING"),
            PatientAppointmentItem("Dr. Zubaeer Rahim", "Dentist", "22 Oct, 2022", "9:30 - 10:00 am", R.drawable.doctor, "UPCOMING")
        )
    }

    fun getPastList(): List<PatientAppointmentItem> {
        return listOf(
            PatientAppointmentItem("Dr. John Doe", "Dentist", "10 Sep, 2022", "10:00 - 10:30 am", R.drawable.doctor, "PAST"),
            PatientAppointmentItem("Dr. Emily Clark", "Neurologist", "05 Aug, 2022", "14:00 - 15:00 pm", R.drawable.doctor, "PAST")
        )
    }
}

