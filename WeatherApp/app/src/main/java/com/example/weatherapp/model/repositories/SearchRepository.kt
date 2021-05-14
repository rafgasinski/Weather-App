package com.example.weatherapp.model.repositories

import androidx.lifecycle.LiveData
import com.example.weatherapp.model.db.Search
import com.example.weatherapp.model.db.SearchDao

class SearchRepository(private val searchDao: SearchDao) {

    val selectAllData: LiveData<List<Search>> = searchDao.selectAll()

    suspend fun add(search: Search){
        searchDao.add(search)
    }

    suspend fun delete(search: Search){
        searchDao.delete(search)
    }

}