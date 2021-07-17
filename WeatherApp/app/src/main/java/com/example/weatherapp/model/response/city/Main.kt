package com.example.weatherapp.model.response.city

import com.google.gson.annotations.SerializedName

data class Main(
    @SerializedName("temp")
    val temp: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    @SerializedName("temp_min")
    val tempMin: Double
)