package com.example.weatherapp.model.repositories

import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.model.db.location.LocationDao
import com.example.weatherapp.model.db.location.LocationUpdate

class LocationRepository(private val locationDao: LocationDao) {

    val selectAllAsFlow = locationDao.observeAll()

    suspend fun selectAll() : List<Location> {
        return locationDao.selectAll()
    }

    suspend fun getLocation(city: String, countryCode: String) : List<Int> {
        return locationDao.getLocation(city, countryCode)
    }

    suspend fun getMaxOrder() : List<Int> {
        return locationDao.getMaxOrder()
    }

    suspend fun add(location: Location): Long {
        return locationDao.add(location)
    }

    suspend fun update(location: LocationUpdate){
        locationDao.update(location)
    }

    suspend fun delete(location: Location){
        locationDao.delete(location)
    }

    suspend fun updateList(locationList: List<Location>){
        locationDao.updateList(locationList)
    }

    suspend fun deleteMultiple(idList: List<Int>){
        locationDao.deleteMultiple(idList)
    }

}