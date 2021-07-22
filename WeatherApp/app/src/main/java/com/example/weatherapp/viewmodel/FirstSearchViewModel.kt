package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.model.db.MainDatabase
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.repositories.LocationRepository
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.Event
import com.example.weatherapp.utils.isOnline
import com.example.weatherapp.utils.preferencesManager
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

class FirstSearchViewModel(application: Application): AndroidViewModel(application) {
    private val apiRepository = ApiRepository()
    private val locationRepository: LocationRepository

    val shouldNavigate = MutableLiveData(false)
    val toastMessage = MutableLiveData<Event<String>>()

    init {
        val locationDao = MainDatabase.getDatabase(
            application
        ).locationDao()

        locationRepository = LocationRepository(locationDao)
    }

    private fun addLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.add(location)
        }
    }

    fun getCityDataToAddByName(name: String) {
        viewModelScope.launch {
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = apiRepository.getCityData(name)
                    if(response.isSuccessful) {
                        val responseCityData = response.body()!!

                        val city = responseCityData.name
                        val countryCode = responseCityData.sys.countryCode
                        val lat = responseCityData.coord.lat
                        val lon = responseCityData.coord.lon
                        val isDay = responseCityData.weather[0].icon.last().toString() == "d"

                        val responseWeatherData = apiRepository.getOneCall(lat, lon, Constants.EXCLUDE_ALERTS_HOURLY_MINUTELY)

                        if(responseWeatherData.isSuccessful) {
                            val responseWeatherDataBody = responseWeatherData.body()!!

                            preferencesManager.city = city
                            preferencesManager.countryCode = countryCode
                            preferencesManager.lat = lat
                            preferencesManager.lon = lon
                            preferencesManager.useBackgroundDay = isDay
                            preferencesManager.timeZone = responseWeatherDataBody.timezone

                            addLocation(
                                Location(0, city, countryCode, lat, lon, responseWeatherDataBody.current.dt, responseWeatherDataBody.current.temp.roundToInt(),
                                    responseWeatherDataBody.daily[0].temp.max.roundToInt(), responseWeatherDataBody.daily[0].temp.min.roundToInt(), isDay)
                            )

                            shouldNavigate.postValue(true)
                        }
                    } else {
                        toastMessage.postValue(Event(getApplication<Application>().resources.getString(
                            R.string.unknown_location)))
                    }
                } else {
                    toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.no_network_connection)))
                }
            } catch(t: Throwable) {
                when(t) {
                    is IOException -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(
                        R.string.network_failure)))
                    else -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(
                        R.string.unknown_error)))
                }
            }
        }
    }

    fun getCityDataToAddByCoord(lat: Double, lon: Double, city: String, countryCode: String) {
        viewModelScope.launch {
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = apiRepository.getOneCall(lat, lon, Constants.EXCLUDE_ALERTS_HOURLY_MINUTELY)
                    if(response.isSuccessful) {

                        val responseCityData = response.body()!!
                        val currentData = responseCityData.current

                        val isDay = responseCityData.current.weather[0].icon.last().toString() == "d"

                        preferencesManager.city = city
                        preferencesManager.countryCode = countryCode
                        preferencesManager.lat = lat
                        preferencesManager.lon = lon
                        preferencesManager.useBackgroundDay = isDay
                        preferencesManager.timeZone = responseCityData.timezone

                        addLocation(
                            Location(0, city, countryCode, lat, lon, currentData.dt, currentData.temp.roundToInt(),
                                responseCityData.daily[0].temp.max.roundToInt(), responseCityData.daily[0].temp.min.roundToInt(), isDay)
                        )


                        shouldNavigate.postValue(true)
                    } else {
                        toastMessage.postValue(Event(response.message()))
                    }
                } else {
                    toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.no_network_connection)))
                }
            } catch(t: Throwable) {
                when(t) {
                    is IOException -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(
                        R.string.network_failure)))
                    else -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(
                        R.string.unknown_error)))
                }
            }
        }
    }
}