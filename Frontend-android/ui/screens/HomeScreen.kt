package com.pce.itassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pce.itassistant.R
import com.pce.itassistant.data.Student
import com.pce.itassistant.utils.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Cache student info in mutableState to avoid re-fetching every recomposition
    var student by remember { mutableStateOf<Student?>(null) }
    var sessionChecked by remember { mutableStateOf(false) }
    var sessionExpiredShown by remember { mutableStateOf(false) }
    var showTrainingToast by remember { mutableStateOf(false) }

    // Load student once on enter
    LaunchedEffect(Unit) {
        student = sessionManager.getStudent()
        sessionChecked = true
    }

    // Trigger logout toast/navigation only once after auth check
    LaunchedEffect(student) {
        if (student == null && sessionChecked && !sessionExpiredShown) {
            Toast.makeText(context, "Session expired. Please login.", Toast.LENGTH_SHORT).show()
            sessionExpiredShown = true
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Show toast for training click, reset after shown
    LaunchedEffect(showTrainingToast) {
        if (showTrainingToast) {
            Toast.makeText(context, "T&P module under development", Toast.LENGTH_LONG).show()
            showTrainingToast = false
        }
    }

    if (!sessionChecked || student == null) {
        // Show loading/placeholder while checking
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val logoutButtonStr=R.string.logout_button
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    sessionManager.logout()
                    Toast.makeText(context, logoutButtonStr, Toast.LENGTH_SHORT).show()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text("Logout")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo & Welcome
            Image(
                painter = painterResource(id = R.drawable.ic_college_logo),
                contentDescription = stringResource(R.string.app_logo),
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_message),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Student Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            R.string.student_info,
                            student!!.erpNumber,
                            student!!.name ?: "N/A",
                            student!!.branch ?: "IT"
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.department_name)} - ${stringResource(R.string.university_name)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Services Title
            Text(
                text = stringResource(R.string.services_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val chatbotTitleStr= stringResource(id = R.string.chatbot_title)
            val chatbotDescStr= stringResource(id = R.string.chatbot_desc )
            val trainingPlacementStr=stringResource(id = R.string.training_placement)
            val trainingPlacementDescStr=stringResource(id =R.string.training_placement_desc)

            // Services Grid (2 columns: Fixed for exact 2; use your drawable IDs)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),  // Fixed: Expects GridCells, not Int
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(  // items() for list; no type param needed with Fixed columns
                    listOf(
                        ServiceItem(
                            icon = R.drawable.ic_chat,  // Your drawable ID (Int)
                            title = chatbotTitleStr,
                            desc = chatbotDescStr,
                            onClick = { navController.navigate("chatbot") }
                        ),
                        ServiceItem(
                            icon = R.drawable.ic_work,  // Your drawable ID (Int)
                            title = trainingPlacementStr,
                            desc = trainingPlacementDescStr,
                            onClick = { navController.navigate("student_tnp") }
                        )
                    )
                ) { service ->
                    ServiceCard(service)
                }
            }
        }
    }
}

data class ServiceItem(
    val icon: Int,  // Changed to Int for drawable resource IDs (handles vectors/PNG)
    val title: String,
    val desc: String,
    val onClick: () -> Unit
)

@Composable
private fun ServiceCard(item: ServiceItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Use Image for drawable resources (painterResource loads any type)
            Image(
                painter = painterResource(id = item.icon),
                contentDescription = item.title,  // Accessible desc
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
