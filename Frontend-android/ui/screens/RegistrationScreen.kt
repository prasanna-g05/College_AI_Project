package com.pce.itassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pce.itassistant.data.RegisterRequest
import com.pce.itassistant.ui.login.LoginViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    navController: NavHostController,
    preFilledStudent: com.pce.itassistant.data.Student? = null,
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var erpNumber by remember { mutableStateOf(preFilledStudent?.erpNumber ?: "") }
    var rollNumber by remember { mutableStateOf(preFilledStudent?.rollNumber ?: "") }
    var name by remember { mutableStateOf(preFilledStudent?.name ?: "") }
    var year by remember { mutableStateOf(preFilledStudent?.year ?: "") }
    var semester by remember { mutableStateOf(preFilledStudent?.semester ?: "") }
    var branch by remember { mutableStateOf(preFilledStudent?.branch ?: "") }
    var section by remember { mutableStateOf(preFilledStudent?.section ?: "") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var registrationState by remember { mutableStateOf<LoginViewModel.LoginState>(LoginViewModel.LoginState.Idle) }

    fun isPasswordValid() = password.isNotBlank()
    fun doPasswordsMatch() = password == confirmPassword
    fun areFieldsValid() =
        erpNumber.isNotBlank() && rollNumber.isNotBlank() && name.isNotBlank() && year.isNotBlank() &&
                semester.isNotBlank() && branch.isNotBlank() && section.isNotBlank() &&
                isPasswordValid() && doPasswordsMatch()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Student Registration") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(value = erpNumber, onValueChange = { erpNumber = it },
                label = { Text("ERP Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = rollNumber, onValueChange = { rollNumber = it },
                label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = year, onValueChange = { year = it },
                label = { Text("Year") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = semester, onValueChange = { semester = it },
                label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = branch, onValueChange = { branch = it },
                label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = section, onValueChange = { section = it },
                label = { Text("Section") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isPasswordValid()) {
                Text("Password cannot be empty", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            } else if (!doPasswordsMatch()) {
                Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        registrationState = LoginViewModel.LoginState.Loading
                        val request = RegisterRequest(
                            erpNumber = erpNumber,
                            name = name,
                            year = year,
                            semester = semester,
                            password = password,
                            branch = branch,
                            section = section,
                            rollNumber = rollNumber
                        )
                        val result = viewModel.registerUser(request)
                        registrationState = if (result.isSuccess) {
                            Toast.makeText(context, "Registration Successful", Toast.LENGTH_LONG).show()
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                            LoginViewModel.LoginState.Success(result.getOrNull()!!)
                        } else {
                            LoginViewModel.LoginState.Error(result.exceptionOrNull()?.message ?: "Registration Failed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = areFieldsValid() && registrationState !is LoginViewModel.LoginState.Loading
            ) {
                if (registrationState is LoginViewModel.LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registering...")
                } else {
                    Text("Register")
                }
            }

            if (registrationState is LoginViewModel.LoginState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (registrationState as LoginViewModel.LoginState.Error).message ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
