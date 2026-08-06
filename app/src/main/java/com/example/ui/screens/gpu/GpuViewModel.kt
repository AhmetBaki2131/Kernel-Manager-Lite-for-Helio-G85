package com.example.ui.screens.gpu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.GpuInfo
import com.example.data.repository.KernelRepository
import com.example.root.SysfsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GpuUiState(
    val isLoading: Boolean = true,
    val gpuInfo: GpuInfo = GpuInfo(),
    val selectedGovernor: String = "",
    val selectedMaxFreqHz: Long = 0L,
    val isApplying: Boolean = false,
    val userMessage: String? = null
)

class GpuViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GpuUiState())
    val uiState: StateFlow<GpuUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())

    init {
        loadGpuData()
    }

    fun loadGpuData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val gpu = SysfsHelper.getGpuInfo()
                _uiState.value = GpuUiState(
                    isLoading = false,
                    gpuInfo = gpu,
                    selectedGovernor = gpu.currentGovernor,
                    selectedMaxFreqHz = gpu.maxFreqHz
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "Failed to load GPU info"
                )
            }
        }
    }

    fun onGovernorSelected(governor: String) {
        _uiState.value = _uiState.value.copy(selectedGovernor = governor)
    }

    fun onFreqSelected(freqHz: Long) {
        _uiState.value = _uiState.value.copy(selectedMaxFreqHz = freqHz)
    }

    fun applyGpuSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            val state = _uiState.value
            val path = state.gpuInfo.path

            if (path.isNotBlank()) {
                if (state.selectedGovernor.isNotBlank()) {
                    repository.applyGpuGovernor(path, state.selectedGovernor)
                }
                if (state.selectedMaxFreqHz > 0) {
                    repository.applyGpuMaxFreq(path, state.selectedMaxFreqHz)
                }
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    userMessage = "GPU settings applied successfully"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    userMessage = "GPU control not supported on this kernel"
                )
            }
            loadGpuData()
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
