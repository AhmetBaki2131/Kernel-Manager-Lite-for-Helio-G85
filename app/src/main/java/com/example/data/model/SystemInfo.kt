package com.example.data.model

data class DeviceInfo(
    val model: String = "Unknown Device",
    val androidVersion: String = "Unknown Android",
    val kernelVersion: String = "Unknown Kernel",
    val isRooted: Boolean = false
)

data class CpuCoreInfo(
    val coreId: Int,
    val isOnline: Boolean,
    val currentFreqKHz: Long,
    val minFreqKHz: Long,
    val maxFreqKHz: Long,
    val governor: String
) {
    val currentFreqMhz: Long get() = currentFreqKHz / 1000
    val minFreqMhz: Long get() = minFreqKHz / 1000
    val maxFreqMhz: Long get() = maxFreqKHz / 1000
}

data class CpuInfo(
    val totalCores: Int = 0,
    val cores: List<CpuCoreInfo> = emptyList(),
    val currentGovernor: String = "N/A",
    val availableGovernors: List<String> = emptyList(),
    val globalMinFreqKHz: Long = 0L,
    val globalMaxFreqKHz: Long = 0L,
    val availableFrequenciesKHz: List<Long> = emptyList()
)

data class GpuInfo(
    val isSupported: Boolean = false,
    val model: String = "N/A",
    val path: String = "",
    val currentFreqHz: Long = 0L,
    val minFreqHz: Long = 0L,
    val maxFreqHz: Long = 0L,
    val currentGovernor: String = "N/A",
    val availableGovernors: List<String> = emptyList(),
    val availableFrequenciesHz: List<Long> = emptyList()
) {
    val currentFreqMhz: Long get() = currentFreqHz / 1_000_000
    val minFreqMhz: Long get() = minFreqHz / 1_000_000
    val maxFreqMhz: Long get() = maxFreqHz / 1_000_000
}

data class ZramInfo(
    val isSupported: Boolean = false,
    val diskSizeMb: Long = 0L,
    val isEnabled: Boolean = false,
    val swapTotalMb: Long = 0L,
    val swapFreeMb: Long = 0L,
    val swapUsedMb: Long = 0L
)

data class IoSchedulerInfo(
    val isSupported: Boolean = false,
    val targetBlockDevice: String = "N/A",
    val currentScheduler: String = "N/A",
    val availableSchedulers: List<String> = emptyList()
)

data class RamInfo(
    val totalMb: Long = 0L,
    val usedMb: Long = 0L,
    val freeMb: Long = 0L,
    val usedPercentage: Float = 0f
)

data class ThermalInfo(
    val cpuTempC: Float = 0f,
    val batteryTempC: Float = 0f
)
