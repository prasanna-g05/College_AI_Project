package com.pce.itassistant.ui.chatbot

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pce.itassistant.data.ChatMessage
import com.pce.itassistant.repository.ChatRepository
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class ChatbotViewModel(private val context: Context) : ViewModel() {  // Inject Context
    private val chatRepository = ChatRepository(context)  // Pass Context to repo

    private val _messages = kotlinx.coroutines.flow.MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: kotlinx.coroutines.flow.StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoading: kotlinx.coroutines.flow.StateFlow<Boolean> = _isLoading

    private val _downloadStatus = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val downloadStatus: kotlinx.coroutines.flow.StateFlow<String?> = _downloadStatus

    private val _connectionStatus = kotlinx.coroutines.flow.MutableStateFlow(false)
    val connectionStatus: kotlinx.coroutines.flow.StateFlow<Boolean> = _connectionStatus

    private val sessionManager = SessionManager(context)  // For student context

    init {
        checkConnection()
    }

    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun sendMessage(message: String) {  // Remove erpNumber; fetch from SessionManager
        Log.d("ChatbotViewModel", "sendMessage called with message: '$message'")
        val student = sessionManager.getStudent()
        val erpNumber = student!!.erpNumber
        // Optional: Extract year/semester/course from student or shared prefs if available

        viewModelScope.launch {
            _isLoading.value = true
            addMessage(ChatMessage(content = message, isUser = true))  // Add user message immediately

            val result = chatRepository.sendMessage(
                message = message,
                erpNumber = erpNumber,
                year = student.year,  // Assume Student has these; add if needed
                semester = student.semester,
                course = null  // Or from current screen state
            )

            if (result.isSuccess) {
                val botMessage = result.getOrNull()!!  // Safe cast post-check
                _messages.value = _messages.value + botMessage
            } else {
                val exception = result.exceptionOrNull()
                Log.e("ChatbotViewModel", "Send message failed", exception)
                val errorMessage = ChatMessage(
                    content = "Sorry, I couldn't process your request. Please try again.\n\nError: ${exception?.message ?: "Unknown error"}",
                    isUser = false,
                    isError = true
                )
                _messages.value = _messages.value + errorMessage
            }

            _isLoading.value = false
        }
    }

    fun downloadFromMessage(message: ChatMessage, context: Context) {  // New: Handle FileDownload in message
        val fileDownload = message.fileDownload
        if (fileDownload != null) {
            viewModelScope.launch {
                _downloadStatus.value = "Downloading ${fileDownload.filename}..."
                val result = if (fileDownload.url.isNotEmpty()) {
                    chatRepository.downloadFromUrl(fileDownload, context)  // If URL provided
                } else {
                    chatRepository.downloadFile(fileDownload.filename, context)  // Fallback to filename
                }

                if (result.isSuccess) {
                    val file = result.getOrNull()!!
                    _downloadStatus.value = "Downloaded successfully: ${file.name}"
                    openFile(file, context)
                    delay(2000)
                    _downloadStatus.value = null
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e("ChatbotViewModel", "Download failed", exception)
                    _downloadStatus.value = "Download failed: ${exception?.message}"
                    delay(3000)
                    _downloadStatus.value = null
                }
            }
        }
    }

    fun downloadFile(filename: String, context: Context) {  // Legacy; prefer downloadFromMessage for full integration
        viewModelScope.launch {
            _downloadStatus.value = "Downloading $filename..."

            val result = chatRepository.downloadFile(filename, context)

            if (result.isSuccess) {
                val file = result.getOrNull()!!
                _downloadStatus.value = "Downloaded successfully: ${file.name}"
                openFile(file, context)
                delay(2000)
                _downloadStatus.value = null
            } else {
                val exception = result.exceptionOrNull()
                Log.e("ChatbotViewModel", "Download failed", exception)
                _downloadStatus.value = "Download failed: ${exception?.message}"
                delay(3000)
                _downloadStatus.value = null
            }
        }
    }


    private fun openFile(file: File, context: Context) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",  // Ensure <provider> in Manifest
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.extension))  // Dynamic MIME
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ChatbotViewModel", "Failed to open file", e)
            _downloadStatus.value = "Cannot open file: No app available"
            viewModelScope.launch { delay(3000); _downloadStatus.value = null }
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "jpg", "jpeg", "png" -> "image/jpeg"
            else -> "*/*"  // Generic fallback
        }
    }

    private fun checkConnection() {
        viewModelScope.launch {
            _connectionStatus.value = chatRepository.checkHealth()
            if (!_connectionStatus.value) {
                Log.w("ChatbotViewModel", "Backend connection unhealthy; check FastAPI server")
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    // ViewModelFactory for Context injection in Compose (use in ChatScreen)
    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatbotViewModel(context) as T
            }
        }
    }
}
