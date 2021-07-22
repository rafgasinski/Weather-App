package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.model.db.MainDatabase
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.model.db.location.LocationUpdate
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.repositories.LocationRepository
import com.example.weatherapp.model.repositories.SavedResponseRepository
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.Event
import com.example.weatherapp.utils.isOnline
import com.example.weatherapp.utils.timestampOlderThanTenMin
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect
import java.io.IOException

class LocationsListViewModel(application: Application): AndroidViewModel(application) {
    private val apiRepository = ApiRepository()
    private val locationRepository: LocationRepository
    private val savedForecastWeatherResponseRepository: SavedResponseRepository

    val toastMessage = MutableLiveData<Event<String>>()
    val allLocationsLiveData = MutableLiveData<List<Location>>()

    private var maxId = 0

    private var allLocations: List<Location> = listOf()

    init {
        val locationDao = MainDatabase.getDatabase(
            application
        ).locationDao()
        val savedDao = MainDatabase.getDatabase(
            application
        ).savedDao()
        locationRepository = LocationRepository(locationDao)
        savedForecastWeatherResponseRepository = SavedResponseRepository(savedDao)

        viewModelScope.launch {
            locationRepository.selectAllAsFlow.collect {
                allLocationsLiveData.postValue(it)
            }
        }
    }

    fun updateList() {
        viewModelScope.launch {
            launch {
                allLocations = locationRepository.selectAll()
            }.join()

            if(isOnline(getApplication<Application>())) {
                allLocations.forEachIndexed { index, location ->
                    if(index == 0){
                        maxId = location.id
                    }
                    getCityDataUpdate(location)
                }
            }
        }
    }

    private fun addLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.add(location)
        }
    }

    fun deleteLocation(location: Location) {
        viewModelScope.launch {
            maxId--
            locationRepository.delete(location)
            savedForecastWeatherResponseRepository.delete(location.city, location.countryCode)
        }
    }

    private fun update(location: LocationUpdate) {
        viewModelScope.launch {
            locationRepository.update(location)
        }
    }

    fun updateList(listLocation: List<Location>) {
        viewModelScope.launch {
            locationRepository.updateList(listLocation)
        }
    }

    fun deleteMultiple(locationList: List<Location>) {
        viewModelScope.launch {
            maxId -= locationList.size
            locationRepository.deleteMultiple(locationList.map { it.id })
            locationList.forEach { location ->
                savedForecastWeatherResponseRepository.delete(location.city, location.countryCode)
            }
        }
    }

    fun getCityDataAdd(name: String) {
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

                            if(allLocationsLiveData.value?.any { it.city == city && it.countryCode == countryCode} == false) {
                                addLocation(
                                    Location(++maxId, city, countryCode, lat, lon, responseWeatherDataBody.current.dt, responseWeatherDataBody.current.temp.roundToInt(),
                                        responseWeatherDataBody.daily[0].temp.max.roundToInt(), responseWeatherDataBody.daily[0].temp.min.roundToInt(), isDay)
                                )
                            } else {
                                toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.already_added)))
                            }
                        }
                    } else {
                        toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.unknown_location)))
                    }
                } else {
                    toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.no_network_connection)))
                }
            } catch(t: Throwable) {
                when(t) {
                    is IOException -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.network_failure)))
                    else -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.unknown_error)))
                }
            }
        }
    }

    private fun getCityDataUpdate(location: Location) {
        viewModelScope.launch {
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = apiRepository.getOneCall(location.lat, location.lon, Constants.EXCLUDE_ALERTS_HOURLY_MINUTELY)

                    if(response.isSuccessful) {
                        val responseCityData = response.body()!!

                        if(timestampOlderThanTenMin(location.dt)) {
                            val responseCurrentDt = responseCityData.current.dt
                            val responseCurrentTemp = responseCityData.current.temp.roundToInt()
                            val responseTempMax = responseCityData.daily[0].temp.max.roundToInt()
                            val responseTempMin = responseCityData.daily[0].temp.min.roundToInt()
                            val responseIsDay = responseCityData.current.weather[0].icon.last().toString() == "d"

                            if(location.currentTemp != responseCurrentTemp
                                || location.tempMax != responseTempMax
                                || location.tempMin != responseTempMin
                                || location.isDay != responseIsDay) {
                                update(LocationUpdate(location.id, responseCurrentDt, responseCurrentTemp, responseTempMax, responseTempMin, responseIsDay))
                            }
                        }
                    }
                }
            } catch(t: Throwable) {
                when(t) {
                    is IOException -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.network_failure)))
                    else -> toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.unknown_error)))
                }
            }
        }
    }

}