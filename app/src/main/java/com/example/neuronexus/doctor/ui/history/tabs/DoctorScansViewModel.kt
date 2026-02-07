package com.example.neuronexus.doctor.ui.history.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.R
import com.example.neuronexus.models.DoctorScanHistoryItem

class DoctorScansViewModel : ViewModel() {

    private val _scans = MutableLiveData<List<DoctorScanHistoryItem>>()
    val scans: LiveData<List<DoctorScanHistoryItem>> = _scans

    init {
        loadData()
    }

    private fun loadData() {
        // We use 'ic_image' as a placeholder for the MRI scan
        val list = listOf(
            DoctorScanHistoryItem("Meningioma", "98%", "Eleanor Vance", "Dec 10, 10:45 AM", R.drawable.ic_mri),
            DoctorScanHistoryItem("No Tumor", "99%", "John Smith", "Dec 08, 09:30 AM", R.drawable.ic_mri),
            DoctorScanHistoryItem("Glioma", "85%", "Marcus Holloway", "Dec 05, 02:15 PM", R.drawable.ic_mri),
            DoctorScanHistoryItem("Pituitary", "92%", "Clara Oswald", "Nov 28, 11:00 AM", R.drawable.ic_mri)
        )
        _scans.value = list
    }
}