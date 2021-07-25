package com.example.weatherapp.model.db.location

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations_table")
data class Location(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val city: String,
    val countryCode: String,
    val lat: Double,
    val lon: Double,
    val dt: Int,
    val currentTemp: Int,
    val tempMax: Int,
    val tempMin: Int,
    val isDay: Boolean,
    var order: Int
)

data class LocationUpdate(
    val id: Int,
    val dt: Int,
    val currentTemp: Int,
    val tempMax: Int,
    val tempMin: Int,
    val isDay: Boolean,
)
