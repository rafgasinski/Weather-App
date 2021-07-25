package com.example.weatherapp.model.db.location

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert
    suspend fun add(location: Location): Long

    @Delete
    suspend fun delete(location: Location)

    @Update(entity = Location::class)
    suspend fun update(location: LocationUpdate)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateList(locationList: List<Location>)

    @Query("DELETE from locations_table WHERE id in (:idList)")
    suspend fun deleteMultiple(idList: List<Int>)

    @Query("SELECT * FROM locations_table ORDER BY `order` DESC")
    fun observeAll(): Flow<List<Location>>

    @Query("SELECT * FROM locations_table ORDER BY `order` DESC")
    suspend fun selectAll(): List<Location>

    @Query("SELECT id FROM locations_table WHERE city=:city AND countryCode=:countryCode")
    suspend fun getLocation(city: String, countryCode: String): List<Int>

    @Query("SELECT MAX(`order`) FROM locations_table")
    suspend fun getMaxOrder(): List<Int>

}