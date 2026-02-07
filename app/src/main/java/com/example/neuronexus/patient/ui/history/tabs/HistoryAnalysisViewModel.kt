package com.example.neuronexus.patient.ui.history.tabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.neuronexus.models.HistoryAnalysisItem

class HistoryAnalysisViewModel : ViewModel() {

    private val _analysisList = MutableLiveData<List<HistoryAnalysisItem>>()
    val analysisList: LiveData<List<HistoryAnalysisItem>> = _analysisList

    init {
        loadData()
    }

    private fun loadData() {
        val list = listOf(
            HistoryAnalysisItem("Tumor Risk Analysis", "Based on 5 symptoms", "Yesterday, 10:30 PM", "High Risk"),
            HistoryAnalysisItem("Tumor Risk Analysis", "Based on 3 symptoms", "Nov 12, 2024", "Moderate Risk"),
            HistoryAnalysisItem("Tumor Risk Analysis", "Based on 2 symptoms", "Oct 05, 2024", "Low Risk"),
            HistoryAnalysisItem("Tumor Risk Analysis", "Based on 4 symptoms", "Sep 20, 2024", "High Risk")
        )
        _analysisList.value = list
    }
}