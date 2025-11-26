package com.pce.itassistant.network  // Same as ApiService

import com.google.gson.GsonBuilder
import com.pce.itassistant.network.ApiService.Companion.BASE_URL
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // HTTP Logging (for debug: shows requests/responses in Logcat; disable for prod)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp Client with timeouts & logging
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Public Retrofit instance (used by ApiService.getInstance)
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)  // Uses shared constant from ApiServices
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder()
                .setLenient()  // Handles flexible JSON
                .create()
        ))
        .build()
}
