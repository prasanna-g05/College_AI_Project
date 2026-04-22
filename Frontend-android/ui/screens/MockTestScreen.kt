package com.pce.itassistant.ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pce.itassistant.network.ApiService
import com.pce.itassistant.network.MockQuestionDto
import com.pce.itassistant.network.MockTestAttemptAnswerRequest
import com.pce.itassistant.network.MockTestAttemptRequest
import com.pce.itassistant.network.MockTestDetailResponse
import com.pce.itassistant.network.MockTestSubmitResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestScreen(
    navController: NavController,
    mockTestId: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mockTest by remember { mutableStateOf<MockTestDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(mockTestId) {
        isLoading = true
        mockTest = loadMockTestDetail(context, mockTestId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Test") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isSubmitting
                    ) {
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

            mockTest == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load mock test",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            else -> {
                val detail = mockTest!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MockTestHeaderCard(detail)

                        detail.questions.forEachIndexed { index, question ->
                            QuestionCard(
                                questionNumber = index + 1,
                                question = question,
                                selectedOptionId = selectedAnswers[question.id],
                                onOptionSelected = { optionId ->
                                    selectedAnswers[question.id] = optionId
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Answered ${selectedAnswers.size} of ${detail.questions.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = submitMockTest(
                                            context = context,
                                            mockTestId = mockTestId,
                                            selectedAnswers = selectedAnswers,
                                            onSubmitting = { isSubmitting = it }
                                        )

                                        if (result != null) {
                                            navController.navigate(
                                                "mock_test_result/" +
                                                        "${result.mockTestId}/" +
                                                        "${result.score}/" +
                                                        "${result.totalMarks}/" +
                                                        "${result.correctAnswers}/" +
                                                        "${result.wrongAnswers}/" +
                                                        "${result.attemptedQuestions}/" +
                                                        "${result.totalQuestions}"
                                            )
                                        }
                                    }
                                },
                                enabled = !isSubmitting && detail.questions.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isSubmitting) "Submitting..." else "Submit Test"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MockTestHeaderCard(detail: MockTestDetailResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  ${detail.durationMinutes} min • ${detail.totalQuestions} questions • ${detail.totalMarks} marks",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (!detail.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = detail.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun QuestionCard(
    questionNumber: Int,
    question: MockQuestionDto,
    selectedOptionId: Int?,
    onOptionSelected: (Int) -> Unit
) {
    val options = listOf(
        1 to question.optionA,
        2 to question.optionB,
        3 to question.optionC,
        4 to question.optionD
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Question $questionNumber",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (id, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedOptionId == id,
                                onClick = { onOptionSelected(id) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOptionId == id,
                            onClick = null
                        )

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

suspend fun loadMockTestDetail(
    context: Context,
    mockTestId: Int
): MockTestDetailResponse? {
    return try {
        val response = ApiService.getInstance.getMockTestDetail(mockTestId)
        if (response.isSuccessful) {
            response.body()
        } else {
            val errorText = response.errorBody()?.string() ?: "Failed to load mock test"
            Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            null
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}

suspend fun submitMockTest(
    context: Context,
    mockTestId: Int,
    selectedAnswers: Map<Int, Int>,
    onSubmitting: (Boolean) -> Unit
): MockTestSubmitResponse? {
    return try {
        onSubmitting(true)

        val request = MockTestAttemptRequest(
            answers = selectedAnswers.map { (questionId, selectedOptionId) ->
                MockTestAttemptAnswerRequest(
                    questionId = questionId,
                    selectedOption = when (selectedOptionId) {
                        1 -> "A"
                        2 -> "B"
                        3 -> "C"
                        4 -> "D"
                        else -> ""
                    }
                )
            }
        )

        val response = ApiService.getInstance.submitMockTest(mockTestId, request)

        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            android.util.Log.d("MockSubmit", "response = $body")
            body
        } else {
            val errorText = response.errorBody()?.string() ?: "Failed to submit mock test"
            Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
            null
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    } finally {
        onSubmitting(false)
    }
}