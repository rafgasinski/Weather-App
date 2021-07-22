package com.example.weatherapp.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.weatherapp.model.db.location.Location
import com.example.weatherapp.model.db.location.LocationDao
import com.example.weatherapp.model.db.saved.SavedResponse
import com.example.weatherapp.model.db.saved.SavedResponseDao

@Database(entities = [Location::class, SavedResponse::class], version = 1, exportSchema = false) @TypeConverters(Converters::class)

abstract class MainDatabase: RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun savedDao(): SavedResponseDao

    companion object {
        @Volatile
        private var INSTANCE: MainDatabase? = null

        fun getDatabase(context: Context): MainDatabase{
            val tempInstance = INSTANCE

            if(tempInstance != null){
                return tempInstance
            }
            else
                synchronized(this){
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        MainDatabase::class.java,
                        "MainDatabase"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    return instance
                }
        }
    }

}