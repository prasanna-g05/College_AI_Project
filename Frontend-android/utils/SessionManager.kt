package com.pce.itassistant.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.pce.itassistant.data.AuthSession
import com.pce.itassistant.data.Student

class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("PCE_SESSION", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_STUDENT_DATA = "student_data"
        private const val KEY_ERP_NUMBER = "erp_number"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
    }

    fun saveLoginSession(authSession: AuthSession) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_STUDENT_DATA, gson.toJson(authSession.student))
            putString(KEY_ERP_NUMBER, authSession.student.erpNumber)
            putString(KEY_ACCESS_TOKEN, authSession.accessToken)
            putString(KEY_TOKEN_TYPE, authSession.tokenType)
            apply()
        }
    }

    fun getStudent(): Student? {
        val studentJson = prefs.getString(KEY_STUDENT_DATA, null)
        return studentJson?.let {
            try {
                gson.fromJson(it, Student::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun updateStudent(student: Student) {
        prefs.edit().apply {
            putString(KEY_STUDENT_DATA, gson.toJson(student))
            putString(KEY_ERP_NUMBER, student.erpNumber)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !token.isNullOrBlank()
    }

    fun getErpNumber(): String? {
        return prefs.getString(KEY_ERP_NUMBER, null)
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getTokenType(): String {
        return prefs.getString(KEY_TOKEN_TYPE, "bearer") ?: "bearer"
    }

    fun getAuthHeader(): String? {
        val token = getAccessToken()
        if (token.isNullOrBlank()) return null
        return "${getTokenType().replaceFirstChar { it.uppercase() }} $token"
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
