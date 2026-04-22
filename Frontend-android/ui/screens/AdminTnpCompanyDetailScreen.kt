package com.pce.itassistant.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pce.itassistant.network.ApiService
import com.pce.itassistant.network.CompanyDetailResponse
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTnpCompanyDetailScreen(
    navController: NavController,
    companyId: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var companyDetail by remember { mutableStateOf<CompanyDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingNotes by remember { mutableStateOf(false) }
    var isUploadingPyq by remember { mutableStateOf(false) }
    var isUploadingMockTest by remember { mutableStateOf(false) }

    var notesTitle by remember { mutableStateOf("") }
    var pyqTitle by remember { mutableStateOf("") }
    var mockTitle by remember { mutableStateOf("") }
    var mockDescription by remember { mutableStateOf("") }
    var mockDuration by remember { mutableStateOf("") }

    var selectedNotesUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPyqUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMockTestUri by remember { mutableStateOf<Uri?>(null) }

    val notesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedNotesUri = uri
        if (uri != null) {
            Toast.makeText(context, "Notes file selected", Toast.LENGTH_SHORT).show()
        }
    }

    val pyqPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedPyqUri = uri
        if (uri != null) {
            Toast.makeText(context, "PYQ file selected", Toast.LENGTH_SHORT).show()
        }
    }

    val mockTestPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedMockTestUri = uri
        if (uri != null) {
            Toast.makeText(context, "Mock test file selected", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshCompanyDetail() {
        coroutineScope.launch {
            isLoading = true
            companyDetail = loadAdminCompanyDetail(context, companyId)
            isLoading = false
        }
    }

    LaunchedEffect(companyId) {
        refreshCompanyDetail()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Company Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && companyDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null
                            )
                            Text(
                                text = companyDetail?.company?.name ?: "Company #$companyId",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Company ID: $companyId")

                        companyDetail?.company?.campusDate?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Campus Date: $it")
                        }

                        companyDetail?.company?.eligibility?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Eligibility: $it")
                        }

                        companyDetail?.company?.news?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("News: $it")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Notes uploaded: ${companyDetail?.notes?.size ?: 0}")
                        Text("PYQs uploaded: ${companyDetail?.pyqs?.size ?: 0}")
                        Text("Mock tests uploaded: ${companyDetail?.mockTests?.size ?: 0}")
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notesTitle,
                            onValueChange = { notesTitle = it },
                            label = { Text("Notes Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = selectedNotesUri?.let { getFileName(context, it) }
                                ?: "No notes file selected"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { notesPickerLauncher.launch(arrayOf("*/*")) }
                        ) {
                            Text("Choose Notes File")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isUploadingNotes = true
                                    val success = uploadTnpResourceFromUri(
                                        context = context,
                                        companyId = companyId,
                                        resourceType = "notes",
                                        title = notesTitle,
                                        fileUri = selectedNotesUri
                                    )
                                    isUploadingNotes = false

                                    if (success) {
                                        notesTitle = ""
                                        selectedNotesUri = null
                                        refreshCompanyDetail()
                                    }
                                }
                            },
                            enabled = notesTitle.isNotBlank() &&
                                    selectedNotesUri != null &&
                                    !isUploadingNotes
                        ) {
                            if (isUploadingNotes) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("Upload Notes")
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload PYQ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pyqTitle,
                            onValueChange = { pyqTitle = it },
                            label = { Text("PYQ Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = selectedPyqUri?.let { getFileName(context, it) }
                                ?: "No PYQ file selected"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { pyqPickerLauncher.launch(arrayOf("*/*")) }
                        ) {
                            Text("Choose PYQ File")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isUploadingPyq = true
                                    val success = uploadTnpResourceFromUri(
                                        context = context,
                                        companyId = companyId,
                                        resourceType = "pyqs",
                                        title = pyqTitle,
                                        fileUri = selectedPyqUri
                                    )
                                    isUploadingPyq = false

                                    if (success) {
                                        pyqTitle = ""
                                        selectedPyqUri = null
                                        refreshCompanyDetail()
                                    }
                                }
                            },
                            enabled = pyqTitle.isNotBlank() &&
                                    selectedPyqUri != null &&
                                    !isUploadingPyq
                        ) {
                            if (isUploadingPyq) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("Upload PYQ")
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload Mock Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mockTitle,
                            onValueChange = { mockTitle = it },
                            label = { Text("Mock Test Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mockDescription,
                            onValueChange = { mockDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mockDuration,
                            onValueChange = { mockDuration = it },
                            label = { Text("Duration (minutes)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = selectedMockTestUri?.let { getFileName(context, it) }
                                ?: "No mock test file selected"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { mockTestPickerLauncher.launch(arrayOf("*/*")) }
                        ) {
                            Text("Choose Mock Test File")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val duration = mockDuration.toIntOrNull()
                                    if (duration == null) {
                                        Toast.makeText(
                                            context,
                                            "Enter valid duration in minutes",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }

                                    isUploadingMockTest = true
                                    val success = uploadMockTestFromUri(
                                        context = context,
                                        companyId = companyId,
                                        title = mockTitle,
                                        description = mockDescription,
                                        durationMinutes = duration,
                                        fileUri = selectedMockTestUri
                                    )
                                    isUploadingMockTest = false

                                    if (success) {
                                        mockTitle = ""
                                        mockDescription = ""
                                        mockDuration = ""
                                        selectedMockTestUri = null
                                        refreshCompanyDetail()
                                    }
                                }
                            },
                            enabled = mockTitle.isNotBlank() &&
                                    mockDescription.isNotBlank() &&
                                    mockDuration.isNotBlank() &&
                                    selectedMockTestUri != null &&
                                    !isUploadingMockTest
                        ) {
                            if (isUploadingMockTest) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("Upload Mock Test")
                            }
                        }
                    }
                }

                companyDetail?.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Uploaded Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            notes.forEach { note ->
                                Text("• ${note.title} (${note.originalFilename})")
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                companyDetail?.pyqs?.takeIf { it.isNotEmpty() }?.let { pyqs ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Uploaded PYQs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            pyqs.forEach { pyq ->
                                Text("• ${pyq.title} (${pyq.originalFilename})")
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                companyDetail?.mockTests?.takeIf { it.isNotEmpty() }?.let { mockTests ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Uploaded Mock Tests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            mockTests.forEach { mock ->
                                Text("• ${mock.title} - ${mock.totalQuestions} questions, ${mock.durationMinutes} mins")
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun loadAdminCompanyDetail(
    context: Context,
    companyId: Int
): CompanyDetailResponse? {
    return try {
        val response = ApiService.getInstance.getTnpCompanyDetail(companyId)
        if (response.isSuccessful) {
            response.body()
        } else {
            val errorText = response.errorBody()?.string() ?: "Failed to load company detail"
            Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            null
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}

suspend fun uploadTnpResourceFromUri(
    context: Context,
    companyId: Int,
    resourceType: String,
    title: String,
    fileUri: Uri?
): Boolean {
    if (fileUri == null) {
        Toast.makeText(context, "Please select a file", Toast.LENGTH_SHORT).show()
        return false
    }

    return try {
        val authHeader = SessionManager(context).getAuthHeader()
        if (authHeader == null) {
            Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
            return false
        }

        val file = createTempFileFromUri(context, fileUri)
        val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val multipartFile = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = ApiService.getInstance.uploadTnpResource(
            companyId = companyId,
            resourceType = resourceType,
            authHeader = authHeader,
            title = titleBody,
            file = multipartFile
        )

        if (response.isSuccessful) {
            Toast.makeText(context, "$resourceType uploaded successfully", Toast.LENGTH_SHORT).show()
            true
        } else {
            val errorText = response.errorBody()?.string() ?: "Upload failed"
            Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
            false
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        false
    }
}

suspend fun uploadMockTestFromUri(
    context: Context,
    companyId: Int,
    title: String,
    description: String,
    durationMinutes: Int,
    fileUri: Uri?
): Boolean {
    if (fileUri == null) {
        Toast.makeText(context, "Please select a file", Toast.LENGTH_SHORT).show()
        return false
    }

    return try {
        val authHeader = SessionManager(context).getAuthHeader()
        if (authHeader == null) {
            Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
            return false
        }

        val file = createTempFileFromUri(context, fileUri)
        val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val multipartFile = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val durationBody = durationMinutes.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val response = ApiService.getInstance.uploadMockTest(
            companyId = companyId,
            authHeader = authHeader,
            title = titleBody,
            description = descriptionBody,
            durationMinutes = durationBody,
            file = multipartFile
        )

        if (response.isSuccessful) {
            Toast.makeText(context, "Mock test uploaded successfully", Toast.LENGTH_SHORT).show()
            true
        } else {
            val errorText = response.errorBody()?.string() ?: "Mock test upload failed"
            Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
            false
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        false
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "selected_file"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) {
            name = it.getString(nameIndex)
        }
    }
    return name
}

fun createTempFileFromUri(context: Context, uri: Uri): File {
    val fileName = getFileName(context, uri)
    val tempFile = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return tempFile
}