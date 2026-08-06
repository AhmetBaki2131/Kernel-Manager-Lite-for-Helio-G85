package com.example.ui.aidoctor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AiDoctorAnalysis
import com.example.data.model.AiHistoryEntity
import com.example.data.model.FullDeviceDiagnostic
import com.example.data.repository.AiDoctorRepository
import com.example.data.repository.SettingsRepository
import com.example.root.SysfsHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AiDoctorUiState(
    val isLoading: Boolean = false,
    val diagnostic: FullDeviceDiagnostic = FullDeviceDiagnostic(),
    val doctorAnalysis: AiDoctorAnalysis = AiDoctorAnalysis(),
    val isLiveGeminiUsed: Boolean = false,
    val reportText: String = "",
    val errorMessage: String? = null
)

class AiDoctorViewModel(application: Application) : AndroidViewModel(application) {

    private val doctorRepository = AiDoctorRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val _uiState = MutableStateFlow(AiDoctorUiState())
    val uiState: StateFlow<AiDoctorUiState> = _uiState.asStateFlow()

    init {
        runDoctorDiagnosis()
    }

    fun runDoctorDiagnosis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val diagnostic = SysfsHelper.collectFullDeviceDiagnostic(getApplication())
                val customApiKey = settingsRepository.customApiKeyFlow.first()

                val result = doctorRepository.diagnoseDevice(diagnostic, customApiKey)

                result.fold(
                    onSuccess = { pair ->
                        val analysis = pair.first
                        val isLive = pair.second
                        val report = doctorRepository.generateReportText(diagnostic, analysis)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            diagnostic = diagnostic,
                            doctorAnalysis = analysis,
                            isLiveGeminiUsed = isLive,
                            reportText = report
                        )

                        // Save run to AI History in Room
                        saveToHistory(diagnostic, analysis)
                    },
                    onFailure = { err ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to run AI Doctor diagnosis."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unexpected error running diagnosis."
                )
            }
        }
    }

    private suspend fun saveToHistory(diagnostic: FullDeviceDiagnostic, analysis: AiDoctorAnalysis) {
        try {
            val adapter = moshi.adapter(AiDoctorAnalysis::class.java)
            val json = adapter.toJson(analysis)
            val entity = AiHistoryEntity(
                timestamp = System.currentTimeMillis(),
                overallScore = analysis.scores.overallHealth.percentage,
                deviceOverview = analysis.summaryText,
                cpuTempC = diagnostic.cpuTempC,
                ramUsedPercentage = diagnostic.ramUsedPercentage,
                batteryLevel = diagnostic.batteryDetail.level,
                jsonResult = json
            )
            db.aiHistoryDao().insertHistory(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
