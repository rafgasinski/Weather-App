package com.example.weatherapp.model.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface PlaceDao {

    @Insert
    fun add(place: Place)

    @Delete
    fun delete(place: Place)

    @Update
    fun update(place: Place)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateList(placeList: List<Place>)

    @Query("DELETE from places_table where id in (:idList)")
    fun deleteMultiple(idList: List<Int>)

    @Query("SELECT * FROM places_table ORDER BY `order` DESC")
    fun observeAll(): Flow<List<Place>>

    @Query("SELECT * FROM places_table ORDER BY `order` DESC")
    suspend fun selectAll(): List<Place>

}