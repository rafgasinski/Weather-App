package com.example.weatherapp.model.repositories

import android.util.Log
import com.example.weatherapp.model.api.OneCallApi
import com.example.weatherapp.model.response.OneCallResponse
import retrofit2.Response

const val DEFAULT_PARAMS = "minutely"

val oneCallApiService = OneCallApi()

class ApiRepository {
    suspend fun getOneCall(lat: String, lon: String, exclude: String = DEFAULT_PARAMS) : Response<OneCallResponse> {
        Log.d("oneCallApi", "oneCallApiResponse")
        return oneCallApiService.getOneCallForecast(lat, lon, exclude)
    }
}