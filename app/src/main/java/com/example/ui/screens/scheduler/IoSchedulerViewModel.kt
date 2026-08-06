package com.example.ui.screens.scheduler

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.IoSchedulerInfo
import com.example.data.repository.KernelRepository
import com.example.root.SysfsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IoSchedulerUiState(
    val isLoading: Boolean = true,
    val schedulerInfo: IoSchedulerInfo = IoSchedulerInfo(),
    val selectedScheduler: String = "",
    val isApplying: Boolean = false,
    val userMessage: String? = null
)

class IoSchedulerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IoSchedulerUiState())
    val uiState: StateFlow<IoSchedulerUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())

    init {
        loadSchedulerData()
    }

    fun loadSchedulerData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val info = SysfsHelper.getIoSchedulerInfo()
                _uiState.value = IoSchedulerUiState(
                    isLoading = false,
                    schedulerInfo = info,
                    selectedScheduler = info.currentScheduler
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "Failed to detect I/O schedulers"
                )
            }
        }
    }

    fun onSchedulerSelected(sched: String) {
        _uiState.value = _uiState.value.copy(selectedScheduler = sched)
    }

    fun applyScheduler() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            val sched = _uiState.value.selectedScheduler
            if (sched.isNotBlank()) {
                val res = repository.applyIoScheduler(sched)
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    userMessage = if (res.isSuccess) "I/O Scheduler set to $sched" else "Failed: ${res.outputText}"
                )
            }
            loadSchedulerData()
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
