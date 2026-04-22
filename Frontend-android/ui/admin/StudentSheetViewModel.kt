package com.pce.itassistant.ui.admin

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pce.itassistant.data.Student
import com.pce.itassistant.network.StudentSheetResponse
import com.pce.itassistant.repository.AdminRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class StudentSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)

    var sheets by mutableStateOf<List<StudentSheetResponse>>(emptyList())
        private set

    var approvedStudents by mutableStateOf<List<Student>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isUploading by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }

    fun loadSheets() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = repository.getStudentSheets()

            if (result.isSuccess) {
                sheets = result.getOrNull() ?: emptyList()
            } else {
                errorMessage =
                    result.exceptionOrNull()?.message ?: "Failed to load student sheets"
            }

            isLoading = false
        }
    }

    fun loadApprovedStudents() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = repository.getApprovedStudents()

            if (result.isSuccess) {
                approvedStudents = result.getOrNull() ?: emptyList()
            } else {
                errorMessage =
                    result.exceptionOrNull()?.message ?: "Failed to load approved students"
            }

            isLoading = false
        }
    }

    fun uploadSheet(file: File) {
        viewModelScope.launch {
            isUploading = true
            successMessage = null
            errorMessage = null

            try {
                val requestFile = file.asRequestBody(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        .toMediaTypeOrNull()
                )

                val body = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    requestFile
                )

                val result = repository.uploadStudentSheet(body)

                if (result.isSuccess) {
                    successMessage = result.getOrNull() ?: "Student sheet uploaded successfully"
                    loadSheets()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Upload failed"
                }
            } catch (e: Exception) {
                errorMessage = "Upload error: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }
}