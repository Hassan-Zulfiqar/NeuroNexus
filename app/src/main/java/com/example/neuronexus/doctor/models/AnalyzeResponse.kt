package com.example.neuronexus.doctor.models

data class AnalyzeResponse(
    val status: String,
    val prediction: String,
    val confidence: Double,
    val has_tumor: Boolean,
    val size_metrics: SizeMetrics,
    val mask: String
)
