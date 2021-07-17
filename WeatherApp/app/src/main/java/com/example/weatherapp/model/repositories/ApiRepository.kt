package com.example.weatherapp.model.repositories

import com.example.weatherapp.model.api.OpenWeatherApi
import com.example.weatherapp.model.response.city.CityResponse
import com.example.weatherapp.model.response.onecall.OneCallResponse
import retrofit2.Response

val oneCallApiService = OpenWeatherApi()

class ApiRepository {
    suspend fun getOneCall(lat: String, lon: String) : Response<OneCallResponse> {
        return oneCallApiService.getOneCallForecast(lat, lon)
    }

    suspend fun getCityData(name: String) : Response<CityResponse> {
        return oneCallApiService.getCityData(name)
    }
}