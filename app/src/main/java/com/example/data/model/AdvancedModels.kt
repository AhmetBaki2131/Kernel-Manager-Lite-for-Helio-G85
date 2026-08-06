package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- AI SCORES & DOCTOR MODELS ---

data class AiScoreDetail(
    val category: String = "",
    val percentage: Int = 85,
    val grade: String = "A", // A+, A, B, C, D, F
    val explanation: String = "",
    val suggestions: List<String> = emptyList()
)

data class AiDoctorScores(
    val overallHealth: AiScoreDetail = AiScoreDetail("Overall Health", 92, "A+", "System is performing well under normal thermal range."),
    val battery: AiScoreDetail = AiScoreDetail("Battery Health & Drain", 88, "A", "Discharging rate is normal with 0 mV voltage variance."),
    val performance: AiScoreDetail = AiScoreDetail("CPU/GPU Efficiency", 90, "A", "Helio G85 Cortex-A75 cores scaling smoothly."),
    val gaming: AiScoreDetail = AiScoreDetail("Gaming Optimization", 85, "B+", "Mali-G52 clock is stable at 950MHz."),
    val thermal: AiScoreDetail = AiScoreDetail("Thermal Management", 95, "A+", "Thermal zone stays under 45°C."),
    val stability: AiScoreDetail = AiScoreDetail("System Stability", 96, "A+", "No kernel panics or LMK kill bursts detected."),
    val memory: AiScoreDetail = AiScoreDetail("ZRAM & RAM Pressure", 84, "B+", "ZRAM active with 3GB compressed swap."),
    val rootConfig: AiScoreDetail = AiScoreDetail("Root & SysFS Nodes", 100, "A+", "Root access granted via libsu with full sysfs access.")
)

data class AiDoctorAnalysis(
    val summaryText: String = "",
    val whatIsHappening: String = "",
    val whyItIsHappening: String = "",
    val isItNormal: String = "",
    val performanceImpact: String = "",
    val batteryImpact: String = "",
    val gamingImpact: String = "",
    val shouldBeChanged: String = "",
    val scores: AiDoctorScores = AiDoctorScores()
)

// --- ROOM ENTITY FOR HISTORY & COMPARISON ---

@Entity(tableName = "ai_history")
data class AiHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val overallScore: Int = 90,
    val deviceOverview: String = "",
    val cpuTempC: Float = 38.0f,
    val ramUsedPercentage: Float = 50.0f,
    val batteryLevel: Int = 100,
    val jsonResult: String = "" // Full JSON payload for comparison
)

// --- ROOM ENTITY FOR BACKUPS ---

@Entity(tableName = "kernel_backups")
data class BackupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val cpuGovernor: String = "schedutil",
    val cpuMinFreqKHz: Long = 800000L,
    val cpuMaxFreqKHz: Long = 2000000L,
    val gpuGovernor: String = "simple_ondemand",
    val gpuMaxFreqHz: Long = 950000000L,
    val zramSizeMb: Long = 3072L,
    val swappiness: Int = 60,
    val ioScheduler: String = "mq-deadline",
    val readAheadKb: Int = 128,
    val vmParametersJson: String = ""
)

// --- SYSFS EXPLORER NODE ITEM ---

data class SysfsNodeItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isWritable: Boolean = false,
    val isReadable: Boolean = true,
    val currentValue: String = "",
    val isFavorite: Boolean = false
)

data class SysfsNodeAiExplanation(
    val path: String = "",
    val whatItControls: String = "",
    val isSafeToEdit: Boolean = true,
    val performanceImpact: String = "",
    val batteryImpact: String = "",
    val temperatureImpact: String = "",
    val possibleRisks: String = ""
)

// --- BENCHMARK RESULT ---

data class BenchmarkResult(
    val timestamp: Long = System.currentTimeMillis(),
    val cpuSingleCoreScore: Int = 0,
    val cpuMultiCoreScore: Int = 0,
    val ramReadWriteMbps: Int = 0,
    val storageReadWriteMbps: Int = 0,
    val totalScore: Int = 0,
    val peakTempC: Float = 0f,
    val durationSeconds: Int = 0
)
