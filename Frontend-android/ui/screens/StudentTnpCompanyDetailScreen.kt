package com.pce.itassistant.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pce.itassistant.network.ApiService
import com.pce.itassistant.network.CompanyDetailResponse
import com.pce.itassistant.network.MockTestSummaryDto
import com.pce.itassistant.network.TnpResourceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTnpCompanyDetailScreen(
    navController: NavController,
    companyId: Int
) {
    val context = LocalContext.current
    var companyDetail by remember { mutableStateOf<CompanyDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(companyId) {
        isLoading = true
        companyDetail = loadStudentCompanyDetail(context, companyId)
        isLoading = false
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            companyDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load company details",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            else -> {
                val detail = companyDetail!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "company_header") {
                        CompanyHeaderCard(detail)
                    }

                    item(key = "notes_header") {
                        SectionHeader(
                            title = "Notes",
                            icon = Icons.Default.Description
                        )
                    }

                    if (detail.notes.isEmpty()) {
                        item(key = "notes_empty") {
                            EmptySectionCard("No notes available yet")
                        }
                    } else {
                        items(
                            items = detail.notes,
                            key = { note -> "note_${note.id}" }
                        ) { note ->
                            ResourceCard(
                                resource = note,
                                onOpen = { openResource(context, note) }
                            )
                        }
                    }

                    item(key = "pyqs_header") {
                        SectionHeader(
                            title = "PYQs",
                            icon = Icons.Default.Assignment
                        )
                    }

                    if (detail.pyqs.isEmpty()) {
                        item(key = "pyqs_empty") {
                            EmptySectionCard("No PYQs available yet")
                        }
                    } else {
                        items(
                            items = detail.pyqs,
                            key = { pyq -> "pyq_${pyq.id}" }
                        ) { pyq ->
                            ResourceCard(
                                resource = pyq,
                                onOpen = { openResource(context, pyq) }
                            )
                        }
                    }

                    item(key = "mock_tests_header") {
                        SectionHeader(
                            title = "Mock Tests",
                            icon = Icons.Default.Quiz
                        )
                    }

                    if (detail.mockTests.isEmpty()) {
                        item(key = "mock_tests_empty") {
                            EmptySectionCard("No mock tests available yet")
                        }
                    } else {
                        items(
                            items = detail.mockTests,
                            key = { mockTest -> "mock_${mockTest.id}" }
                        ) { mockTest ->
                            MockTestCard(
                                mockTest = mockTest,
                                onClick = {
                                    navController.navigate("mock_test/${mockTest.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyHeaderCard(detail: CompanyDetailResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = detail.company.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!detail.company.campusDate.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "📅 Campus Date: ${detail.company.campusDate}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!detail.company.eligibility.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📋 Eligibility: ${detail.company.eligibility}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (!detail.company.news.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detail.company.news.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptySectionCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceCard(
    resource: TnpResourceDto,
    onOpen: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = resource.originalFilename ?: "Unnamed file",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!resource.mimeType.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: ${resource.mimeType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open resource",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestCard(
    mockTest: MockTestSummaryDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = mockTest.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${mockTest.totalQuestions} questions • ${mockTest.totalMarks} marks • ${mockTest.durationMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open mock test",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun openResource(context: Context, resource: TnpResourceDto) {
    val url = resource.downloadUrl?.takeIf { it.isNotBlank() }

    if (url == null) {
        Toast.makeText(context, "File link not available", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), resource.mimeType ?: "application/pdf")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open file", Toast.LENGTH_SHORT).show()
    }
}

suspend fun loadStudentCompanyDetail(
    context: Context,
    companyId: Int
): CompanyDetailResponse? {
    return try {
        val response = ApiService.getInstance.getTnpCompanyDetail(companyId)
        if (response.isSuccessful) {
            response.body()
        } else {
            val errorText = response.errorBody()?.string() ?: "Failed to load company details"
            Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            null
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}