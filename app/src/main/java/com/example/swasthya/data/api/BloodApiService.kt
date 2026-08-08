package com.example.swasthya.data.api

import com.example.swasthya.data.model.BloodStockResponse
import retrofit2.http.GET

interface BloodApiService {
    @GET("api/blood-stock")
    suspend fun getBloodStock(): BloodStockResponse
}
