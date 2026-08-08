package com.example.swasthya.data.model

import com.google.gson.annotations.SerializedName

data class BloodStockResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<BloodStockItem>?,
    @SerializedName("message") val message: String?
)

data class BloodStockItem(
    @SerializedName("bank_name") val bankName: String,
    @SerializedName("availability") val availability: String,
    @SerializedName("last_updated") val lastUpdated: String,
    @SerializedName("category") val category: String
)
