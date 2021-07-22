package com.example.weatherapp.model.repositories

import androidx.lifecycle.LiveData
import com.example.weatherapp.model.db.saved.SavedResponse
import com.example.weatherapp.model.db.saved.SavedResponseDao
import com.example.weatherapp.model.response.onecall.OneCallResponse

class SavedResponseRepository(private val savedResponseDao: SavedResponseDao) {

    fun observeSavedWeather(city: String, countryCode: String) : LiveData<OneCallResponse> {
        return savedResponseDao.observeSavedWeather(city, countryCode)
    }

    suspend fun getSavedWeather(city: String, countryCode: String) : List<Int> {
        return savedResponseDao.getSavedWeather(city, countryCode)
    }

    suspend fun add(savedResponse: SavedResponse){
        savedResponseDao.add(savedResponse)
    }

    suspend fun update(city: String, countryCode: String, oneCallResponse: OneCallResponse) {
        savedResponseDao.update(city, countryCode, oneCallResponse)
    }

    suspend fun delete(city: String, countryCode: String){
        savedResponseDao.delete(city, countryCode)
    }

}