package com.pce.itassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestResultScreen(
    navController: NavController,
    mockTestId: Int,
    score: Int,
    totalMarks: Int,
    correctAnswers: Int,
    wrongAnswers: Int,
    attemptedQuestions: Int,
    totalQuestions: Int
) {
    val percentage = remember(score, totalMarks) {
        if (totalMarks > 0) {
            (score.toDouble() / totalMarks.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    val progress = remember(score, totalMarks) {
        if (totalMarks > 0) {
            score.toFloat() / totalMarks.toFloat()
        } else {
            0f
        }
    }

    val message = remember(score, totalMarks) {
        when {
            totalMarks == 0 -> "Result submitted successfully."
            percentage >= 75.0 -> "Excellent performance."
            percentage >= 50.0 -> "Good job. Keep practicing to improve further."
            else -> "Keep practicing. You can do better in the next attempt."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Test Result") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultHeroCard(
                title = "Mock Test #$mockTestId",
                score = score,
                totalMarks = totalMarks,
                percentage = percentage,
                progress = progress
            )

            ResultStatRow(
                correctAnswers = correctAnswers,
                wrongAnswers = wrongAnswers,
                attemptedQuestions = attemptedQuestions,
                totalQuestions = totalQuestions
            )

            InfoCard(message = message)

            Button(
                onClick = {
                    navController.navigate("student_tnp") {
                        popUpTo("student_tnp") { inclusive = false }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to T&P Updates")
            }
        }
    }
}

@Composable
fun ResultHeroCard(
    title: String,
    score: Int,
    totalMarks: Int,
    percentage: Double,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$score / $totalMarks",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${"%.2f".format(percentage)}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ResultStatRow(
    correctAnswers: Int,
    wrongAnswers: Int,
    attemptedQuestions: Int,
    totalQuestions: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultMiniCard(
                modifier = Modifier.weight(1f),
                title = "Correct",
                value = correctAnswers.toString(),
                icon = Icons.Default.CheckCircle
            )

            ResultMiniCard(
                modifier = Modifier.weight(1f),
                title = "Wrong",
                value = wrongAnswers.toString(),
                icon = Icons.Default.HighlightOff
            )
        }

        ResultMiniCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Attempted",
            value = "$attemptedQuestions / $totalQuestions",
            icon = Icons.Default.TaskAlt
        )
    }
}

@Composable
fun ResultMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = message,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}