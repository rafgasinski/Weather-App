package com.example.weatherapp.model.data

data class WeatherData (
    val temp: String,
    val description: String,
    val sunrise: String,
    val sunset: String,
    val feelsLike: String,
    val clouds: String,
    val windSpeed: String,
    val humidity: String,
    val pressure: String,
    val uvIndex: String,
    val sunProgress: Int
)