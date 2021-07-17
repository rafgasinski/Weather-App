package com.example.weatherapp.model.response.city

import com.google.gson.annotations.SerializedName


data class CityResponse(
    @SerializedName("coord")
    val coord: Coord,
    @SerializedName("main")
    val main: Main,
    @SerializedName("name")
    val name: String,
    @SerializedName("sys")
    val sys: Sys,
    @SerializedName("weather")
    val weather: List<Weather>
)