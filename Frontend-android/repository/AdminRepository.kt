package com.pce.itassistant.repository

import android.content.Context
import com.pce.itassistant.data.Student
import com.pce.itassistant.network.AdminUserDto
import com.pce.itassistant.network.ApiService
import com.pce.itassistant.network.StudentSheetResponse
import com.pce.itassistant.network.UpdateRoleRequest
import com.pce.itassistant.utils.SessionManager
import okhttp3.MultipartBody

class AdminRepository(
    private val context: Context,
    private val apiService: ApiService = ApiService.getInstance
) {
    private val sessionManager = SessionManager(context)

    private fun getAuthHeader(): String {
        val token = sessionManager.getAccessToken()
            ?: throw Exception("No access token found. Please login again.")
        return "Bearer $token"
    }

    suspend fun getCurrentUser(): Result<Student> {
        return try {
            val response = apiService.getCurrentUser(getAuthHeader())

            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Empty user response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(
                    Exception(errorBody ?: "Failed to fetch current user")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<AdminUserDto>> {
        return try {
            val response = apiService.getAllUsers(getAuthHeader())

            if (response.isSuccessful) {
                val users = response.body() ?: emptyList()
                Result.success(users)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(
                    Exception(errorBody ?: "Failed to fetch users")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(erpNumber: String, newRole: String): Result<String> {
        return try {
            val response = apiService.updateUserRole(
                erpNumber = erpNumber,
                authHeader = getAuthHeader(),
                request = UpdateRoleRequest(role = newRole)
            )

            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.message ?: "Role updated successfully")
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(
                    Exception(errorBody ?: "Failed to update user role")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentSheets(): Result<List<StudentSheetResponse>> {
        return try {
            val response = apiService.getStudentSheets(getAuthHeader())

            if (response.isSuccessful) {
                val sheets = response.body() ?: emptyList()
                Result.success(sheets)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(
                    Exception(errorBody ?: "Failed to fetch student sheets")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadStudentSheet(file: MultipartBody.Part): Result<String> {
        return try {
            val response = apiService.uploadStudentSheet(
                authHeader = getAuthHeader(),
                file = file
            )

            if (response.isSuccessful) {
                Result.success("Student sheet uploaded successfully")
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(
                    Exception(errorBody ?: "Failed to upload student sheet")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApprovedStudents(): Result<List<Student>> {
        return try {
            val response = apiService.getApprovedStudents(getAuthHeader())

            if (response.isSuccessful) {
                val students = response.body() ?: emptyList()
                Result.success(students)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(
                    Exception(errorBody ?: "Failed to fetch approved students")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}