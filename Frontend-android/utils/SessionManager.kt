package com.pce.itassistant.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.pce.itassistant.data.Student

class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("PCE_SESSION", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_STUDENT_DATA = "student_data"
        private const val KEY_ERP_NUMBER = "erp_number"
    }

    fun saveLoginSession(student: Student) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_STUDENT_DATA, gson.toJson(student))
            putString(KEY_ERP_NUMBER, student.erpNumber)
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

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun getErpNumber(): String? {
        return prefs.getString(KEY_ERP_NUMBER, null)
    }
}
