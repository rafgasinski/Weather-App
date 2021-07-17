package com.example.weatherapp.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Place::class], version = 1, exportSchema = false)
abstract class PlacesDatabase: RoomDatabase() {

    abstract fun placeDao(): PlaceDao

    companion object {
        @Volatile
        private var INSTANCE: PlacesDatabase? = null

        fun getDatabase(context: Context): PlacesDatabase{
            val tempInstance = INSTANCE

            if(tempInstance != null){
                return tempInstance
            }
            else
                synchronized(this){
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        PlacesDatabase::class.java,
                        "PlacesDatabase"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    return instance
                }
        }
    }

}