package com.example.weatherapp.model.repositories

import com.example.weatherapp.model.db.Place
import com.example.weatherapp.model.db.PlaceDao

class PlacesRepository(private val placeDao: PlaceDao) {

    val selectAllAsFlow = placeDao.observeAll()

    suspend fun selectAll() : List<Place> {
        return placeDao.selectAll()
    }

    fun add(place: Place){
        placeDao.add(place)
    }

    fun delete(place: Place){
        placeDao.delete(place)
    }

    fun update(place: Place){
        placeDao.update(place)
    }

    fun updateList(placeList: List<Place>){
        placeDao.updateList(placeList)
    }

    fun deleteMultiple(idList: List<Int>){
        placeDao.deleteMultiple(idList)
    }

}