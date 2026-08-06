package com.example.ui.screens.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.ProfileEntity
import com.example.data.repository.KernelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val isLoading: Boolean = true,
    val profiles: List<ProfileEntity> = emptyList(),
    val selectedProfileId: Long = 2L,
    val isApplying: Boolean = false,
    val editingProfile: ProfileEntity? = null,
    val showEditorDialog: Boolean = false,
    val userMessage: String? = null
)

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())
    private val settings = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            combine(
                database.profileDao().getAllProfiles(),
                settings.selectedProfileId
            ) { profilesList, selectedId ->
                ProfilesUiState(
                    isLoading = false,
                    profiles = profilesList,
                    selectedProfileId = selectedId
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun applyProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true)
            val res = repository.applyProfile(profile)
            if (res.isSuccess) {
                settings.setSelectedProfileId(profile.id)
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    userMessage = "Profile '${profile.name}' applied successfully!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    userMessage = "Failed to apply profile: ${res.outputText}"
                )
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(
            editingProfile = ProfileEntity(name = "", description = ""),
            showEditorDialog = true
        )
    }

    fun openEditDialog(profile: ProfileEntity) {
        _uiState.value = _uiState.value.copy(
            editingProfile = profile,
            showEditorDialog = true
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showEditorDialog = false, editingProfile = null)
    }

    fun saveProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            if (profile.id == 0L) {
                database.profileDao().insertProfile(profile)
            } else {
                database.profileDao().updateProfile(profile)
            }
            dismissDialog()
            _uiState.value = _uiState.value.copy(userMessage = "Profile saved")
        }
    }

    fun duplicateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            val copy = profile.copy(
                id = 0L,
                name = "${profile.name} (Copy)",
                isBuiltIn = false
            )
            database.profileDao().insertProfile(copy)
            _uiState.value = _uiState.value.copy(userMessage = "Profile duplicated")
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            if (profile.isBuiltIn) {
                _uiState.value = _uiState.value.copy(userMessage = "Built-in profiles cannot be deleted")
                return@launch
            }
            database.profileDao().deleteProfile(profile)
            _uiState.value = _uiState.value.copy(userMessage = "Profile deleted")
        }
    }

    fun exportProfiles(): String {
        var jsonStr = "[]"
        viewModelScope.launch {
            jsonStr = repository.exportProfilesToJson(_uiState.value.profiles)
            _uiState.value = _uiState.value.copy(userMessage = "Profiles exported to JSON")
        }
        return jsonStr
    }

    fun importProfiles(jsonStr: String) {
        viewModelScope.launch {
            val count = repository.importProfilesFromJson(jsonStr)
            _uiState.value = _uiState.value.copy(
                userMessage = if (count > 0) "Imported $count profiles" else "Invalid JSON format"
            )
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
