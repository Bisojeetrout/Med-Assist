package com.example.swasthya.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Pointing to production Render backend
    private const val BASE_URL = "https://swasthya-blood-api.onrender.com/"

    val instance: BloodApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BloodApiService::class.java)
    }
}
