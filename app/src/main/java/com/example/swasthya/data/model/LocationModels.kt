package com.example.swasthya.data.model

import com.google.gson.annotations.SerializedName

data class StatesResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<LocationItem>?,
    @SerializedName("message") val message: String?
)

data class DistrictsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<LocationItem>?,
    @SerializedName("message") val message: String?
)

data class LocationItem(
    @SerializedName("value") val value: String,
    @SerializedName("label") val label: String
)
