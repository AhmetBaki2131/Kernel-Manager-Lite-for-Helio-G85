package com.example.ui.sysfsexplorer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SysfsNodeAiExplanation
import com.example.data.model.SysfsNodeItem
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SysfsExplorerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SysfsExplorerUiState(
    val currentPath: String = "/sys",
    val items: List<SysfsNodeItem> = emptyList(),
    val filteredItems: List<SysfsNodeItem> = emptyList(),
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val recentPaths: List<String> = listOf("/sys", "/sys/devices/system/cpu", "/sys/class/devfreq", "/proc/sys/vm"),
    val selectedNodeForEdit: SysfsNodeItem? = null,
    val selectedNodeExplanation: SysfsNodeAiExplanation? = null,
    val isLoadingExplanation: Boolean = false,
    val isLoadingDirectory: Boolean = false,
    val statusMessage: String? = null
)

class SysfsExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SysfsExplorerRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SysfsExplorerUiState())
    val uiState: StateFlow<SysfsExplorerUiState> = _uiState.asStateFlow()

    init {
        navigateToPath("/sys")
    }

    fun navigateToPath(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDirectory = true, searchQuery = "")
            val items = repository.listDirectory(path)

            val recents = (_uiState.value.recentPaths + path).distinct().takeLast(8)

            _uiState.value = _uiState.value.copy(
                currentPath = path,
                items = items,
                filteredItems = items,
                recentPaths = recents,
                isLoadingDirectory = false
            )
        }
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        if (current == "/" || current == "/sys") return
        val parent = current.substringBeforeLast('/')
        val target = if (parent.isEmpty()) "/" else parent
        navigateToPath(target)
    }

    fun onSearchQueryChanged(query: String) {
        val items = _uiState.value.items
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) || it.path.contains(query, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredItems = filtered)
    }

    fun toggleFavorite(path: String) {
        val currentFavs = _uiState.value.favorites.toMutableSet()
        if (currentFavs.contains(path)) {
            currentFavs.remove(path)
        } else {
            currentFavs.add(path)
        }
        _uiState.value = _uiState.value.copy(favorites = currentFavs)
    }

    fun selectNodeForEdit(node: SysfsNodeItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedNodeForEdit = node,
                isLoadingExplanation = true,
                selectedNodeExplanation = null
            )

            val apiKey = settingsRepository.customApiKeyFlow.first()
            val explanation = repository.getAiNodeExplanation(node.path, node.currentValue, apiKey)

            explanation.fold(
                onSuccess = { expl ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingExplanation = false,
                        selectedNodeExplanation = expl
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isLoadingExplanation = false)
                }
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(selectedNodeForEdit = null, selectedNodeExplanation = null)
    }

    fun writeNodeValue(path: String, newValue: String) {
        viewModelScope.launch {
            val success = repository.writeNodeValue(path, newValue)
            val msg = if (success) "Successfully written '$newValue' to $path" else "Failed to write value. Verify root permissions."
            _uiState.value = _uiState.value.copy(statusMessage = msg)
            dismissEditDialog()
            // Refresh current path
            navigateToPath(_uiState.value.currentPath)
        }
    }
}
