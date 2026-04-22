package com.pce.itassistant.repository

import com.pce.itassistant.data.AuthSession
import com.pce.itassistant.data.LoginRequest
import com.pce.itassistant.data.RegisterRequest
import com.pce.itassistant.data.RegisterResponse
import com.pce.itassistant.data.Student
import com.pce.itassistant.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class AuthRepository {

    private val apiService = ApiService.getInstance

    suspend fun loginUser(erpNumber: String, password: String): Result<AuthSession> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginUser(LoginRequest(erpNumber, password))

                if (response.isSuccessful) {
                    val body = response.body()

                    val accessToken = body?.accessToken
                    val tokenType = body?.tokenType
                    val student = body?.student

                    if (!accessToken.isNullOrBlank() && !tokenType.isNullOrBlank() && student != null) {
                        Result.success(
                            AuthSession(
                                accessToken = accessToken,
                                tokenType = tokenType,
                                student = student
                            )
                        )
                    } else {
                        Result.failure(Exception("Incomplete login response from server"))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string()?.let { errorBodyStr ->
                        try {
                            val jsonObj = JSONObject(errorBodyStr)
                            jsonObj.optString("detail", "Login failed")
                        } catch (e: Exception) {
                            "Login failed"
                        }
                    } ?: "Login failed"

                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun registerUser(request: RegisterRequest): Result<Student> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<RegisterResponse> = apiService.registerUser(request)
                if (response.isSuccessful) {
                    response.body()?.let { registerResponse ->
                        if (registerResponse.user != null) {
                            Result.success(registerResponse.user)
                        } else {
                            Result.failure(Exception(registerResponse.message.ifBlank { "Registration failed" }))
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorMsg = response.errorBody()?.string()?.let { errorBodyStr ->
                        try {
                            val jsonObj = JSONObject(errorBodyStr)
                            jsonObj.optString("detail", "Registration failed")
                        } catch (e: Exception) {
                            "Registration failed"
                        }
                    } ?: "Registration failed"

                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun checkApiHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.healthCheck()
                response.isSuccessful && response.code() == 200
            } catch (e: Exception) {
                false
            }
        }
    }
}
