package com.example.weatherapp.model.db.saved

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.weatherapp.model.response.onecall.OneCallResponse


@Dao
interface SavedResponseDao {

    @Insert
    suspend fun add(savedResponse: SavedResponse)

    @Query("UPDATE saved_table SET oneCallResponse=:oneCallResponse WHERE city=:city AND countryCode=:countryCode")
    suspend fun update(city: String, countryCode: String, oneCallResponse: OneCallResponse)

    @Query("DELETE from saved_table WHERE city=:city AND countryCode=:countryCode")
    suspend fun delete(city: String, countryCode: String)

    @Query("DELETE from saved_table WHERE locationId in (:locationIdList)")
    suspend fun deleteMultiple(locationIdList: List<Int>)

    @Query("SELECT oneCallResponse FROM saved_table as OneCallResponse WHERE city=:city AND countryCode=:countryCode")
    fun observedSavedResponse(city: String, countryCode: String): LiveData<OneCallResponse>

    @Query("SELECT id FROM saved_table WHERE city=:city AND countryCode=:countryCode")
    suspend fun getSavedResponse(city: String, countryCode: String): List<Int>

}