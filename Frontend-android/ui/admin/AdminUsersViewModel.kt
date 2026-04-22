package com.pce.itassistant.ui.admin

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pce.itassistant.network.AdminUserDto
import com.pce.itassistant.repository.AdminRepository
import kotlinx.coroutines.launch

data class AdminUser(
    val erpNumber: String,
    val name: String,
    val role: String,
)

class AdminUsersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)

    var users by mutableStateOf<List<AdminUser>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val togglingUsers = mutableSetOf<String>()

    fun isToggling(erpNumber: String): Boolean = togglingUsers.contains(erpNumber)

    fun clearError() {
        errorMessage = null
    }

    fun loadUsers() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = repository.getAllUsers()

            if (result.isSuccess) {
                users = result.getOrNull().orEmpty().map { it.toUiModel() }
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to load users"
            }

            isLoading = false
        }
    }

    fun toggleAdmin(erpNumber: String) {
        val currentUser = users.find { it.erpNumber == erpNumber } ?: return
        val newRole = if (currentUser.role.equals("admin", ignoreCase = true)) {
            "student"
        } else {
            "admin"
        }

        viewModelScope.launch {
            togglingUsers.add(erpNumber)
            errorMessage = null

            val result = repository.updateUserRole(erpNumber, newRole)

            if (result.isSuccess) {
                users = users.map { user ->
                    if (user.erpNumber == erpNumber) {
                        user.copy(role = newRole)
                    } else {
                        user
                    }
                }
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to update role"
            }

            togglingUsers.remove(erpNumber)
        }
    }

    private fun AdminUserDto.toUiModel(): AdminUser {
        return AdminUser(
            erpNumber = erpNumber,
            name = name,
            role = role
        )
    }
}