package com.example.ui.screens.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.LogEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogsUiState(
    val logs: List<LogEntity> = emptyList(),
    val userMessage: String? = null
)

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val logDao = database.logDao()

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = combine(
        _uiState,
        logDao.getAllLogs()
    ) { state, logsList ->
        state.copy(logs = logsList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogsUiState())

    fun clearLogs() {
        viewModelScope.launch {
            logDao.clearLogs()
            _uiState.value = _uiState.value.copy(userMessage = "Execution logs cleared")
        }
    }

    fun getExportableLogsText(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("=== KERNEL MANAGER LITE EXECUTION LOGS ===")
        for (log in _uiState.value.logs) {
            val dateStr = sdf.format(Date(log.timestamp))
            val status = if (log.success) "SUCCESS" else "FAILED"
            sb.appendLine("[$dateStr] [$status] ${log.actionName} - ${log.message}")
        }
        return sb.toString()
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
