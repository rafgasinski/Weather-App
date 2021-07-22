package com.example.weatherapp.model.db

import androidx.room.TypeConverter
import com.example.weatherapp.model.response.onecall.OneCallResponse
import com.google.gson.Gson

class Converters {

    @TypeConverter
    fun gsonToWeather(json: String): OneCallResponse {
        return Gson().fromJson(json, OneCallResponse::class.java)
    }

    @TypeConverter
    fun weatherToGson(oneCallResponse: OneCallResponse): String {
        return Gson().toJson(oneCallResponse)
    }

}
