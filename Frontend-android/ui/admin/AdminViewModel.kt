package com.pce.itassistant.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminStats(
    val totalStudents: Int = 0,
    val totalAdmins: Int = 0,
    val totalCompanies: Int = 0
)

class AdminViewModel : ViewModel() {
    private val _stats = MutableStateFlow(AdminStats())
    val stats: StateFlow<AdminStats> = _stats.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            // TODO: Call admin repository
            // _stats.value = adminRepo.getStats()
        }
    }
}
