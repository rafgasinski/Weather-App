package com.example.weatherapp.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places_table")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val lat: Double,
    val lon: Double,
    val currentTemp: Int,
    val tempMax: Int,
    val tempMin: Int,
    val isDay: Boolean,
    var order: Int
)