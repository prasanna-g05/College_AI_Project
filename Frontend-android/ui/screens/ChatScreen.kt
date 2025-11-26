package com.pce.itassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pce.itassistant.R
import com.pce.itassistant.data.ChatMessage
import com.pce.itassistant.ui.chatbot.ChatbotViewModel
import com.pce.itassistant.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val student = remember { sessionManager.getStudent() }
    val viewModel: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.Factory(context)
    )
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Session check: Redirect if not logged in
    LaunchedEffect(student) {
        if (student == null) {
            Toast.makeText(context, "Please login to access chatbot.", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo("chatbot") { inclusive = true }
            }
        }
    }

    if (student == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val chatBotWelcome = stringResource(
        R.string.chatbot_welcome,
        student.name ?: "Student"
    )

    // Load welcome message on first compose (if messages empty)
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            val welcomeMessage = ChatMessage(
                content = chatBotWelcome,
                isUser = false
            )
            viewModel.addMessage(welcomeMessage)
        }
        listState.animateScrollToItem(index = messages.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chatbot_header)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages List (reversed: latest at bottom, emulating XML scroll)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),  // Tighter padding like XML
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(4.dp),  // Closer spacing
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(
                        message = message,
                        onFileDownload = {
                            viewModel.downloadFromMessage(message, context)
                        }
                    )
                }
            }

            if (isLoading) {
                // Loading as small bot bubble, like previous XML indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 16.dp, topStart = 8.dp, bottomStart = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Typing...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Input Row (styled like XML footer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(stringResource(R.string.type_message)) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),  // Rounded like XML
                    enabled = !isLoading,
                    maxLines = 4  // Allow multi-line input
                )
                Spacer(modifier = Modifier.width(8.dp))
                val emptyMessageStr = stringResource(R.string.empty_message)
                IconButton(
                    onClick = {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotEmpty() && !isLoading) {
                            // Fixed: Remove addMessage here to avoid duplicate; ViewModel handles it
                            viewModel.sendMessage(trimmed)  // Single param; no erpNumber
                            messageText = ""
                        } else if (trimmed.isEmpty()) {
                            Toast.makeText(context, emptyMessageStr, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = messageText.trim().isNotEmpty() && !isLoading
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = stringResource(R.string.send_message),
                        modifier = Modifier
                            .size(48.dp)  // Larger like XML
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onFileDownload: () -> Unit
) {
    val isUser = message.isUser
    val isError = message.isError == true  // Null-safe
    val hasFile = !isUser && message.fileDownload != null
    val bubbleColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timestamp = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if(isUser) 48.dp else 8.dp,end=if(isUser) 8.dp else 48.dp,top=2.dp,bottom=2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 4.dp else 20.dp,  // Asymmetric like XML bot/user
                bottomEnd = if (isUser) 20.dp else 4.dp
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)  // Subtle shadow
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Full text without truncation; scrolls in LazyColumn
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = Int.MAX_VALUE,  // Fixed: No limit for full RAG answers
                    overflow = TextOverflow.Visible  // Fixed: Show all content
                )
                if (hasFile) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier
                                .clickable { onFileDownload() }
                                .padding(end = 4.dp), // margin right of the download box
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download), // or Icons.Default.Download
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.fileDownload?.filename ?: "Download file",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Timestamp like XML (bottom, smaller font)
                Text(
                    text = timestamp,
                    color = textColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
