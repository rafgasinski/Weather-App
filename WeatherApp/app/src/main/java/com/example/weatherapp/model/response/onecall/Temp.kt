package com.example.weatherapp.model.response.onecall


import com.google.gson.annotations.SerializedName

data class Temp(
        @SerializedName("max")
        val max: Double,
        @SerializedName("min")
        val min: Double
)