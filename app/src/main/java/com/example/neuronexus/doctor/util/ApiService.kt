package com.example.neuronexus.doctor.util

import com.example.neuronexus.doctor.models.AnalyzeResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("analyze")
    suspend fun analyzeImage(
        @Part file: MultipartBody.Part
    ): AnalyzeResponse

}