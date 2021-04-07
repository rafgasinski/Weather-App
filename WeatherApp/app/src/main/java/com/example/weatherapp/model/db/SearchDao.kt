package com.example.weatherapp.model.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(search: Search)

    @Delete
    suspend fun delete(search: Search)

    @Query("DELETE FROM searches_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM searches_table ORDER BY id DESC")
    fun selectAll(): LiveData<List<Search>>
}