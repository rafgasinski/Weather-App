package com.example.weatherapp.model.repositories

import com.example.weatherapp.model.api.OpenWeatherApi
import com.example.weatherapp.model.response.city.CityResponse
import com.example.weatherapp.model.response.onecall.OneCallResponse
import com.example.weatherapp.utils.Constants
import retrofit2.Response

val oneCallApiService = OpenWeatherApi()

class ApiRepository {

    suspend fun getOneCall(lat: Double, lon: Double, exclude: String = Constants.EXCLUDE_MINUTELY_ALERTS) : Response<OneCallResponse> {
        return oneCallApiService.getOneCallForecast(lat, lon, exclude)
    }

    suspend fun getCityData(name: String) : Response<CityResponse> {
        return oneCallApiService.getCityData(name)
    }

}