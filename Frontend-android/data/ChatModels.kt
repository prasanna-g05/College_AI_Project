package com.pce.itassistant.data

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("query")
    val query: String,

    @SerializedName("erp_number")
    val erpNumber: String? = null,

    @SerializedName("year")
    val year: String? = null,

    @SerializedName("semester")
    val semester: String? = null,

    @SerializedName("course")
    val course: String? = null
)

data class ChatResponse(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("context")
    val context: Map<String, Any>? = null,

    @SerializedName("answer")
    val answer: String? = null,

    @SerializedName("docs")
    val docs: List<String>? = null,

    @SerializedName("file_download")
    val fileDownload: FileDownload? = null,

    @SerializedName("matching_files")
    val matchingFiles: List<MatchingFile>? = null
)

data class FileDownload(
    @SerializedName("filename")
    val filename: String? = null,

    @SerializedName("url")
    val url: String? = null
)

data class MatchingFile(
    @SerializedName("filename")
    val filename: String? = null,

    @SerializedName("download_url")
    val downloadUrl: String? = null,

    // Fallback for string-only backend
    @SerializedName("value")
    val value: String? = null
)

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val fileDownload: FileDownload? = null,
    val matchingFiles: List<MatchingFile> = emptyList()
)
