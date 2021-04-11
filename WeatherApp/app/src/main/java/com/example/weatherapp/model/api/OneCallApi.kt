package com.example.weatherapp.model.api

import com.example.weatherapp.Constants
import com.example.weatherapp.model.response.OneCallResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface OneCallApi {
    @GET("data/2.5/onecall?")
    suspend fun getOneCallForecast(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("exclude") exclude: String
    ): Response<OneCallResponse>

    companion object {
        operator fun invoke(): OneCallApi {
            val requestInterceptor = Interceptor { chain ->
                val url = chain.request()
                        .url
                        .newBuilder()
                        .addQueryParameter("appid", Constants.Companion.WEATHER_API_KEY)
                        .addQueryParameter("units", "metric")
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

            val api: OneCallApi = Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()) // Retrofit must know how serialize the data
                    .build()
                    .create(OneCallApi::class.java)

            return api
        }
    }
}