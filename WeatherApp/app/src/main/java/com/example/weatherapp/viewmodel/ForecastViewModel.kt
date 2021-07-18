package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.weatherapp.R
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.response.onecall.OneCallResponse
import com.example.weatherapp.utils.*
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class ForecastViewModel(application: Application): AndroidViewModel(application) {
    private val forecastRepository = ApiRepository()

    val responseData: MutableLiveData<Resource<OneCallResponse>> = MutableLiveData()

    fun getOneCallForecast(lat: String, lon: String) {
        viewModelScope.launch {
            safeApiCall(lat, lon)
        }
    }

    private suspend fun safeApiCall(lat: String, lon: String) {
        responseData.postValue(Resource.Loading())
        try {
            if(isOnline(getApplication<Application>())) {
                val response = forecastRepository.getOneCall(lat, lon)
                responseData.postValue(handleResponse(response))
            } else {
                responseData.postValue(Resource.Error(Event(getApplication<Application>().resources.getString(R.string.no_network_connection))))
            }
        } catch(t: Throwable) {
            when(t) {
                is IOException -> responseData.postValue(Resource.Error(Event(getApplication<Application>().resources.getString(R.string.network_failure))))
                else -> responseData.postValue(Resource.Error(Event(getApplication<Application>().resources.getString(R.string.unknown_error))))
            }

        }
    }

    private fun handleResponse(response: Response<OneCallResponse>) : Resource<OneCallResponse> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(Event(response.message()))
    }

}