package com.example.weatherapp.model.response.onecall

import com.google.gson.annotations.SerializedName

data class Hourly (
    @SerializedName("dt")
        val dt: Int,
    @SerializedName("temp")
        var temp: Double,
    @SerializedName("wind_speed")
        var windSpeed: Double,
    @SerializedName("weather")
        val weather: List<Weather>
)