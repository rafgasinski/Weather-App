package com.example.weatherapp.model.response.onecall


import com.google.gson.annotations.SerializedName

data class Weather(
        @SerializedName("description")
        val description: String,
        @SerializedName("icon")
        var icon: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("main")
        val main: String
)