package com.example.weatherapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.response.OneCallResponse
import kotlinx.coroutines.launch

class ForecastViewModel(application: Application) : AndroidViewModel(application) {
    val responseBody: MutableLiveData<OneCallResponse> = MutableLiveData()
    private val forecastRepository = ApiRepository()

    /**
     * API call with lat and lon as coordinates
     * */
    fun getOneCallForecast(lat: String, lon: String) {
        viewModelScope.launch {
            val response = forecastRepository.getOneCall(lat, lon)

            if(response.isSuccessful){
                responseBody.value = response.body()!!
                Log.d("Response", response.body()!!.current.weather[0].description)
            }
            else{
                Log.d("Response", response.errorBody().toString())
                Log.d("ErrorCode:", response.code().toString())
            }
        }
    }

    companion object{
        var defaultLat = 50.2584
        var defaultLon = 19.0275
        var userCity = ""
    }
}