package com.example.weatherapp.model.response.city

import com.google.gson.annotations.SerializedName


data class Weather(
    @SerializedName("icon")
    val icon: String
)