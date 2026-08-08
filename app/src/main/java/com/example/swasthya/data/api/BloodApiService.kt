package com.example.swasthya.data.api

import com.example.swasthya.data.model.BloodStockResponse
import com.example.swasthya.data.model.StatesResponse
import com.example.swasthya.data.model.DistrictsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BloodApiService {
    @GET("api/blood-stock")
    suspend fun getBloodStock(
        @Query("state_code") stateCode: String,
        @Query("district_code") districtCode: String,
        @Query("blood_group") bloodGroup: String,
        @Query("blood_component") bloodComponent: String
    ): BloodStockResponse

    @GET("api/states")
    suspend fun getStates(): StatesResponse

    @GET("api/districts")
    suspend fun getDistricts(
        @Query("state_code") stateCode: String
    ): DistrictsResponse
}
