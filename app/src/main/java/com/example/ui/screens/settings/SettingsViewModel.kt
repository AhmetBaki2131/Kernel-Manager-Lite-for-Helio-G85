package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.KernelRepository
import com.example.root.RootShell
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val applyOnBoot: Boolean = false,
    val themeMode: String = "dark",
    val dynamicColor: Boolean = true,
    val showDangerWarnings: Boolean = true,
    val isRootGranted: Boolean = false,
    val userMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = combine(
        settings.applyOnBoot,
        settings.themeMode,
        settings.dynamicColor,
        settings.showDangerWarnings
    ) { boot, theme, dynamic, warnings ->
        SettingsUiState(
            applyOnBoot = boot,
            themeMode = theme,
            dynamicColor = dynamic,
            showDangerWarnings = warnings,
            isRootGranted = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        checkRootStatus()
    }

    fun checkRootStatus() {
        viewModelScope.launch {
            val root = RootShell.isRootAvailable()
            _uiState.value = _uiState.value.copy(isRootGranted = root)
        }
    }

    fun setApplyOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !RootShell.isRootAvailable()) {
                _uiState.value = _uiState.value.copy(userMessage = "Root access is required to enable Apply on Boot")
                settings.setApplyOnBoot(false)
            } else {
                settings.setApplyOnBoot(enabled)
                _uiState.value = _uiState.value.copy(userMessage = if (enabled) "Apply on Boot enabled" else "Apply on Boot disabled")
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settings.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDynamicColor(enabled)
        }
    }

    fun setShowDangerWarnings(enabled: Boolean) {
        viewModelScope.launch {
            settings.setShowDangerWarnings(enabled)
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
