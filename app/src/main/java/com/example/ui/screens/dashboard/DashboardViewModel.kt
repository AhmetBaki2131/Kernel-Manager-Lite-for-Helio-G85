package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.*
import com.example.data.repository.KernelRepository
import com.example.root.SysfsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val cpuInfo: CpuInfo = CpuInfo(),
    val gpuInfo: GpuInfo = GpuInfo(),
    val ramInfo: RamInfo = RamInfo(),
    val thermalInfo: ThermalInfo = ThermalInfo(),
    val ioSchedulerInfo: IoSchedulerInfo = IoSchedulerInfo(),
    val errorMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = KernelRepository(application, database.logDao(), database.profileDao())

    init {
        loadDashboardData()
        startLiveUpdates()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val context = getApplication<Application>()
                val device = SysfsHelper.getDeviceInfo(context)
                val cpu = SysfsHelper.getCpuInfo()
                val gpu = SysfsHelper.getGpuInfo()
                val ram = SysfsHelper.getRamInfo(context)
                val thermal = SysfsHelper.getThermalInfo(context)
                val ioSched = SysfsHelper.getIoSchedulerInfo()

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    deviceInfo = device,
                    cpuInfo = cpu,
                    gpuInfo = gpu,
                    ramInfo = ram,
                    thermalInfo = thermal,
                    ioSchedulerInfo = ioSched
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to read system status: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun startLiveUpdates() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Poll every 3s
                val context = getApplication<Application>()
                val ram = SysfsHelper.getRamInfo(context)
                val thermal = SysfsHelper.getThermalInfo(context)
                val cpu = SysfsHelper.getCpuInfo()
                _uiState.value = _uiState.value.copy(
                    ramInfo = ram,
                    thermalInfo = thermal,
                    cpuInfo = cpu
                )
            }
        }
    }
}
