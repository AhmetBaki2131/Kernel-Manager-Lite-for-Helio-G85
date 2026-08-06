package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AiHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)

    val historyFlow = db.aiHistoryDao().getAllHistory()

    private val _selectedForComparison = MutableStateFlow<List<AiHistoryEntity>>(emptyList())
    val selectedForComparison: StateFlow<List<AiHistoryEntity>> = _selectedForComparison.asStateFlow()

    fun toggleComparisonSelection(item: AiHistoryEntity) {
        val current = _selectedForComparison.value.toMutableList()
        if (current.any { it.id == item.id }) {
            current.removeAll { it.id == item.id }
        } else {
            if (current.size >= 2) {
                current.removeAt(0) // keep max 2
            }
            current.add(item)
        }
        _selectedForComparison.value = current
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.aiHistoryDao().clearHistory()
            _selectedForComparison.value = emptyList()
        }
    }
}
