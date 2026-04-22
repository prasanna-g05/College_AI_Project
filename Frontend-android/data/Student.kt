package com.pce.itassistant.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Student(
    @SerializedName("erp_number")
    val erpNumber: String,
    val name: String,
    val branch: String? = null,
    val year: String? = null,
    val semester: String? = null,
    val section: String? = null,
    @SerializedName("roll_number")
    val rollNumber: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String = "student",  // ✅ NEW: Added for admin detection
    @SerializedName("is_active")
    val isActive: Boolean = true   // ✅ NEW: Backend field
) : Parcelable
