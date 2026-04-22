package com.pce.itassistant.ui.chatbot

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pce.itassistant.data.ChatMessage
import com.pce.itassistant.data.MatchingFile
import com.pce.itassistant.repository.ChatRepository
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChatbotViewModel(private val context: Context) : ViewModel() {
    private val chatRepository = ChatRepository(context)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus

    private val sessionManager = SessionManager(context)

    init {
        checkConnection()
    }

    fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun sendMessage(message: String) {
        Log.d("ChatbotViewModel", "sendMessage called with message: '$message'")

        val student = sessionManager.getStudent()
        if (student == null) {
            _messages.value = _messages.value + ChatMessage(
                content = "Session expired. Please login again.",
                isUser = false,
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            addMessage(ChatMessage(content = message, isUser = true))

            val result = chatRepository.sendMessage(
                message = message,
                erpNumber = student.erpNumber,
                year = student.year,
                semester = student.semester,
                course = null
            )

            if (result.isSuccess) {
                val botMessage = result.getOrNull()
                if (botMessage != null) {
                    _messages.value = _messages.value + botMessage
                } else {
                    _messages.value = _messages.value + ChatMessage(
                        content = "Empty response received from server.",
                        isUser = false,
                        isError = true
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                Log.e("ChatbotViewModel", "Send message failed", exception)
                _messages.value = _messages.value + ChatMessage(
                    content = "Sorry, I couldn't process your request. Please try again.\n\nError: ${exception?.message ?: "Unknown error"}",
                    isUser = false,
                    isError = true
                )
            }

            _isLoading.value = false
        }
    }

    fun downloadFromMessage(message: ChatMessage, context: Context) {
        val fileDownload = message.fileDownload ?: run {
            showTemporaryStatus("No downloadable file found.")
            return
        }

        val filename = fileDownload.filename
        if (filename.isNullOrBlank()) {
            showTemporaryStatus("Filename is missing.")
            return
        }

        viewModelScope.launch {
            _downloadStatus.value = "Downloading $filename..."

            val result = chatRepository.downloadFromUrl(fileDownload, context)

            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    _downloadStatus.value = "Downloaded successfully: ${file.name}"
                    openFile(file, context)
                    delay(2000)
                    _downloadStatus.value = null
                } else {
                    _downloadStatus.value = "Download failed: Empty file"
                    delay(3000)
                    _downloadStatus.value = null
                }
            } else {
                val exception = result.exceptionOrNull()
                Log.e("ChatbotViewModel", "Download failed", exception)
                _downloadStatus.value = "Download failed: ${exception?.message}"
                delay(3000)
                _downloadStatus.value = null
            }
        }
    }

    fun downloadMatchingFile(matchingFile: MatchingFile, context: Context) {
        val filename = matchingFile.filename
        if (filename.isNullOrBlank()) {
            showTemporaryStatus("Filename is missing.")
            return
        }

        viewModelScope.launch {
            _downloadStatus.value = "Downloading $filename..."

            val result = chatRepository.downloadMatchingFile(matchingFile, context)

            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    _downloadStatus.value = "Downloaded successfully: ${file.name}"
                    openFile(file, context)
                    delay(2000)
                    _downloadStatus.value = null
                } else {
                    _downloadStatus.value = "Download failed: Empty file"
                    delay(3000)
                    _downloadStatus.value = null
                }
            } else {
                val exception = result.exceptionOrNull()
                Log.e("ChatbotViewModel", "Matching file download failed", exception)
                _downloadStatus.value = "Download failed: ${exception?.message}"
                delay(3000)
                _downloadStatus.value = null
            }
        }
    }

    fun downloadFile(filename: String, context: Context) {
        if (filename.isBlank()) {
            showTemporaryStatus("Filename is missing.")
            return
        }

        viewModelScope.launch {
            _downloadStatus.value = "Downloading $filename..."

            val result = chatRepository.downloadFile(filename, context)

            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    _downloadStatus.value = "Downloaded successfully: ${file.name}"
                    openFile(file, context)
                    delay(2000)
                    _downloadStatus.value = null
                } else {
                    _downloadStatus.value = "Download failed: Empty file"
                    delay(3000)
                    _downloadStatus.value = null
                }
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
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.extension))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ChatbotViewModel", "Failed to open file", e)
            viewModelScope.launch {
                _downloadStatus.value = "Cannot open file: No app available"
                delay(3000)
                _downloadStatus.value = null
            }
        }
    }

    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "txt" -> "text/plain"
            else -> "*/*"
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

    private fun showTemporaryStatus(message: String) {
        viewModelScope.launch {
            _downloadStatus.value = message
            delay(2500)
            _downloadStatus.value = null
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatbotViewModel(context) as T
                }
            }
    }
}
