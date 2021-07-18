package com.example.weatherapp.model.response.onecall


import com.google.gson.annotations.SerializedName

data class Daily(
    @SerializedName("dt")
        val dt: Int,
    @SerializedName("temp")
        val temp: Temp,
    @SerializedName("weather")
        val weather: List<Weather>
)