package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.weatherapp.R
import com.example.weatherapp.model.db.MainDatabase
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.model.db.location.LocationUpdate
import com.example.weatherapp.model.db.saved.SavedResponse
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.repositories.LocationRepository
import com.example.weatherapp.model.repositories.SavedResponseRepository
import com.example.weatherapp.model.response.onecall.OneCallResponse
import com.example.weatherapp.utils.*
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

class CurrentWeatherViewModel(application: Application): AndroidViewModel(application) {
    private val apiRepository = ApiRepository()
    private val locationRepository: LocationRepository
    private val savedResponseRepository: SavedResponseRepository

    val responseData: MutableLiveData<Resource<OneCallResponse>> = MutableLiveData()
    val savedWeatherData: LiveData<OneCallResponse>

    init {
        val locationDao = MainDatabase.getDatabase(
            application
        ).locationDao()
        val savedDao = MainDatabase.getDatabase(
            application
        ).savedDao()
        locationRepository = LocationRepository(locationDao)
        savedResponseRepository = SavedResponseRepository(savedDao)

        savedWeatherData = savedResponseRepository.observeSavedResponse(preferencesManager.city!!, preferencesManager.countryCode!!)
    }

    private fun addLocation(location: Location) {
        viewModelScope.launch {
            preferencesManager.locationId = locationRepository.add(location).toInt()
        }
    }

    private fun updateLocation(location: LocationUpdate) {
        viewModelScope.launch {
            locationRepository.update(location)
        }
    }

    fun upsertWeatherResponse(savedResponse: SavedResponse) {
        viewModelScope.launch {
            var dbData = listOf<Int>()
            launch {
                dbData = savedResponseRepository.getSavedResponse(savedResponse.city, savedResponse.countryCode)
            }.join()

            viewModelScope.launch {
                if(dbData.isEmpty()) {
                    savedResponseRepository.add(savedResponse)
                } else {
                    savedResponseRepository.update(savedResponse.city, savedResponse.countryCode, savedResponse.oneCallResponse)
                }
            }
        }
    }

    private fun upsertLocation(location: Location) {
        viewModelScope.launch {
            var dbData = listOf<Int>()
            launch {
                dbData = locationRepository.getLocation(location.city, location.countryCode)
            }.join()

            if(dbData.isEmpty()) {
                var maxOrderList = listOf<Int>()
                var maxOrder = 0
                launch {
                    maxOrderList = locationRepository.getMaxOrder()
                }.join()

                if(maxOrderList.isNotEmpty()) {
                    maxOrder = maxOrderList.first()
                    maxOrder++
                }

                addLocation(
                    Location(0, location.city, location.countryCode, location.lat, location.lon, location.dt, location.currentTemp,
                        location.tempMax, location.tempMin, location.isDay, maxOrder)
                )

            } else {
                updateLocation(LocationUpdate(location.id, location.dt, location.currentTemp,
                    location.tempMax, location.tempMin, location.isDay))
            }
        }
    }

    fun getOneCallForecast(lat: Double, lon: Double) {
        viewModelScope.launch {
            responseData.postValue(Resource.Loading())
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = apiRepository.getOneCall(lat, lon)

                    if(response.isSuccessful) {
                        response.body()?.let { resultResponse ->

                            val responseCurrentDt = resultResponse.current.dt
                            val responseCurrentTemp = resultResponse.current.temp.roundToInt()
                            val responseTempMax = resultResponse.daily[0].temp.max.roundToInt()
                            val responseTempMin = resultResponse.daily[0].temp.min.roundToInt()
                            val responseIsDay = resultResponse.current.weather[0].icon.last().toString() == "d"

                            updateLocation(LocationUpdate(preferencesManager.locationId, responseCurrentDt, responseCurrentTemp,
                                responseTempMax, responseTempMin, responseIsDay))

                            responseData.postValue(Resource.Success(resultResponse))
                        }
                    } else {
                        responseData.postValue(Resource.Error(Event(response.message())))
                    }
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
    }

    fun getCityDataToAddByCoord(lat: Double, lon: Double, city: String, countryCode: String) {
        viewModelScope.launch {
            responseData.postValue(Resource.Loading())
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = apiRepository.getOneCall(lat, lon)

                    if(response.isSuccessful) {
                        val responseCityData = response.body()!!

                        val responseCurrentDt = responseCityData.current.dt
                        val responseCurrentTemp = responseCityData.current.temp.roundToInt()
                        val responseTempMax = responseCityData.daily[0].temp.max.roundToInt()
                        val responseTempMin = responseCityData.daily[0].temp.min.roundToInt()
                        val responseIsDay = responseCityData.current.weather[0].icon.last().toString() == "d"

                        preferencesManager.city = city
                        preferencesManager.countryCode = countryCode
                        preferencesManager.lat = lat
                        preferencesManager.lon = lon

                        upsertLocation(Location(preferencesManager.locationId, city, countryCode, lat, lon, responseCurrentDt,
                            responseCurrentTemp, responseTempMax, responseTempMin, responseIsDay, 0))

                        responseData.postValue(Resource.Success(responseCityData))
                    } else {
                        responseData.postValue(Resource.Error(Event(response.message())))
                    }
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
    }

}