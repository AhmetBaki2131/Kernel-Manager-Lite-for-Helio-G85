package com.example.ui.screens.cpu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CpuCoreInfo
import com.example.data.model.CpuInfo
import com.example.data.repository.KernelRepository
import com.example.root.SysfsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CpuUiState(
    val isLoading: Boolean = true,
    val cpuInfo: CpuInfo = CpuInfo(),
    val selectedGovernor: String = "",
    val selectedMinFreqKHz: Long = 0L,
    val selectedMaxFreqKHz: Long = 0L,
    val isApplying: Boolean = false,
    val userMessage: String? = null
)

class CpuViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CpuUiState())
    val uiState: StateFlow<CpuUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())

    init {
        loadCpuData()
    }

    fun loadCpuData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val cpu = SysfsHelper.getCpuInfo()
                _uiState.value = CpuUiState(
                    isLoading = false,
                    cpuInfo = cpu,
                    selectedGovernor = cpu.currentGovernor,
                    selectedMinFreqKHz = cpu.globalMinFreqKHz,
                    selectedMaxFreqKHz = cpu.globalMaxFreqKHz
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "Failed to load CPU configuration"
                )
            }
        }
    }

    fun onGovernorSelected(governor: String) {
        _uiState.value = _uiState.value.copy(selectedGovernor = governor)
    }

    fun onMinFreqSelected(freqKHz: Long) {
        _uiState.value = _uiState.value.copy(selectedMinFreqKHz = freqKHz)
    }

    fun onMaxFreqSelected(freqKHz: Long) {
        _uiState.value = _uiState.value.copy(selectedMaxFreqKHz = freqKHz)
    }

    fun toggleCoreState(coreId: Int, online: Boolean) {
        viewModelScope.launch {
            val res = repository.applyCpuCoreOnline(coreId, online)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(userMessage = "CPU Core $coreId state updated")
            } else {
                _uiState.value = _uiState.value.copy(userMessage = "Failed: ${res.outputText}")
            }
            loadCpuData()
        }
    }

    fun applyCpuSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            val state = _uiState.value

            if (state.selectedGovernor.isNotBlank() && state.selectedGovernor != state.cpuInfo.currentGovernor) {
                repository.applyCpuGovernor(state.selectedGovernor)
            }

            if (state.selectedMinFreqKHz > 0) {
                repository.applyCpuMinFreq(state.selectedMinFreqKHz)
            }

            if (state.selectedMaxFreqKHz > 0) {
                repository.applyCpuMaxFreq(state.selectedMaxFreqKHz)
            }

            _uiState.value = _uiState.value.copy(
                isApplying = false,
                userMessage = "CPU settings applied successfully"
            )
            loadCpuData()
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
