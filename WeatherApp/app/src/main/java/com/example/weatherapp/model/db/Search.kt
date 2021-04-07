package com.example.weatherapp.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "searches_table")
data class Search(@PrimaryKey(autoGenerate = true) val id: Int, val placeId: String, val name: String, val country: String, val lat: Double, val lon: Double)