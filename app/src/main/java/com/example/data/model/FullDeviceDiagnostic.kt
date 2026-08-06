package com.example.data.model

data class BatteryDetail(
    val health: String = "Good",
    val status: String = "Discharging",
    val level: Int = 100,
    val voltageMv: Int = 4000,
    val currentMa: Int = 0,
    val wattageW: Float = 0.0f,
    val cycles: Int = 0,
    val temperatureC: Float = 30.0f
)

data class VmParameters(
    val swappiness: Int = 60,
    val dirtyRatio: Int = 20,
    val dirtyBackgroundRatio: Int = 10,
    val vfsCachePressure: Int = 100,
    val extraFreeKbytes: Int = 0
)

data class FullDeviceDiagnostic(
    val chipset: String = "MediaTek Helio G85",
    val cpuArchitecture: String = "2x Cortex-A75 (2.0 GHz) + 6x Cortex-A55 (1.8 GHz)",
    val gpuModel: String = "Mali-G52 MC2 (up to 1000 MHz)",
    val deviceModel: String = "MediaTek Helio G85 Phone",
    val androidVersion: String = "Android 13 / 14",
    val kernelVersion: String = "Linux 4.14 / 5.10 / 5.15",
    val isRooted: Boolean = true,
    val rootFramework: String = "Magisk / APatch",
    val selinuxStatus: String = "Enforcing",
    val cpuGovernor: String = "schedutil",
    val availableCpuGovernors: List<String> = emptyList(),
    val cpuMinFreqKHz: Long = 800000L,
    val cpuMaxFreqKHz: Long = 2000000L,
    val availableCpuFreqsKHz: List<Long> = emptyList(),
    val onlineCoresCount: Int = 8,
    val totalCoresCount: Int = 8,
    val cpuLoadPercentage: Float = 15.0f,
    val cpuTempC: Float = 38.0f,
    val gpuGovernor: String = "simple_ondemand",
    val availableGpuGovernors: List<String> = emptyList(),
    val gpuFreqHz: Long = 950000000L,
    val minGpuFreqHz: Long = 300000000L,
    val maxGpuFreqHz: Long = 1000000000L,
    val availableGpuFreqsHz: List<Long> = emptyList(),
    val totalRamMb: Long = 6144L,
    val usedRamMb: Long = 3072L,
    val freeRamMb: Long = 3072L,
    val ramUsedPercentage: Float = 50.0f,
    val isZramSupported: Boolean = true,
    val zramSizeMb: Long = 3072L,
    val isZramEnabled: Boolean = true,
    val swapTotalMb: Long = 3072L,
    val swapUsedMb: Long = 1024L,
    val swapFreeMb: Long = 2048L,
    val batteryDetail: BatteryDetail = BatteryDetail(),
    val thermalStatus: String = "Normal",
    val ioScheduler: String = "mq-deadline",
    val targetBlockDevice: String = "mmcblk0",
    val availableIoSchedulers: List<String> = emptyList(),
    val readAheadKb: Int = 128,
    val vmParameters: VmParameters = VmParameters(),
    val lmkdMinfree: String = "18432,23040,27648,32256,55296,80640",
    val storageTotalGb: Float = 128.0f,
    val storageFreeGb: Float = 45.0f,
    val fileSystemType: String = "f2fs",
    val installedModules: List<String> = emptyList(),
    val supportedSysfsNodes: List<String> = emptyList()
)

data class AiRecommendations(
    val cpuGovernor: String = "schedutil",
    val cpuMinFreqMhz: Int = 800,
    val cpuMaxFreqMhz: Int = 2000,
    val gpuFreqMhz: Int = 950,
    val zramSizeMb: Int = 3072,
    val swappiness: Int = 60,
    val ioScheduler: String = "mq-deadline",
    val readAheadKb: Int = 128,
    val vmParameters: String = "dirty_ratio=20, dirty_background_ratio=10, vfs_cache_pressure=100",
    val lmkdSettings: String = "Optimal LMKD parameters for Helio G85 4GB/6GB RAM"
)

data class AiExplanation(
    val target: String = "",
    val explanation: String = ""
)

data class HelioG85Profile(
    val title: String = "",
    val description: String = "",
    val cpuGovernor: String = "schedutil",
    val cpuMinFreqKHz: Long = 800000L,
    val cpuMaxFreqKHz: Long = 2000000L,
    val gpuGovernor: String = "simple_ondemand",
    val gpuFreqHz: Long = 950000000L,
    val zramSizeMb: Long = 3072L,
    val ioScheduler: String = "mq-deadline",
    val swappiness: Int = 60
)

data class AiAnalysisResult(
    val deviceOverview: String = "",
    val recommendations: AiRecommendations = AiRecommendations(),
    val explanations: List<AiExplanation> = emptyList(),
    val profiles: List<HelioG85Profile> = emptyList(),
    val safetyNotes: String = ""
)
