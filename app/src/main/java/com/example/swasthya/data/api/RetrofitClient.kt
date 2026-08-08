package com.example.swasthya.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // For local emulator, use 10.0.2.2. For physical device on same wifi, use computer's IP
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val instance: BloodApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BloodApiService::class.java)
    }
}
