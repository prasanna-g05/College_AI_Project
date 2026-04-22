package com.pce.itassistant.network

import com.google.gson.annotations.SerializedName
import com.pce.itassistant.data.ChatRequest
import com.pce.itassistant.data.ChatResponse
import com.pce.itassistant.data.LoginRequest
import com.pce.itassistant.data.LoginResponse
import com.pce.itassistant.data.RegisterRequest
import com.pce.itassistant.data.RegisterResponse
import com.pce.itassistant.data.Student
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

data class StudentSheetResponse(
    val id: Int,
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("approved_count")
    val approvedCount: Int,
    @SerializedName("uploaded_at")
    val uploadedAt: String
)

data class AdminUserDto(
    @SerializedName("erp_number")
    val erpNumber: String,
    val name: String,
    val role: String,
    val branch: String? = null,
    val year: String? = null
)

data class UpdateRoleRequest(
    val role: String
)

data class UpdateRoleResponse(
    val message: String,
    val user: AdminUserDto
)

data class CompanyDto(
    val id: Int,
    val name: String,
    @SerializedName("folder_slug")
    val folderSlug: String? = null,
    @SerializedName("campus_date")
    val campusDate: String? = null,
    val eligibility: String? = null,
    val news: String? = null
)

data class CompanyCreateRequest(
    val name: String,
    @SerializedName("campus_date")
    val campusDate: String? = null,
    val eligibility: String? = null,
    val news: String? = null
)

data class TnpResourceDto(
    val id: Int,
    @SerializedName("company_id")
    val companyId: Int,
    @SerializedName("resource_type")
    val resourceType: String,
    val title: String,
    @SerializedName("original_filename")
    val originalFilename: String,
    @SerializedName("stored_filename")
    val storedFilename: String? = null,
    @SerializedName("file_path")
    val filePath: String? = null,
    @SerializedName("mime_type")
    val mimeType: String? = null,
    @SerializedName("uploaded_at")
    val uploadedAt: String? = null,
    @SerializedName("uploaded_by")
    val uploadedBy: String? = null,
    @SerializedName("download_url")
    val downloadUrl: String? = null
)

data class MockTestSummaryDto(
    val id: Int,
    @SerializedName("company_id")
    val companyId: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    @SerializedName("total_questions")
    val totalQuestions: Int,
    @SerializedName("total_marks")
    val totalMarks: Int,
    @SerializedName("uploaded_at")
    val uploadedAt: String? = null,
    @SerializedName("uploaded_by")
    val uploadedBy: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true
)

data class CompanyDetailResponse(
    val company: CompanyDto,
    val notes: List<TnpResourceDto>,
    val pyqs: List<TnpResourceDto>,
    @SerializedName("mock_tests")
    val mockTests: List<MockTestSummaryDto>
)

data class MockTestOptionDto(
    val id: Int,
    @SerializedName("option_text")
    val optionText: String
)

data class MockQuestionDto(
    val id: Int,
    @SerializedName("question_text")
    val questionText: String,
    @SerializedName("option_a")
    val optionA: String,
    @SerializedName("option_b")
    val optionB: String,
    @SerializedName("option_c")
    val optionC: String,
    @SerializedName("option_d")
    val optionD: String,
    @SerializedName("question_order")
    val questionOrder: Int,
    val marks: Int
)

data class MockTestDetailResponse(
    val id: Int,
    @SerializedName("company_id")
    val companyId: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    @SerializedName("total_questions")
    val totalQuestions: Int,
    @SerializedName("total_marks")
    val totalMarks: Int,
    val questions: List<MockQuestionDto> = emptyList()
)

data class MockTestAttemptAnswerRequest(
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("selected_option")
    val selectedOption: String
)

data class MockTestAttemptRequest(
    val answers: List<MockTestAttemptAnswerRequest>
)

data class MockTestAttemptResponse(
    @SerializedName("attempt_id")
    val attemptId: Int
)

data class MockTestAttemptResultResponse(
    @SerializedName("attempt_id")
    val attemptId: Int,
    @SerializedName("mock_test_id")
    val mockTestId: Int,
    @SerializedName("test_title")
    val testTitle: String,
    val score: Int,
    @SerializedName("total_marks")
    val totalMarks: Int,
    val percentage: Double,
    @SerializedName("correct_answers")
    val correctAnswers: Int,
    @SerializedName("wrong_answers")
    val wrongAnswers: Int,
    @SerializedName("attempted_questions")
    val attemptedQuestions: Int,
    @SerializedName("total_questions")
    val totalQuestions: Int,
    val message: String? = null
)

data class MockTestSubmitResponse(
    @SerializedName("mock_test_id")
    val mockTestId: Int,
    @SerializedName("score")
    val score: Int,
    @SerializedName("total_marks")
    val totalMarks: Int,
    @SerializedName("correct_answers")
    val correctAnswers: Int,
    @SerializedName("wrong_answers")
    val wrongAnswers: Int,
    @SerializedName("attempted_questions")
    val attemptedQuestions: Int,
    @SerializedName("total_questions")
    val totalQuestions: Int
)

