package com.example.ui.screens.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TerminalCommandEntity
import com.example.root.RootShell
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TerminalUiState(
    val commandInput: String = "",
    val commandHistory: List<TerminalCommandEntity> = emptyList(),
    val isExecuting: Boolean = false,
    val userMessage: String? = null
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val terminalDao = database.terminalDao()

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = combine(
        _uiState,
        terminalDao.getAllCommands()
    ) { state, history ->
        state.copy(commandHistory = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TerminalUiState())

    fun onCommandInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(commandInput = input)
    }

    fun executeCommand() {
        val cmd = _uiState.value.commandInput.trim()
        if (cmd.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExecuting = true)
            val result = RootShell.runCommand(cmd)

            val entity = TerminalCommandEntity(
                command = cmd,
                output = result.outputText,
                exitCode = result.exitCode
            )
            terminalDao.insertCommand(entity)

            _uiState.value = _uiState.value.copy(
                commandInput = "",
                isExecuting = false
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            terminalDao.clearHistory()
            _uiState.value = _uiState.value.copy(userMessage = "Terminal history cleared")
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
