package com.example.weatherapp.model.response


import com.google.gson.annotations.SerializedName

data class Daily(
        @SerializedName("clouds")
        val clouds: Double,
        @SerializedName("dew_point")
        val dewPoint: Double,
        @SerializedName("dt")
        val dt: Int,
        @SerializedName("feels_like")
        val feelsLike: FeelsLike,
        @SerializedName("humidity")
        val humidity: Int,
        @SerializedName("pop")
        val pop: Double,
        @SerializedName("pressure")
        val pressure: Double,
        @SerializedName("rain")
        val rain: Double,
        @SerializedName("sunrise")
        val sunrise: Int,
        @SerializedName("sunset")
        val sunset: Int,
        @SerializedName("temp")
        val temp: Temp,
        @SerializedName("uvi")
        val uvi: Double,
        @SerializedName("weather")
        val weather: List<WeatherX>,
        @SerializedName("wind_deg")
        val windDeg: Double,
        @SerializedName("wind_speed")
        val windSpeed: Double
)