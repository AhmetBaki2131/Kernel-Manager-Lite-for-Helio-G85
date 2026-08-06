package com.example.ui.screens.aianalyzer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GeminiAnalyzerRepository
import com.example.data.repository.KernelRepository
import com.example.root.SysfsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiAnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val kernelRepo = KernelRepository(application, db.logDao(), db.profileDao())
    private val geminiRepo = GeminiAnalyzerRepository(application)

    private val _diagnostic = MutableStateFlow<FullDeviceDiagnostic?>(null)
    val diagnostic: StateFlow<FullDeviceDiagnostic?> = _diagnostic.asStateFlow()

    private val _analysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val analysisResult: StateFlow<AiAnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isOnlineAi = MutableStateFlow(true)
    val isOnlineAi: StateFlow<Boolean> = _isOnlineAi.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _applyMessage = MutableStateFlow<String?>(null)
    val applyMessage: StateFlow<String?> = _applyMessage.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            try {
                val diag = SysfsHelper.collectFullDeviceDiagnostic(getApplication())
                _diagnostic.value = diag
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh telemetry: ${e.localizedMessage}"
            }
        }
    }

    fun runAiAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            _applyMessage.value = null

            val currentDiag = SysfsHelper.collectFullDeviceDiagnostic(getApplication())
            _diagnostic.value = currentDiag

            val result = geminiRepo.analyzeDevice(currentDiag)
            result.onSuccess { (aiResult, isOnline) ->
                _analysisResult.value = aiResult
                _isOnlineAi.value = isOnline
                _isAnalyzing.value = false
            }.onFailure { err ->
                _errorMessage.value = "AI Analysis error: ${err.localizedMessage}"
                _isAnalyzing.value = false
            }
        }
    }

    fun applyRecommendations(recs: AiRecommendations) {
        viewModelScope.launch {
            _isApplying.value = true
            _applyMessage.value = null
            val res = kernelRepo.applyAiRecommendations(recs)
            _isApplying.value = false
            if (res.isSuccess) {
                _applyMessage.value = "Successfully applied AI Recommendations: ${res.outputText}"
                refreshDiagnostics()
            } else {
                _errorMessage.value = "Failed to apply AI recommendations: ${res.outputText}"
            }
        }
    }

    fun applyProfile(profile: HelioG85Profile) {
        viewModelScope.launch {
            _isApplying.value = true
            _applyMessage.value = null
            val res = kernelRepo.applyHelioG85Profile(profile)
            _isApplying.value = false
            if (res.isSuccess) {
                _applyMessage.value = "Applied AI Profile: ${profile.title}"
                refreshDiagnostics()
            } else {
                _errorMessage.value = "Failed to apply profile ${profile.title}: ${res.outputText}"
            }
        }
    }


    fun clearMessages() {
        _errorMessage.value = null
        _applyMessage.value = null
    }
}
