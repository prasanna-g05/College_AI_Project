package com.pce.itassistant.network

import com.pce.itassistant.data.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Phase 2 backend auth endpoints: /auth/login and /auth/register
    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    // Other existing endpoints
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>

    @GET("download/{filename}")
    suspend fun downloadFile(@Path("filename") filename: String): Response<ResponseBody>

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>

    @GET("stats")
    suspend fun getStats(): Response<Map<String, Any>>

    companion object {
        const val BASE_URL = "http://10.0.2.2:8000/" // For emulator

        // Lazy singleton instance of ApiService using RetrofitClient (shared Retrofit instance)
        val getInstance: ApiService by lazy {
            RetrofitClient.retrofit.create(ApiService::class.java)
        }
    }
}
