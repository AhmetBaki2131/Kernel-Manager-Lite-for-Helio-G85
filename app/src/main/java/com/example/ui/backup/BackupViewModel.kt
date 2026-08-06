package com.example.ui.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BackupEntity
import com.example.root.SysfsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)

    val backupsFlow = db.backupDao().getAllBackups()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun createBackup(title: String) {
        viewModelScope.launch {
            try {
                val diagnostic = SysfsHelper.collectFullDeviceDiagnostic(getApplication())
                val backup = BackupEntity(
                    title = if (title.isBlank()) "Kernel Backup ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}" else title,
                    timestamp = System.currentTimeMillis(),
                    cpuGovernor = diagnostic.cpuGovernor,
                    cpuMinFreqKHz = diagnostic.cpuMinFreqKHz,
                    cpuMaxFreqKHz = diagnostic.cpuMaxFreqKHz,
                    gpuGovernor = diagnostic.gpuGovernor,
                    gpuMaxFreqHz = diagnostic.maxGpuFreqHz,
                    zramSizeMb = diagnostic.zramSizeMb,
                    swappiness = diagnostic.vmParameters.swappiness,
                    ioScheduler = diagnostic.ioScheduler,
                    readAheadKb = diagnostic.readAheadKb
                )
                db.backupDao().insertBackup(backup)
                _statusMessage.value = "Backup '${backup.title}' saved successfully."
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.localizedMessage}"
            }
        }
    }

    fun restoreBackup(backup: BackupEntity) {
        viewModelScope.launch {
            try {
                SysfsHelper.setCpuGovernor(backup.cpuGovernor)
                SysfsHelper.setCpuMinFreq(backup.cpuMinFreqKHz)
                SysfsHelper.setCpuMaxFreq(backup.cpuMaxFreqKHz)
                SysfsHelper.setIoScheduler(backup.ioScheduler)
                SysfsHelper.resizeZram(backup.zramSizeMb)

                _statusMessage.value = "Backup '${backup.title}' restored with 1-tap!"
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteBackup(backup: BackupEntity) {
        viewModelScope.launch {
            db.backupDao().deleteBackup(backup)
            _statusMessage.value = "Backup deleted."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
