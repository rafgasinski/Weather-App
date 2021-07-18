package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.R
import com.example.weatherapp.model.db.PlacesDatabase
import com.example.weatherapp.model.db.Place
import com.example.weatherapp.model.repositories.ApiRepository
import com.example.weatherapp.model.repositories.PlacesRepository
import com.example.weatherapp.utils.Event
import com.example.weatherapp.utils.isOnline
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect
import java.io.IOException

class PlacesListViewModel(application: Application): AndroidViewModel(application) {
    private val forecastRepository = ApiRepository()
    private val repository: PlacesRepository

    val toastMessage = MutableLiveData<Event<String>>()
    val allPlacesLiveData = MutableLiveData<List<Place>>()

    var listToUpdate = true

    private var maxOrder = 0

    private var allPlaces: List<Place> = listOf()

    init {
        val placeDao = PlacesDatabase.getDatabase(
            application
        ).placeDao()
        repository = PlacesRepository(placeDao)

        viewModelScope.launch {
            repository.selectAllAsFlow.collect {
                allPlacesLiveData.postValue(it)
            }
        }
    }

    fun updateList() {
        viewModelScope.launch {
            launch {
                allPlaces = repository.selectAll()
            }.join()

            if(isOnline(getApplication<Application>())) {
                allPlaces.forEachIndexed { index, place ->
                    if(index == 0){
                        maxOrder = place.order
                    }
                    getCityDataUpdate(place.name, place.id, place.order)
                }

                listToUpdate = false
            }
        }
    }

    private fun addPlace(place: Place) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.add(place)
        }
    }

    fun deletePlace(place: Place) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(place)
        }
    }

    private fun update(place: Place) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(place)
        }
    }

    fun updateList(listPlace: List<Place>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateList(listPlace)
        }
    }

    fun deleteMultiple(idList: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMultiple(idList)
        }
    }

    fun getCityDataAdd(name: String) {
        viewModelScope.launch {
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = forecastRepository.getCityData(name)
                    if(response.isSuccessful) {
                        val responseBody = response.body()!!

                        val isDay = responseBody.weather[0].icon.last().toString() == "d"

                        val cityCountry = String.format(getApplication<Application>().resources.getString(R.string.place_country), responseBody.name, responseBody.sys.countryCode)
                        if(allPlacesLiveData.value?.any { it.name == cityCountry} == false) {
                            addPlace(Place(0, cityCountry, responseBody.coord.lat, responseBody.coord.lon, responseBody.main.temp.roundToInt(),
                                responseBody.main.tempMax.roundToInt(), responseBody.main.tempMin.roundToInt(), isDay, ++maxOrder))
                        } else {
                            toastMessage.postValue(Event(getApplication<Application>().resources.getString(R.string.already_added)))
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

    private fun getCityDataUpdate(name: String, id: Int, order: Int) {
        viewModelScope.launch {
            try {
                if(isOnline(getApplication<Application>())) {
                    val response = forecastRepository.getCityData(name)

                    if(response.isSuccessful) {
                        val responseBody = response.body()!!

                        val isDay = responseBody.weather[0].icon.last().toString() == "d"

                        update(Place(id, name, responseBody.coord.lat, responseBody.coord.lon, responseBody.main.temp.roundToInt(),
                            responseBody.main.tempMax.roundToInt(), responseBody.main.tempMin.roundToInt(), isDay, order))
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