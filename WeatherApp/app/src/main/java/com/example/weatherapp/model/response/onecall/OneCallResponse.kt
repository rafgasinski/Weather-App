package com.example.weatherapp.model.response.onecall


import com.google.gson.annotations.SerializedName

data class OneCallResponse(
    @SerializedName("current")
        val current: Current,
    @SerializedName("hourly")
        val hourly: List<Hourly>,
    @SerializedName("daily")
        val daily: List<Daily>,
    @SerializedName("lat")
        val lat: Double,
    @SerializedName("lon")
        val lon: Double,
    @SerializedName("timezone")
        val timezone: String,
    @SerializedName("timezone_offset")
        val timezoneOffset: Int
)