data class MessageResponse(
    val message: String
)

data class ChatbotFileDto(
    val id: Int,
    @SerializedName("original_filename")
    val originalFilename: String,
    @SerializedName("stored_filename")
    val storedFilename: String? = null,
    @SerializedName("file_path")
    val filePath: String? = null,
    @SerializedName("mime_type")
    val mimeType: String? = null,
    @SerializedName("uploaded_at")
    val uploadedAt: String? = null,
    @SerializedName("uploaded_by")
    val uploadedBy: String? = null,
    @SerializedName("is_processed")
    val isProcessed: Boolean = false
)

data class ChatbotFileUploadResponse(
    val message: String,
    val file: ChatbotFileDto? = null
)

interface ApiService {

    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authHeader: String
    ): Response<Student>

    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>

    @Streaming
    @GET("download/{filename}")
    suspend fun downloadFile(
        @Path("filename") filename: String
    ): Response<ResponseBody>

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>

    @GET("stats")
    suspend fun getStats(): Response<Map<String, Any>>

    @GET("admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") authHeader: String
    ): Response<List<AdminUserDto>>

    @PUT("admin/users/{erp_number}/role")
    suspend fun updateUserRole(
        @Path("erp_number") erpNumber: String,
        @Header("Authorization") authHeader: String,
        @Body request: UpdateRoleRequest
    ): Response<UpdateRoleResponse>

    @Multipart
    @POST("admin/student-sheets/upload")
    suspend fun uploadStudentSheet(
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @GET("admin/student-sheets")
    suspend fun getStudentSheets(
        @Header("Authorization") authHeader: String
    ): Response<List<StudentSheetResponse>>

    @GET("admin/approved-students")
    suspend fun getApprovedStudents(
        @Header("Authorization") authHeader: String
    ): Response<List<Student>>

    @POST("admin/companies")
    suspend fun createCompany(
        @Header("Authorization") authHeader: String,
        @Body request: CompanyCreateRequest
    ): Response<CompanyDto>

    @GET("admin/companies")
    suspend fun getAdminCompanies(
        @Header("Authorization") authHeader: String
    ): Response<List<CompanyDto>>

    @GET("tnp/companies")
    suspend fun getTnpCompanies(): Response<List<CompanyDto>>

    @GET("tnp/companies/{company_id}")
    suspend fun getTnpCompanyDetail(
        @Path("company_id") companyId: Int
    ): Response<CompanyDetailResponse>

    @GET("tnp/mock-tests/{mock_test_id}")
    suspend fun getMockTestDetail(
        @Path("mock_test_id") mockTestId: Int
    ): Response<MockTestDetailResponse>

    @POST("tnp/mock-tests/{mock_test_id}/submit")
    suspend fun submitMockTest(
        @Path("mock_test_id") mockTestId: Int,
        @Body request: MockTestAttemptRequest
    ): Response<MockTestSubmitResponse>

    @GET("tnp/mock-tests/attempts/{attempt_id}")
    suspend fun getMockTestResult(
        @Path("attempt_id") attemptId: Int
    ): Response<MockTestAttemptResultResponse>

    @DELETE("admin/companies/{company_id}")
    suspend fun deleteCompany(
        @Path("company_id") companyId: Int,
        @Header("Authorization") authHeader: String
    ): Response<MessageResponse>

    @Multipart
    @POST("admin/companies/{company_id}/resources/{resource_type}")
    suspend fun uploadTnpResource(
        @Path("company_id") companyId: Int,
        @Path("resource_type") resourceType: String,
        @Header("Authorization") authHeader: String,
        @Part("title") title: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<TnpResourceDto>

    @Multipart
    @POST("admin/companies/{company_id}/mock-tests/upload")
    suspend fun uploadMockTest(
        @Path("company_id") companyId: Int,
        @Header("Authorization") authHeader: String,
        @Part("title") title: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part("duration_minutes") durationMinutes: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<MockTestSummaryDto>

    @Multipart
    @POST("admin/chatbot-files/upload")
    suspend fun uploadChatbotFile(
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part
    ): Response<ChatbotFileUploadResponse>

    @GET("admin/chatbot-files")
    suspend fun getChatbotFiles(
        @Header("Authorization") authHeader: String
    ): Response<List<ChatbotFileDto>>

    @DELETE("admin/chatbot-files/{file_id}")
    suspend fun deleteChatbotFile(
        @Path("file_id") fileId: Int,
        @Header("Authorization") authHeader: String
    ): Response<MessageResponse>

    companion object {
        const val BASE_URL = "http://10.0.2.2:8000/"

        val getInstance: ApiService by lazy {
            RetrofitClient.retrofit.create(ApiService::class.java)
        }
    }
}
