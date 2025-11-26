package com.pce.itassistant.repository

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

    // Updated loginUser method
    suspend fun loginUser(erpNumber: String, password: String): Result<Student> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginUser(LoginRequest(erpNumber, password))
                if (response.isSuccessful) {
                    response.body()?.student?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("User data missing in login response"))
                } else {
                    // Try to parse error message from response error body
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
                            Result.failure(Exception(registerResponse.message ?: "Registration failed"))
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Result.failure(Exception("Registration failed: HTTP ${response.code()} - ${response.message()}"))
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
