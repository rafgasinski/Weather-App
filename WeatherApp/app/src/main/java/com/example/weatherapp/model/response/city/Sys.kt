package com.example.weatherapp.model.response.city

import com.google.gson.annotations.SerializedName

data class Sys (
    @SerializedName("country")
    val countryCode: String
)