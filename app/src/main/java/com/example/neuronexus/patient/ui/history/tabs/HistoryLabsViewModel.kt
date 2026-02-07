package com.example.neuronexus.patient.ui.history.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.models.HistoryLabItem

class HistoryLabsViewModel : ViewModel() {

    private val _labs = MutableLiveData<List<HistoryLabItem>>()
    val labs: LiveData<List<HistoryLabItem>> = _labs

    init {
        loadData()
    }

    private fun loadData() {
        val list = listOf(
            HistoryLabItem("MRI Brain Scan", "City Diagnostic Lab", "Nov 05, 2024", "Completed"),
            HistoryLabItem("Blood Test (CBC)", "City Diagnostic Lab", "Oct 20, 2024", "Cancelled"),
            HistoryLabItem("CT Scan Head", "Central Hospital Lab", "Sep 15, 2024", "Completed"),
            HistoryLabItem("X-Ray Skull", "City Diagnostic Lab", "Aug 10, 2024", "Completed")
        )
        _labs.value = list
    }
}