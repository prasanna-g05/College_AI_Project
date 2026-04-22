package com.pce.itassistant.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pce.itassistant.data.AuthSession
import com.pce.itassistant.data.RegisterRequest
import com.pce.itassistant.data.Student
import com.pce.itassistant.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class LoginViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _apiHealth = MutableStateFlow("checking")
    val apiHealth: StateFlow<String> = _apiHealth.asStateFlow()

    var loginSuccessEvent = mutableStateOf(false)
        private set

    private val _navigationEvent = MutableStateFlow<String?>(null)
    val navigationEvent: StateFlow<String?> = _navigationEvent.asStateFlow()

    private val _registrationState = MutableStateFlow<LoginState>(LoginState.Idle)
    val registrationState: StateFlow<LoginState> = _registrationState.asStateFlow()

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val authSession: AuthSession) : LoginState()
        data class RegistrationSuccess(val student: Student) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    fun isValidErpNumber(erp: String): Boolean {
        return Pattern.matches("^\\d{9}$", erp)
    }

    fun isValidPassword(password: String): Boolean {
        return password.isNotBlank()
    }

    fun loginSuccessEventConsumed() {
        loginSuccessEvent.value = false
    }

    fun navigationEventConsumed() {
        _navigationEvent.value = null
    }

    fun loginStudent(erpNumber: String, password: String) {
        if (!isValidErpNumber(erpNumber)) {
            _loginState.value = LoginState.Error("ERP must be exactly 9 digits")
            return
        }

        if (!isValidPassword(password)) {
            _loginState.value = LoginState.Error("Password cannot be empty")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = authRepository.loginUser(erpNumber, password)

            if (result.isSuccess) {
                val authSession = result.getOrNull()!!
                val student = authSession.student

                _loginState.value = LoginState.Success(authSession)
                loginSuccessEvent.value = true

                _navigationEvent.value = when (student.role.lowercase()) {
                    "admin" -> "admin_screen"
                    "student" -> "home"
                    else -> "home"
                }
            } else {
                val backendError = result.exceptionOrNull()?.message ?: "Login failed"
                val userMessage = when {
                    backendError.contains("Invalid ERP number", ignoreCase = true) -> "Please enter a valid ERP number"
                    backendError.contains("Invalid password", ignoreCase = true) -> "Invalid password"
                    backendError.contains("inactive", ignoreCase = true) -> "User account is inactive."
                    else -> "Login failed. Please try again."
                }
                _loginState.value = LoginState.Error(userMessage)
            }
        }
    }

    suspend fun registerUser(request: RegisterRequest): Result<Student> {
        _registrationState.value = LoginState.Loading
        return try {
            val result = authRepository.registerUser(request)
            if (result.isSuccess) {
                _registrationState.value = LoginState.RegistrationSuccess(result.getOrNull()!!)
            } else {
                _registrationState.value = LoginState.Error(
                    result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
            result
        } catch (e: Exception) {
            _registrationState.value = LoginState.Error(e.message ?: "Registration failed")
            Result.failure(e)
        }
    }

    suspend fun checkApiHealth() {
        _apiHealth.value = "checking"
        try {
            val isHealthy = authRepository.checkApiHealth()
            _apiHealth.value = if (isHealthy) "healthy" else "unhealthy"
        } catch (e: Exception) {
            _apiHealth.value = "unhealthy"
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}
