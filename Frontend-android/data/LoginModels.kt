package com.pce.itassistant.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("erp_number")
    val erpNumber: String,

    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String = "",

    @SerializedName("token_type")
    val tokenType: String = "",

    @SerializedName("student")
    val student: Student? = null
)

data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val student: Student
)

data class RegisterRequest(
    @SerializedName("erp_number")
    val erpNumber: String,

    val name: String,
    val year: String,
    val semester: String,

    @SerializedName("roll_number")
    val rollNumber: String,

    val password: String,
    val branch: String = "Information Technology",
    val section: String
)

data class RegisterResponse(
    val message: String = "",
    val user: Student? = null
)
