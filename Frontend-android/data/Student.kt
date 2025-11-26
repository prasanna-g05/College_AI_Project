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
    val rollNumber: String? = null,
    val email: String? = null,
    val phone: String? = null
) : Parcelable

