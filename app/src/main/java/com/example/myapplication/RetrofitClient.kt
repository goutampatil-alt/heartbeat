package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // For Physical Device: use your PC's local IP
    // For Android Emulator: use 10.0.2.2 instead
    private const val BASE_URL = "http://10.41.74.246:8000/"

    val api: HealthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HealthApiService::class.java)
    }
}