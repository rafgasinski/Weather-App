package com.example.weatherapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.model.db.PlacesDatabase
import com.example.weatherapp.model.db.Search
import com.example.weatherapp.model.repositories.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlacesListViewModel(application: Application): AndroidViewModel(application) {

    val allPlaces: LiveData<List<Search>>
    private val repository: SearchRepository

    init {
        val searchDao = PlacesDatabase.getDatabase(
            application
        ).searchDao()
        repository = SearchRepository(searchDao)
        allPlaces = repository.selectAllData
    }

    fun addPlace(search: Search){
        viewModelScope.launch(Dispatchers.IO) {
            repository.add(search)
        }
    }

    fun deletePlace(search: Search){
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(search)
        }
    }
}