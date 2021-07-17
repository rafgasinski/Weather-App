package com.example.weatherapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.model.data.WeatherData
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.response.onecall.OneCallResponse
import com.example.weatherapp.utils.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ForecastViewModel : ViewModel() {
    private val forecastRepository = ApiRepository()

    val responseData: MutableLiveData<OneCallResponse?> = MutableLiveData()

    private val mWeatherData = MutableLiveData<WeatherData>()
    val weatherData: LiveData<WeatherData> get() = mWeatherData

    private val preferencesManager = PreferencesManager.getInstance()

    fun getOneCallForecast(lat: String, lon: String) {
        viewModelScope.launch {
            val response = forecastRepository.getOneCall(lat, lon)

            if(response.isSuccessful){
                responseData.value = response.body()!!

                val responseBody = response.body()!!
                val currentData = responseBody.current

                preferencesManager.timeZone = responseBody.timezone

                val currentTime = currentData.dt
                val sunrise = currentData.sunrise
                val sunset = currentData.sunset

                val sunriseNowDiff = ((currentTime - sunrise) / 60).toDouble()
                val sunsetSunriseDiff = ((sunset - sunrise) / 60).toDouble()

                val sunProgress = if(currentTime in (sunrise + 1) until sunset) {
                        ((sunriseNowDiff / sunsetSunriseDiff) * 100).toInt()
                } else {
                    0
                }

                mWeatherData.postValue(WeatherData(
                    currentData.temp.roundToInt().toString(),
                    currentData.weather[0].description.capitalizeFirst,
                    timeFormat(currentData.sunrise, responseBody.timezone),
                    timeFormat(currentData.sunset, responseBody.timezone),
                    round(currentData.feelsLike),
                    currentData.clouds.roundToInt().toString(),
                    convertWindUnit(currentData.windSpeed),
                    currentData.humidity.toString(),
                    currentData.pressure.toString(),
                    currentData.uvi.roundToInt().toString(),
                    sunProgress
                ))

            }
        }
    }
}