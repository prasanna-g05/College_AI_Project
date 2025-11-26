package com.pce.itassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.pce.itassistant.R
import com.pce.itassistant.ui.login.LoginViewModel
import com.pce.itassistant.utils.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val viewModel: LoginViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val loginState by viewModel.loginState.collectAsState()
    val apiHealth by viewModel.apiHealth.collectAsState()
    val loginSuccessEvent by viewModel.loginSuccessEvent

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkApiHealth()
    }

    val loginSuccessStr = stringResource(R.string.login_success)
    LaunchedEffect(loginSuccessEvent) {
        if (loginSuccessEvent) {
            (viewModel.loginState.value as? LoginViewModel.LoginState.Success)?.student?.let { student ->
                sessionManager.saveLoginSession(student)
            }
            Toast.makeText(context, loginSuccessStr, Toast.LENGTH_SHORT).show()
            viewModel.loginSuccessEventConsumed()
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_college_logo),
            contentDescription = stringResource(R.string.college_name),
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        val healthColor = when (apiHealth) {
            "healthy" -> Color.Green
            "unhealthy" -> Color.Red
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = healthColor
                )
                Column {
                    Text(
                        text = stringResource(R.string.api_status),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (apiHealth) {
                            "healthy" -> stringResource(R.string.api_healthy)
                            "unhealthy" -> stringResource(R.string.api_unhealthy)
                            else -> stringResource(R.string.api_checking)
                        },
                        color = healthColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var erpText by remember { mutableStateOf("") }
        var passwordText by remember { mutableStateOf("") }

        OutlinedTextField(
            value = erpText,
            onValueChange = { erpText = it },
            label = { Text(stringResource(R.string.erp_label)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = erpText.isNotEmpty() && !viewModel.isValidErpNumber(erpText),
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginViewModel.LoginState.Loading
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text(stringResource(R.string.password_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginViewModel.LoginState.Loading
        )

        val errorMessage = when (val state = loginState) {
            is LoginViewModel.LoginState.Error -> state.message
            else -> null
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
            )

            // Auto redirect on "no password" message from backend
            if (it.contains("no password", ignoreCase = true)) {
                LaunchedEffect(Unit) {
                    navController.navigate("register")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val invalidErpMessage = stringResource(R.string.invalid_erp)
        val invalidPasswordMessage = stringResource(R.string.invalid_password)
        val loggingInMessage = stringResource(R.string.logging_in)
        val loginButtonText = stringResource(R.string.login_button)

        Button(
            onClick = {
                if (!viewModel.isValidErpNumber(erpText)) {
                    Toast.makeText(context, invalidErpMessage, Toast.LENGTH_LONG).show()
                } else if (passwordText.isBlank()) {
                    Toast.makeText(context, invalidPasswordMessage, Toast.LENGTH_LONG).show()
                } else {
                    coroutineScope.launch {
                        viewModel.loginStudent(erpText, passwordText)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.isValidErpNumber(erpText)
                    && passwordText.isNotBlank()
                    && loginState !is LoginViewModel.LoginState.Loading
        ) {
            when (loginState) {
                is LoginViewModel.LoginState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(loggingInMessage)
                }
                else -> Text(loginButtonText)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Registration redirect text
        Text(
            text = stringResource(R.string.register_prompt),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(8.dp)
                .clickable { navController.navigate("register") }
        )

        if (loginState is LoginViewModel.LoginState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}
