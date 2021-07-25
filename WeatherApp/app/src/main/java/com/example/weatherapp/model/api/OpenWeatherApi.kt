package com.example.weatherapp.model.api

import com.example.weatherapp.model.response.city.CityResponse
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.model.response.onecall.OneCallResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface OpenWeatherApi {
    @GET("onecall?")
    suspend fun getOneCallForecast(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("exclude") exclude: String
    ): Response<OneCallResponse>

    @GET("weather?")
    suspend fun getCityData(
        @Query("q") name: String,
    ): Response<CityResponse>

    companion object {
        operator fun invoke(): OpenWeatherApi {
            val requestInterceptor = Interceptor { chain ->
                val url = chain.request()
                    .url
                    .newBuilder()
                    .addQueryParameter("appid", Constants.API_KEY)
                    .addQueryParameter("units", Constants.UNITS)
                    .build()

                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()

                return@Interceptor chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenWeatherApi::class.java)
        }
    }

}