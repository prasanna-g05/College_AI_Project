package com.pce.itassistant.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
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
import com.pce.itassistant.network.CompanyCreateRequest
import com.pce.itassistant.network.CompanyDto
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTnpScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var companies by remember { mutableStateOf(listOf<CompanyDto>()) }
    var isLoading by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var campusDate by remember { mutableStateOf("") }
    var eligibility by remember { mutableStateOf("") }
    var news by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        companies = loadAdminCompanies(context)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("T&P Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Add Company",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Company Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = campusDate,
                        onValueChange = { campusDate = it },
                        label = { Text("Campus Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = eligibility,
                        onValueChange = { eligibility = it },
                        label = { Text("Eligibility") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = news,
                        onValueChange = { news = it },
                        label = { Text("News/Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val success = addCompanyToBackend(
                                    context = context,
                                    name = name,
                                    campusDate = campusDate.ifBlank { null },
                                    eligibility = eligibility.ifBlank { null },
                                    news = news.ifBlank { null }
                                )

                                if (success) {
                                    companies = loadAdminCompanies(context)
                                    name = ""
                                    campusDate = ""
                                    eligibility = ""
                                    news = ""
                                    Toast.makeText(context, "Company added!", Toast.LENGTH_SHORT).show()
                                }

                                isLoading = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = name.isNotBlank() && !isLoading
                    ) {
                        Text("Add Company")
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                companies.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No companies yet")
                        }
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(companies) { company ->
                            CompanyCard(
                                company = company,
                                onClick = {
                                    navController.navigate("admin_tnp_detail/${company.id}")
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        val success = deleteCompanyFromBackend(context, company.id)
                                        if (success) {
                                            companies = loadAdminCompanies(context)
                                            Toast.makeText(context, "Company deleted", Toast.LENGTH_SHORT).show()
                                        }
                                        isLoading = false
                                    }
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
fun CompanyCard(
    company: CompanyDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!company.campusDate.isNullOrEmpty()) {
                    Text("📅 ${company.campusDate}")
                }

                if (!company.eligibility.isNullOrEmpty()) {
                    Text("📋 ${company.eligibility}")
                }

                if (!company.news.isNullOrEmpty()) {
                    Text(company.news)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun getAuthHeader(context: Context): String? {
    return SessionManager(context).getAuthHeader()
}

suspend fun loadAdminCompanies(context: Context): List<CompanyDto> {
    return try {
        val authHeader = getAuthHeader(context)
        if (authHeader == null) {
            Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
            emptyList()
        } else {
            val response = ApiService.getInstance.getAdminCompanies(authHeader)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                val errorText = response.errorBody()?.string() ?: "Failed to load companies"
                Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                emptyList()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        emptyList()
    }
}

suspend fun addCompanyToBackend(
    context: Context,
    name: String,
    campusDate: String?,
    eligibility: String?,
    news: String?
): Boolean {
    return try {
        val authHeader = getAuthHeader(context)
        if (authHeader == null) {
            Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
            false
        } else {
            val response = ApiService.getInstance.createCompany(
                authHeader = authHeader,
                request = CompanyCreateRequest(
                    name = name,
                    campusDate = campusDate,
                    eligibility = eligibility,
                    news = news
                )
            )

            if (response.isSuccessful) {
                true
            } else {
                val errorText = response.errorBody()?.string() ?: "Failed to add company"
                Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                false
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}

suspend fun deleteCompanyFromBackend(context: Context, id: Int): Boolean {
    return try {
        val authHeader = getAuthHeader(context)
        if (authHeader == null) {
            Toast.makeText(context, "Please login again", Toast.LENGTH_SHORT).show()
            false
        } else {
            val response = ApiService.getInstance.deleteCompany(
                companyId = id,
                authHeader = authHeader
            )

            if (response.isSuccessful) {
                true
            } else {
                val errorText = response.errorBody()?.string() ?: "Failed to delete company"
                Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                false
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}