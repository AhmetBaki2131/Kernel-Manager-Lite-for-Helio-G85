package com.example.ui.screens.zram

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ZramInfo
import com.example.data.repository.KernelRepository
import com.example.root.SysfsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ZramUiState(
    val isLoading: Boolean = true,
    val zramInfo: ZramInfo = ZramInfo(),
    val targetSizeMb: Long = 2048L,
    val showConfirmDialog: Boolean = false,
    val isApplying: Boolean = false,
    val userMessage: String? = null
)

class ZramViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ZramUiState())
    val uiState: StateFlow<ZramUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())

    init {
        loadZramData()
    }

    fun loadZramData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val zram = SysfsHelper.getZramInfo()
                _uiState.value = ZramUiState(
                    isLoading = false,
                    zramInfo = zram,
                    targetSizeMb = if (zram.diskSizeMb > 0) zram.diskSizeMb else 2048L
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "Failed to read ZRAM state"
                )
            }
        }
    }

    fun onSizeSelected(sizeMb: Long) {
        _uiState.value = _uiState.value.copy(targetSizeMb = sizeMb)
    }

    fun requestResizeConfirm() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = true)
    }

    fun dismissConfirm() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    fun toggleZram(enable: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            val res = repository.applyZramState(enable)
            _uiState.value = _uiState.value.copy(
                isApplying = false,
                userMessage = if (res.isSuccess) "ZRAM state toggled" else "Failed: ${res.outputText}"
            )
            loadZramData()
        }
    }

    fun applyResize() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, showConfirmDialog = false)
            val target = _uiState.value.targetSizeMb
            val res = repository.applyZramSize(target)
            _uiState.value = _uiState.value.copy(
                isApplying = false,
                userMessage = if (res.isSuccess) "ZRAM resized to ${target}MB" else "Failed: ${res.outputText}"
            )
            loadZramData()
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
