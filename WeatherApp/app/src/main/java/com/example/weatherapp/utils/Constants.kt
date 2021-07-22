package com.example.weatherapp.utils

class Constants {
    companion object{
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        const val API_KEY = ""
        const val UNITS = "metric"
        const val EXCLUDE_MINUTELY_ALERTS = "alerts, minutely"
        const val EXCLUDE_ALERTS_HOURLY_MINUTELY = "alerts, hourly, minutely"
        const val LOCATION_SETTINGS_REQUEST = 500
    }
}