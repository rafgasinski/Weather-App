package com.example.weatherapp.model.response.onecall


import com.google.gson.annotations.SerializedName

data class Current(
    @SerializedName("clouds")
        val clouds: Int,
    @SerializedName("dt")
        val dt: Int,
    @SerializedName("feels_like")
        val feelsLike: Double,
    @SerializedName("humidity")
        val humidity: Int,
    @SerializedName("pressure")
        val pressure: Int,
    @SerializedName("sunrise")
        val sunrise: Int,
    @SerializedName("sunset")
        val sunset: Int,
    @SerializedName("temp")
        val temp: Double,
    @SerializedName("uvi")
        val uvi: Double,
    @SerializedName("weather")
        val weather: List<Weather>,
    @SerializedName("wind_deg")
        val windDeg: Double,
    @SerializedName("wind_speed")
        val windSpeed: Double
)