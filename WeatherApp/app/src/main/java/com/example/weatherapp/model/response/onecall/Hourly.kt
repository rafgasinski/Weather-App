package com.example.weatherapp.model.response.onecall

import com.google.gson.annotations.SerializedName

data class Hourly (
    @SerializedName("dt")
        val dt: Int,
    @SerializedName("temp")
        var temp: Double,
    @SerializedName("feels_like")
        val feelsLike: Double,
    @SerializedName("pressure")
        val pressure: Int,
    @SerializedName("humidity")
        val humidity: Int,
    @SerializedName("dew_point")
        val dewPoint: Double,
    @SerializedName("uvi")
        val uvi: Double,
    @SerializedName("clouds")
        val clouds: Double,
    @SerializedName("visibility")
        val visibility: Int,
    @SerializedName("wind_deg")
        val windDeg: Double,
    @SerializedName("wind_speed")
        var windSpeed: Double,
    @SerializedName("weather")
        val weather: List<Weather>,
    @SerializedName("pop")
        val pop: Double
)