package com.pce.itassistant.ui.admin

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pce.itassistant.network.ApiService
import com.pce.itassistant.network.ChatbotFileDto
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class AdminChatbotFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiService.getInstance
    private val sessionManager = SessionManager(application)

    private val _files = MutableStateFlow<List<ChatbotFileDto>>(emptyList())
    val files: StateFlow<List<ChatbotFileDto>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null

            try {
                val token = sessionManager.getAccessToken()
                if (token.isNullOrBlank()) {
                    _statusMessage.value = "Admin token not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }

                val response = apiService.getChatbotFiles("Bearer $token")
                if (response.isSuccessful) {
                    _files.value = response.body().orEmpty()
                } else {
                    _statusMessage.value = "Failed to load files: ${response.code()}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading files: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _statusMessage.value = null

            try {
                val token = sessionManager.getAccessToken()
                if (token.isNullOrBlank()) {
                    _statusMessage.value = "Admin token not found. Please login again."
                    _isUploading.value = false
                    return@launch
                }

                val context = getApplication<Application>()
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/pdf"
                val fileName = getFileName(uri) ?: "chatbot_file.pdf"

                val inputStream = resolver.openInputStream(uri)
                    ?: throw Exception("Unable to read selected file")

                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }

                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    tempFile.name,
                    requestBody
                )

                val response = apiService.uploadChatbotFile(
                    authHeader = "Bearer $token",
                    file = multipartBody
                )

                if (response.isSuccessful) {
                    _statusMessage.value = response.body()?.message ?: "File uploaded successfully"
                    loadFiles()
                } else {
                    _statusMessage.value = "Upload failed: ${response.code()}"
                }

                tempFile.delete()
            } catch (e: Exception) {
                _statusMessage.value = "Upload error: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun deleteFile(fileId: Int) {
        viewModelScope.launch {
            _statusMessage.value = null

            try {
                val token = sessionManager.getAccessToken()
                if (token.isNullOrBlank()) {
                    _statusMessage.value = "Admin token not found. Please login again."
                    return@launch
                }

                val response = apiService.deleteChatbotFile(
                    fileId = fileId,
                    authHeader = "Bearer $token"
                )

                if (response.isSuccessful) {
                    _statusMessage.value = response.body()?.message ?: "File deleted successfully"
                    loadFiles()
                } else {
                    _statusMessage.value = "Delete failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Delete error: ${e.message}"
            }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    private fun getFileName(uri: Uri): String? {
        val context = getApplication<Application>()
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                return it.getString(nameIndex)
            }
        }
        return null
    }
}