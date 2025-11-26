package com.pce.itassistant.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.pce.itassistant.data.ChatMessage
import com.pce.itassistant.data.ChatRequest
import com.pce.itassistant.data.ChatResponse
import com.pce.itassistant.data.FileDownload
import com.pce.itassistant.network.ApiService
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ChatRepository(private val context: Context) {  // Inject Context for downloads; optional for SessionManager
    private val apiService = ApiService.getInstance  // Fixed reference

    suspend fun sendMessage(
        message: String,
        erpNumber: String? = null,
        year: String? = null,
        semester: String? = null,
        course: String? = null
    ): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChatRepository", "Creating ChatRequest with query='$message', year=$year, semester=$semester")
                val request = ChatRequest(
                    query = message,
                    erpNumber = erpNumber,
                    year = year ?: getCurrentYearFromSession(),  // Optional: Fetch from SessionManager
                    semester = semester,
                    course = course
                )
                val response: Response<ChatResponse> = apiService.sendMessage(request)

                if (response.isSuccessful) {
                    response.body()?.let { chatResponse ->
                        // Comprehensive logging for RAG debugging
                        Log.d("ChatRepository", "Full response: $chatResponse")
                        Log.d("ChatRepository", "Type: ${chatResponse.type}")
                        Log.d("ChatRepository", "Answer: ${chatResponse.answer}")
                        Log.d("ChatRepository", "Context size: ${chatResponse.context.size}")
                        Log.d("ChatRepository", "Docs: ${chatResponse.docs}")
                        Log.d("ChatRepository", "File download: ${chatResponse.fileDownload}")

                        Result.success(
                            ChatMessage(
                                content = chatResponse.answer,
                                isUser = false,
                                timestamp = System.currentTimeMillis(),
                                fileDownload = chatResponse.fileDownload,  // Now FileDownload?
                                isError = false
                            )
                        )
                    } ?: Result.failure(Exception("Invalid or empty response body"))
                } else {
                    Log.e("ChatRepository", "API failure: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("API Error: ${response.code()} - ${response.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error sending message", e)
                Result.success(  // Use success with isError for UI feedback
                    ChatMessage(
                        content = "Error: ${e.message}",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                )  // Or failure if strict; adjust based on UX
            }
        }
    }

    suspend fun downloadFile(filename: String, context: Context): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<ResponseBody> = apiService.downloadFile(filename)

                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val file = saveFileToDownloads(responseBody, filename, context)
                        Log.d("ChatRepository", "File downloaded: ${file.absolutePath}")
                        Result.success(file)
                    } ?: Result.failure(Exception("Empty file response body"))
                } else {
                    Log.e("ChatRepository", "Download failed: ${response.code()} - ${response.errorBody()?.string()}")
                    Result.failure(Exception("Download failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Download error", e)
                Result.failure(e)
            }
        }
    }

    // Optional: Handle FileDownload.url directly if backend provides it
    suspend fun downloadFromUrl(fileDownload: FileDownload, context: Context): Result<File> {
        // Implement if needed: Use OkHttp to fetch from url, or fallback to filename from /download
        return downloadFile(fileDownload.filename, context)  // For now, use existing endpoint
    }

    private fun saveFileToDownloads(body: ResponseBody, filename: String, context: Context): File {
        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PCE_Documents")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, filename)
        val inputStream: InputStream = body.byteStream()
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return file
    }

    suspend fun checkHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<Map<String, String>> = apiService.healthCheck()
                val isHealthy = response.isSuccessful && response.code() == 200
                if (isHealthy) {
                    Log.d("ChatRepository", "Health check: OK - ${response.body()}")
                } else {
                    Log.w("ChatRepository", "Health check failed: ${response.code()}")
                }
                isHealthy
            } catch (e: Exception) {
                Log.e("ChatRepository", "Health check error", e)
                false
            }
        }
    }

    // Helper: Fetch from SessionManager for contextual queries
    private fun getCurrentYearFromSession(): String? {
        val sessionManager = SessionManager(context)
        val student = sessionManager.getStudent()
        return student?.erpNumber?.substring(0, 4)  // e.g., extract year from ERP like "2021PCE123"
    }
}
