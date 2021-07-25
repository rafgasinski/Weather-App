package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.model.db.MainDatabase
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.model.db.saved.SavedResponse
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.repositories.LocationRepository
import com.example.weatherapp.model.repositories.SavedResponseRepository
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
    private val savedResponseRepository: SavedResponseRepository

    val shouldNavigate = MutableLiveData(false)
    val toastMessage = MutableLiveData<Event<String>>()

    init {
        val locationDao = MainDatabase.getDatabase(
            application
        ).locationDao()
        val savedDao = MainDatabase.getDatabase(
            application
        ).savedDao()
        locationRepository = LocationRepository(locationDao)
        savedResponseRepository = SavedResponseRepository(savedDao)
    }

    private fun addLocation(location: Location) {
        viewModelScope.launch {
            preferencesManager.locationId = locationRepository.add(location).toInt()
        }
    }

    private fun addWeatherResponse(savedResponse: SavedResponse) {
        viewModelScope.launch {
            savedResponseRepository.add(savedResponse)
        }
    }

    fun getCityDataByName(name: String) {
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

                        val responseWeatherData = apiRepository.getOneCall(lat, lon)

                        if(responseWeatherData.isSuccessful) {
                            val responseWeatherDataBody = responseWeatherData.body()!!

                            preferencesManager.city = city
                            preferencesManager.countryCode = countryCode
                            preferencesManager.lat = lat
                            preferencesManager.lon = lon
                            preferencesManager.useBackgroundDay = isDay
                            preferencesManager.timeZone = responseWeatherDataBody.timezone

                            addWeatherResponse(SavedResponse(0, preferencesManager.locationId + 1, city, countryCode, responseWeatherDataBody))

                            addLocation(
                                Location(0, city, countryCode, lat, lon, responseWeatherDataBody.current.dt, responseWeatherDataBody.current.temp.roundToInt(),
                                    responseWeatherDataBody.daily[0].temp.max.roundToInt(), responseWeatherDataBody.daily[0].temp.min.roundToInt(), isDay, 0)
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

    fun getCityDataByCoord(lat: Double, lon: Double, city: String, countryCode: String) {
        viewModelScope.launch {
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = apiRepository.getOneCall(lat, lon)
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

                        addWeatherResponse(SavedResponse(0, preferencesManager.locationId + 1, city, countryCode, responseCityData))

                        addLocation(
                            Location(0, city, countryCode, lat, lon, currentData.dt, currentData.temp.roundToInt(),
                                responseCityData.daily[0].temp.max.roundToInt(), responseCityData.daily[0].temp.min.roundToInt(), isDay, 0)
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