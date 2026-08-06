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

class GeminiAnalyzerRepository(
    private val context: Context,
    private val geminiService: GeminiService = GeminiService()
) {
    private val TAG = "GeminiAnalyzerRepo"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val analysisAdapter = moshi.adapter(AiAnalysisResult::class.java)

    suspend fun analyzeDevice(
        diagnostic: FullDeviceDiagnostic,
        customApiKey: String? = null
    ): Result<Pair<AiAnalysisResult, Boolean>> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null } ?: try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isKeyValid = !apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"

        val promptJson = buildPromptText(diagnostic)

        if (!isKeyValid) {
            Log.w(TAG, "Gemini API key is missing or default. Returning offline Helio G85 analysis.")
            val offlineResult = generateOfflineHelioG85Analysis(
                diagnostic,
                "Gemini API Key is not configured in Secrets panel. Generated using local MediaTek Helio G85 engine."
            )
            return@withContext Result.success(Pair(offlineResult, false))
        }

        try {
            val systemText = """
                You are an expert Linux Kernel & Android Hardware Optimization Engine specialized for MediaTek Helio G85 devices.
                Helio G85 Specs: 2x ARM Cortex-A75 (2.0 GHz) + 6x ARM Cortex-A55 (1.8 GHz), ARM Mali-G52 MC2 GPU (up to 1000 MHz).
                Analyze the provided telemetry and return JSON strictly matching this structure:
                {
                  "deviceOverview": "String summary of Helio G85 thermal/memory state",
                  "recommendations": {
                    "cpuGovernor": "schedutil",
                    "cpuMinFreqMhz": 800,
                    "cpuMaxFreqMhz": 2000,
                    "gpuFreqMhz": 950,
                    "zramSizeMb": 3072,
                    "swappiness": 60,
                    "ioScheduler": "mq-deadline",
                    "readAheadKb": 128,
                    "vmParameters": "dirty_ratio=20, dirty_background_ratio=10, vfs_cache_pressure=100",
                    "lmkdSettings": "Minfree thresholds optimized for 4GB/6GB RAM"
                  },
                  "explanations": [
                    {"target": "CPU Governor & Scaling", "explanation": "Explanation for Cortex-A75 & A55"},
                    {"target": "GPU Mali-G52 Frequency", "explanation": "Explanation for Mali-G52 scaling"},
                    {"target": "ZRAM & Swappiness", "explanation": "Explanation for RAM/swap"},
                    {"target": "I/O Scheduler & Read Ahead", "explanation": "Explanation for block queue"},
                    {"target": "Thermal & VM Parameters", "explanation": "Explanation for MediaTek thermals"}
                  ],
                  "profiles": [
                    {
                      "title": "🎮 Helio G85 Gaming Profile",
                      "description": "High clock lock for Cortex-A75 cores and Mali-G52 GPU with high ZRAM for games.",
                      "cpuGovernor": "performance",
                      "cpuMinFreqKHz": 1400000,
                      "cpuMaxFreqKHz": 2000000,
                      "gpuGovernor": "performance",
                      "gpuFreqHz": 950000000,
                      "zramSizeMb": 4096,
                      "ioScheduler": "mq-deadline",
                      "swappiness": 80
                    },
                    {
                      "title": "🔋 Helio G85 Battery Saver Profile",
                      "description": "Limits Cortex-A75 peak frequency and uses powersave GPU scaling for all-day battery.",
                      "cpuGovernor": "powersave",
                      "cpuMinFreqKHz": 500000,
                      "cpuMaxFreqKHz": 1400000,
                      "gpuGovernor": "powersave",
                      "gpuFreqHz": 400000000,
                      "zramSizeMb": 2048,
                      "ioScheduler": "none",
                      "swappiness": 40
                    },
                    {
                      "title": "⚖️ Helio G85 Balanced Profile",
                      "description": "Default schedutil governor with dynamic CorePilot scaling for smooth daily usage.",
                      "cpuGovernor": "schedutil",
                      "cpuMinFreqKHz": 800000,
                      "cpuMaxFreqKHz": 2000000,
                      "gpuGovernor": "simple_ondemand",
                      "gpuFreqHz": 800000000,
                      "zramSizeMb": 3072,
                      "ioScheduler": "mq-deadline",
                      "swappiness": 60
                    },
                    {
                      "title": "🚀 Helio G85 Maximum Performance Profile",
                      "description": "Unlocks maximum 2.0 GHz Cortex-A75 and 1000 MHz Mali-G52 GPU frequencies.",
                      "cpuGovernor": "performance",
                      "cpuMinFreqKHz": 2000000,
                      "cpuMaxFreqKHz": 2000000,
                      "gpuGovernor": "performance",
                      "gpuFreqHz": 1000000000,
                      "zramSizeMb": 4096,
                      "ioScheduler": "kyber",
                      "swappiness": 90
                    }
                  ],
                  "safetyNotes": "Safety instructions for thermal thresholds and sysfs verification."
                }
            """.trimIndent()

            val geminiResult = geminiService.generateOptimizationRecommendations(
                apiKey = apiKey,
                systemInstructionText = systemText,
                promptText = promptJson
            )

            geminiResult.fold(
                onSuccess = { responseText ->
                    val cleanedJson = cleanResponseJson(responseText)
                    val aiResult = analysisAdapter.fromJson(cleanedJson)
                    if (aiResult != null) {
                        Result.success(Pair(aiResult, true))
                    } else {
                        val fallback = generateOfflineHelioG85Analysis(
                            diagnostic,
                            "Failed to parse Gemini response. Generated via offline Helio G85 engine."
                        )
                        Result.success(Pair(fallback, false))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Gemini API request failed", error)
                    val fallback = generateOfflineHelioG85Analysis(
                        diagnostic,
                        "Gemini API error (${error.localizedMessage ?: "Network or Key error"}). Generated via offline MediaTek Helio G85 engine."
                    )
                    Result.success(Pair(fallback, false))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Repository exception", e)
            val fallback = generateOfflineHelioG85Analysis(
                diagnostic,
                "Gemini service error (${e.localizedMessage}). Generated via offline MediaTek Helio G85 engine."
            )
            Result.success(Pair(fallback, false))
        }
    }

    private fun cleanResponseJson(text: String): String {
        var trimmed = text.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        return trimmed.trim()
    }

    private fun buildPromptText(d: FullDeviceDiagnostic): String {
        return """
            Analyze MediaTek Helio G85 device telemetry:
            - Chipset: ${d.chipset} (${d.cpuArchitecture})
            - GPU: ${d.gpuModel}, Current Freq: ${d.gpuFreqHz / 1_000_000} MHz, Governor: ${d.gpuGovernor}
            - CPU Governor: ${d.cpuGovernor}, Online Cores: ${d.onlineCoresCount}/${d.totalCoresCount}, Min/Max: ${d.cpuMinFreqKHz/1000} - ${d.cpuMaxFreqKHz/1000} MHz
            - CPU Temp: ${d.cpuTempC}°C, Battery Temp: ${d.batteryDetail.temperatureC}°C, Thermal Status: ${d.thermalStatus}
            - CPU Load: ${d.cpuLoadPercentage}%
            - RAM: ${d.usedRamMb}MB / ${d.totalRamMb}MB (${d.ramUsedPercentage}%)
            - ZRAM: ${d.zramSizeMb}MB (Enabled: ${d.isZramEnabled}), Swap Used: ${d.swapUsedMb}MB / ${d.swapTotalMb}MB
            - Swappiness: ${d.vmParameters.swappiness}, Dirty Ratio: ${d.vmParameters.dirtyRatio}
            - I/O Scheduler: ${d.ioScheduler}, Read-Ahead: ${d.readAheadKb} KB, Device: ${d.targetBlockDevice}
            - Battery Health: ${d.batteryDetail.health}, Cycles: ${d.batteryDetail.cycles}, Current: ${d.batteryDetail.currentMa} mA (${d.batteryDetail.wattageW} W)
            - Android: ${d.androidVersion}, Kernel: ${d.kernelVersion}, Root: ${d.rootFramework}, SELinux: ${d.selinuxStatus}
            - Storage: ${d.storageFreeGb}GB free / ${d.storageTotalGb}GB total (${d.fileSystemType})
            - Modules: ${d.installedModules.joinToString(", ").ifBlank { "None" }}
            
            Provide optimal parameters for Helio G85 dual-cluster ARM setup (2x A75 + 6x A55) and 4 dynamic profiles.
        """.trimIndent()
    }

    private fun generateOfflineHelioG85Analysis(d: FullDeviceDiagnostic, message: String): AiAnalysisResult {
        val totalRamGb = (d.totalRamMb / 1024).toInt()
        val recommendedZram = when {
            totalRamGb <= 4 -> 3072
            totalRamGb <= 6 -> 4096
            else -> 4096
        }

        val isWarm = d.cpuTempC > 50f || d.batteryDetail.temperatureC > 42f

        return AiAnalysisResult(
            deviceOverview = "MediaTek Helio G85 state analyzed. 2x Cortex-A75 cores and 6x Cortex-A55 cores operating at ${d.cpuTempC}°C with ${d.ramUsedPercentage.toInt()}% RAM utilization (${d.usedRamMb}MB / ${d.totalRamMb}MB). $message",
            recommendations = AiRecommendations(
                cpuGovernor = "schedutil",
                cpuMinFreqMhz = 800,
                cpuMaxFreqMhz = 2000,
                gpuFreqMhz = if (isWarm) 800 else 950,
                zramSizeMb = recommendedZram,
                swappiness = 60,
                ioScheduler = if (d.availableIoSchedulers.contains("mq-deadline")) "mq-deadline" else d.ioScheduler,
                readAheadKb = 128,
                vmParameters = "dirty_ratio=20, dirty_background_ratio=10, vfs_cache_pressure=100",
                lmkdSettings = "Optimized for Helio G85 ${totalRamGb}GB RAM variant to prevent foreground app stutter."
            ),
            explanations = listOf(
                AiExplanation("CPU Cortex-A75 & A55 Scaling", "Schedutil dynamically balances performance across the 2x Cortex-A75 2.0GHz performance cluster and 6x Cortex-A55 1.8GHz efficiency cluster without causing rapid thermal throttling."),
                AiExplanation("Mali-G52 MC2 GPU", "Locking GPU frequency near 800-950 MHz prevents sudden frame drops in graphics-intensive games while preventing MediaTek thermal throttling."),
                AiExplanation("ZRAM & Swappiness (${recommendedZram}MB / 60)", "Configuring ZRAM size to ${recommendedZram}MB with swappiness=60 provides ample swap compressed buffer for background apps on ${d.totalRamMb}MB total physical RAM."),
                AiExplanation("I/O Queue & Read-Ahead (128KB)", "128KB read-ahead buffer maximizes sequential flash reads on internal eMMC 5.1/UFS storage."),
                AiExplanation("VM Parameters & LMKD", "Adjusting vfs_cache_pressure=100 and dirty_ratio=20 prevents write stalls during prolonged app usage.")
            ),
            profiles = listOf(
                HelioG85Profile(
                    title = "🎮 Helio G85 Gaming Profile",
                    description = "Prioritizes 2.0 GHz Cortex-A75 performance cores & Mali-G52 GPU for sustained high FPS in games.",
                    cpuGovernor = "performance",
                    cpuMinFreqKHz = 1400000L,
                    cpuMaxFreqKHz = 2000000L,
                    gpuGovernor = "performance",
                    gpuFreqHz = 950000000L,
                    zramSizeMb = 4096L,
                    ioScheduler = "mq-deadline",
                    swappiness = 80
                ),
                HelioG85Profile(
                    title = "🔋 Helio G85 Battery Saver Profile",
                    description = "Limits Cortex-A75 peak frequency to 1.4 GHz and reduces Mali-G52 clocks to extend battery life.",
                    cpuGovernor = "powersave",
                    cpuMinFreqKHz = 500000L,
                    cpuMaxFreqKHz = 1400000L,
                    gpuGovernor = "powersave",
                    gpuFreqHz = 400000000L,
                    zramSizeMb = 2048L,
                    ioScheduler = "none",
                    swappiness = 40
                ),
                HelioG85Profile(
                    title = "⚖️ Helio G85 Balanced Profile",
                    description = "Optimal everyday profile using schedutil governor and 3GB ZRAM for fluid multitasking.",
                    cpuGovernor = "schedutil",
                    cpuMinFreqKHz = 800000L,
                    cpuMaxFreqKHz = 2000000L,
                    gpuGovernor = "simple_ondemand",
                    gpuFreqHz = 800000000L,
                    zramSizeMb = 3072L,
                    ioScheduler = "mq-deadline",
                    swappiness = 60
                ),
                HelioG85Profile(
                    title = "🚀 Helio G85 Maximum Performance Profile",
                    description = "Maxes out all 8 CPU cores at 2.0 GHz and unlocks Mali-G52 GPU to 1000 MHz maximum frequency.",
                    cpuGovernor = "performance",
                    cpuMinFreqKHz = 2000000L,
                    cpuMaxFreqKHz = 2000000L,
                    gpuGovernor = "performance",
                    gpuFreqHz = 1000000000L,
                    zramSizeMb = 4096L,
                    ioScheduler = "kyber",
                    swappiness = 90
                )
            ),
            safetyNotes = "Verify that governor and frequency sysfs nodes exist on your kernel before applying. Always monitor CPU temperatures under heavy load."
        )
    }
}
