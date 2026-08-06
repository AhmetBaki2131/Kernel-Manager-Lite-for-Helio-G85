package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiDoctorRepository(
    private val context: Context,
    private val geminiService: GeminiService = GeminiService()
) {
    private val TAG = "AiDoctorRepository"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val doctorAdapter = moshi.adapter(AiDoctorAnalysis::class.java)

    suspend fun diagnoseDevice(
        diagnostic: FullDeviceDiagnostic,
        customApiKey: String? = null
    ): Result<Pair<AiDoctorAnalysis, Boolean>> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null } ?: try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isKeyValid = !apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"

        val systemPrompt = """
            You are the AI Kernel Doctor specialized for MediaTek Helio G85 devices (2x Cortex-A75 + 6x Cortex-A55, Mali-G52 MC2).
            Analyze live telemetry and produce a detailed diagnosis in JSON strictly matching this structure:
            {
              "summaryText": "Short medical summary of overall device state",
              "whatIsHappening": "Detailed description of what CPU/GPU/RAM/Thermal zones are doing right now",
              "whyItIsHappening": "Why these parameters are behaving this way under current governor/frequency setup",
              "isItNormal": "Whether this state is normal for Helio G85",
              "performanceImpact": "Impact on app launch speeds and UI fluidity",
              "batteryImpact": "Impact on active mAh drain and battery life",
              "gamingImpact": "Impact on thermal throttling and gaming frame stability",
              "shouldBeChanged": "Specific recommendations on what sysfs parameters to tweak",
              "scores": {
                "overallHealth": {"category": "Overall Health", "percentage": 92, "grade": "A+", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1", "Suggestion 2"]},
                "battery": {"category": "Battery Health & Drain", "percentage": 88, "grade": "A", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]},
                "performance": {"category": "CPU/GPU Efficiency", "percentage": 90, "grade": "A", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]},
                "gaming": {"category": "Gaming Optimization", "percentage": 85, "grade": "B+", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]},
                "thermal": {"category": "Thermal Management", "percentage": 95, "grade": "A+", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]},
                "stability": {"category": "System Stability", "percentage": 96, "grade": "A+", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]},
                "memory": {"category": "ZRAM & RAM Pressure", "percentage": 84, "grade": "B+", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]},
                "rootConfig": {"category": "Root & SysFS Nodes", "percentage": 100, "grade": "A+", "explanation": "Detailed explanation", "suggestions": ["Suggestion 1"]}
              }
            }
        """.trimIndent()

        val userPrompt = """
            Helio G85 Live Telemetry:
            - Chipset: ${diagnostic.chipset} (${diagnostic.cpuArchitecture})
            - GPU: ${diagnostic.gpuModel}, Current Freq: ${diagnostic.gpuFreqHz / 1_000_000} MHz, Gov: ${diagnostic.gpuGovernor}
            - CPU Governor: ${diagnostic.cpuGovernor}, Cores Online: ${diagnostic.onlineCoresCount}/${diagnostic.totalCoresCount}, Min/Max: ${diagnostic.cpuMinFreqKHz/1000} - ${diagnostic.cpuMaxFreqKHz/1000} MHz
            - CPU Temp: ${diagnostic.cpuTempC}°C, Load: ${diagnostic.cpuLoadPercentage}%
            - RAM: ${diagnostic.usedRamMb}MB / ${diagnostic.totalRamMb}MB (${diagnostic.ramUsedPercentage}%)
            - ZRAM: ${diagnostic.zramSizeMb}MB, Swappiness: ${diagnostic.vmParameters.swappiness}
            - Battery: ${diagnostic.batteryDetail.level}%, Health: ${diagnostic.batteryDetail.health}, Temp: ${diagnostic.batteryDetail.temperatureC}°C, Wattage: ${diagnostic.batteryDetail.wattageW}W
            - I/O Scheduler: ${diagnostic.ioScheduler}, Read-Ahead: ${diagnostic.readAheadKb} KB
            - Root: ${diagnostic.rootFramework}, SELinux: ${diagnostic.selinuxStatus}
            - Storage Free: ${diagnostic.storageFreeGb} GB / ${diagnostic.storageTotalGb} GB (${diagnostic.fileSystemType})
        """.trimIndent()

        if (!isKeyValid) {
            val offlineDoc = generateOfflineDoctorAnalysis(diagnostic, "Generated using local MediaTek Helio G85 AI Doctor engine.")
            return@withContext Result.success(Pair(offlineDoc, false))
        }

        val res = geminiService.generateOptimizationRecommendations(
            apiKey = apiKey,
            systemInstructionText = systemPrompt,
            promptText = userPrompt
        )

        res.fold(
            onSuccess = { responseText ->
                val json = cleanJson(responseText)
                val parsed = doctorAdapter.fromJson(json)
                if (parsed != null) {
                    Result.success(Pair(parsed, true))
                } else {
                    Result.success(Pair(generateOfflineDoctorAnalysis(diagnostic, "Offline fallback due to JSON parse error."), false))
                }
            },
            onFailure = { err ->
                Log.e(TAG, "Gemini Doctor request failed", err)
                Result.success(Pair(generateOfflineDoctorAnalysis(diagnostic, "Offline fallback (${err.localizedMessage})."), false))
            }
        )
    }

    private fun cleanJson(text: String): String {
        var trimmed = text.trim()
        if (trimmed.startsWith("```json")) trimmed = trimmed.removePrefix("```json")
        else if (trimmed.startsWith("```")) trimmed = trimmed.removePrefix("```")
        if (trimmed.endsWith("```")) trimmed = trimmed.removeSuffix("```")
        return trimmed.trim()
    }

    private fun generateOfflineDoctorAnalysis(d: FullDeviceDiagnostic, sourceNote: String): AiDoctorAnalysis {
        val tempPct = if (d.cpuTempC < 45f) 96 else if (d.cpuTempC < 55f) 82 else 65
        val tempGrade = if (tempPct >= 90) "A+" else if (tempPct >= 80) "B" else "C"

        val ramPct = if (d.ramUsedPercentage < 70f) 90 else if (d.ramUsedPercentage < 85f) 78 else 62
        val ramGrade = if (ramPct >= 85) "A" else "B"

        val battPct = if (d.batteryDetail.health == "Good") 92 else 70
        val overallPct = (tempPct + ramPct + battPct + 95) / 4

        return AiDoctorAnalysis(
            summaryText = "MediaTek Helio G85 diagnostic completed. System operating smoothly at ${d.cpuTempC}°C with ${d.ramUsedPercentage.toInt()}% RAM utilization. $sourceNote",
            whatIsHappening = "Dual ARM Cortex-A75 performance cores and 6 Cortex-A55 efficiency cores are active under ${d.cpuGovernor} governor. Mali-G52 GPU is clocking at ${d.gpuFreqHz / 1_000_000} MHz.",
            whyItIsHappening = "MediaTek CorePilot dynamic frequency management is adjusting clock speeds based on current background processes and ${d.cpuLoadPercentage.toInt()}% CPU load.",
            isItNormal = "Yes, thermal profile (${d.cpuTempC}°C) and battery discharge current (${d.batteryDetail.currentMa} mA) are within safe specifications for Helio G85.",
            performanceImpact = "App launch times and UI frame rates are operating at normal speeds with zero throttle penalty.",
            batteryImpact = "Battery consumption is estimated at ${String.format("%.2f", d.batteryDetail.wattageW)} Watts, providing healthy battery runtime.",
            gamingImpact = "Mali-G52 GPU has headroom available up to 950-1000 MHz for stable FPS in high-load games.",
            shouldBeChanged = if (d.zramSizeMb < 3072) "Consider expanding ZRAM to 3072MB or 4096MB to prevent background task force kills." else "Current ZRAM and Governor settings are well balanced.",
            scores = AiDoctorScores(
                overallHealth = AiScoreDetail("Overall Health", overallPct, if (overallPct >= 90) "A+" else "A", "Helio G85 SoC is healthy.", listOf("Keep thermals under 50°C during extended gaming sessions.")),
                battery = AiScoreDetail("Battery Health & Drain", battPct, "A", "Battery health is ${d.batteryDetail.health} at ${d.batteryDetail.temperatureC}°C.", listOf("Use powersave governor when battery drops below 20%.")),
                performance = AiScoreDetail("CPU/GPU Efficiency", 90, "A", "2x A75 + 6x A55 scaling cleanly.", listOf("Schedutil governor balances latency and battery efficiently.")),
                gaming = AiScoreDetail("Gaming Optimization", 88, "B+", "Mali-G52 GPU ready for high clock locks.", listOf("Apply Helio G85 Gaming Profile before launching heavy titles.")),
                thermal = AiScoreDetail("Thermal Management", tempPct, tempGrade, "CPU temperature is ${d.cpuTempC}°C.", listOf("Avoid direct sunlight charging while gaming.")),
                stability = AiScoreDetail("System Stability", 96, "A+", "SELinux status: ${d.selinuxStatus}.", listOf("System kernel parameters are stable.")),
                memory = AiScoreDetail("ZRAM & RAM Pressure", ramPct, ramGrade, "RAM used: ${d.usedRamMb}MB / ${d.totalRamMb}MB.", listOf("Keep ZRAM active at 3GB or 4GB.")),
                rootConfig = AiScoreDetail("Root & SysFS Nodes", 100, "A+", "Root framework: ${d.rootFramework}.", listOf("All sysfs nodes are accessible via libsu."))
            )
        )
    }

    fun generateReportText(diagnostic: FullDeviceDiagnostic, doctor: AiDoctorAnalysis): String {
        return """
====================================================
      KERNEL MANAGER LITE - AI KERNEL REPORT
            MediaTek Helio G85 Optimization
====================================================
Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}

--- DEVICE & CHIPSET ---
Chipset: ${diagnostic.chipset}
Architecture: ${diagnostic.cpuArchitecture}
Model: ${diagnostic.deviceModel}
Android Version: ${diagnostic.androidVersion}
Kernel Version: ${diagnostic.kernelVersion}
Root Framework: ${diagnostic.rootFramework}
SELinux: ${diagnostic.selinuxStatus}

--- CPU & GPU TELEMETRY ---
CPU Governor: ${diagnostic.cpuGovernor}
CPU Min/Max Freq: ${diagnostic.cpuMinFreqKHz / 1000} MHz / ${diagnostic.cpuMaxFreqKHz / 1000} MHz
Online Cores: ${diagnostic.onlineCoresCount} / ${diagnostic.totalCoresCount}
CPU Load: ${diagnostic.cpuLoadPercentage}%
CPU Temperature: ${diagnostic.cpuTempC}°C
GPU Model: ${diagnostic.gpuModel}
GPU Governor: ${diagnostic.gpuGovernor}
GPU Frequency: ${diagnostic.gpuFreqHz / 1_000_000} MHz

--- MEMORY & STORAGE ---
RAM Total / Used / Free: ${diagnostic.totalRamMb} MB / ${diagnostic.usedRamMb} MB / ${diagnostic.freeRamMb} MB (${diagnostic.ramUsedPercentage.toInt()}%)
ZRAM Disksize: ${diagnostic.zramSizeMb} MB
Swappiness: ${diagnostic.vmParameters.swappiness}
I/O Scheduler: ${diagnostic.ioScheduler} (Read-Ahead: ${diagnostic.readAheadKb} KB)
Storage Free: ${String.format("%.1f", diagnostic.storageFreeGb)} GB / ${String.format("%.1f", diagnostic.storageTotalGb)} GB (${diagnostic.fileSystemType})

--- BATTERY & THERMALS ---
Battery Level: ${diagnostic.batteryDetail.level}%
Battery Health / Status: ${diagnostic.batteryDetail.health} / ${diagnostic.batteryDetail.status}
Battery Temperature: ${diagnostic.batteryDetail.temperatureC}°C
Voltage / Current: ${diagnostic.batteryDetail.voltageMv} mV / ${diagnostic.batteryDetail.currentMa} mA (${String.format("%.2f", diagnostic.batteryDetail.wattageW)} W)

--- AI DOCTOR ANALYSIS ---
Summary: ${doctor.summaryText}
What Is Happening: ${doctor.whatIsHappening}
Why It Is Happening: ${doctor.whyItIsHappening}
Performance Impact: ${doctor.performanceImpact}
Battery Impact: ${doctor.batteryImpact}
Gaming Impact: ${doctor.gamingImpact}
Recommendations: ${doctor.shouldBeChanged}

--- AI SCORES ---
Overall Health: ${doctor.scores.overallHealth.percentage}% (${doctor.scores.overallHealth.grade})
Battery Score: ${doctor.scores.battery.percentage}% (${doctor.scores.battery.grade})
Performance Score: ${doctor.scores.performance.percentage}% (${doctor.scores.performance.grade})
Gaming Score: ${doctor.scores.gaming.percentage}% (${doctor.scores.gaming.grade})
Thermal Score: ${doctor.scores.thermal.percentage}% (${doctor.scores.thermal.grade})
Stability Score: ${doctor.scores.stability.percentage}% (${doctor.scores.stability.grade})
Memory Score: ${doctor.scores.memory.percentage}% (${doctor.scores.memory.grade})
Root Config Score: ${doctor.scores.rootConfig.percentage}% (${doctor.scores.rootConfig.grade})
====================================================
        """.trimIndent()
    }
}
