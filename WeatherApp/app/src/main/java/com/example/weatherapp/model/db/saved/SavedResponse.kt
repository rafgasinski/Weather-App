package com.example.weatherapp.model.db.saved

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.weatherapp.model.response.onecall.OneCallResponse

@Entity(tableName = "saved_table")
data class SavedResponse(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val locationId: Int,
    val city: String,
    val countryCode: String,
    val oneCallResponse: OneCallResponse
)