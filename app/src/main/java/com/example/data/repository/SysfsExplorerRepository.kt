package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.SysfsNodeAiExplanation
import com.example.data.model.SysfsNodeItem
import com.example.data.remote.GeminiService
import com.example.root.RootShell
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SysfsExplorerRepository(
    private val context: Context,
    private val geminiService: GeminiService = GeminiService()
) {
    private val TAG = "SysfsExplorerRepository"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val explanationAdapter = moshi.adapter(SysfsNodeAiExplanation::class.java)

    suspend fun listDirectory(path: String): List<SysfsNodeItem> = withContext(Dispatchers.IO) {
        val target = File(path)
        if (!target.exists() || !target.isDirectory) return@withContext emptyList()

        val items = mutableListOf<SysfsNodeItem>()
        val files = target.listFiles() ?: emptyArray()

        for (f in files) {
            val isDir = f.isDirectory
            var isWritable = f.canWrite()
            var isReadable = f.canRead()
            var currentValue = ""

            if (!isDir && isReadable) {
                currentValue = RootShell.readSysfsNode(f.absolutePath).trim()
            }

            // Check root writability for sysfs
            if (!isWritable && RootShell.isRootAvailable()) {
                isWritable = true // su root can edit chmod 644
            }

            items.add(
                SysfsNodeItem(
                    path = f.absolutePath,
                    name = f.name,
                    isDirectory = isDir,
                    isWritable = isWritable,
                    isReadable = isReadable,
                    currentValue = currentValue
                )
            )
        }

        items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun readNodeValue(path: String): String = withContext(Dispatchers.IO) {
        RootShell.readSysfsNode(path).trim()
    }

    suspend fun writeNodeValue(path: String, value: String): Boolean = withContext(Dispatchers.IO) {
        val res = RootShell.writeSysfsNode(path, value)
        res.isSuccess
    }

    suspend fun getAiNodeExplanation(
        nodePath: String,
        currentValue: String,
        customApiKey: String? = null
    ): Result<SysfsNodeAiExplanation> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null } ?: try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isKeyValid = !apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null"

        val systemPrompt = """
            You are an Android Kernel & SysFS expert.
            Explain the requested SysFS node in JSON strictly matching:
            {
              "path": "$nodePath",
              "whatItControls": "Clear explanation of what this sysfs kernel node does",
              "isSafeToEdit": true,
              "performanceImpact": "How modifying this affects CPU/GPU/UI performance",
              "batteryImpact": "How modifying this affects battery consumption",
              "temperatureImpact": "How modifying this affects SoC thermals",
              "possibleRisks": "Potential kernel panics, bootloops, or instability if improper value is set"
            }
        """.trimIndent()

        val userPrompt = "Sysfs Node Path: $nodePath\nCurrent Value: $currentValue"

        if (!isKeyValid) {
            return@withContext Result.success(generateOfflineNodeExplanation(nodePath, currentValue))
        }

        val res = geminiService.generateOptimizationRecommendations(
            apiKey = apiKey,
            systemInstructionText = systemPrompt,
            promptText = userPrompt
        )

        res.fold(
            onSuccess = { responseText ->
                var trimmed = responseText.trim()
                if (trimmed.startsWith("```json")) trimmed = trimmed.removePrefix("```json")
                else if (trimmed.startsWith("```")) trimmed = trimmed.removePrefix("```")
                if (trimmed.endsWith("```")) trimmed = trimmed.removeSuffix("```")

                val parsed = explanationAdapter.fromJson(trimmed.trim())
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.success(generateOfflineNodeExplanation(nodePath, currentValue))
                }
            },
            onFailure = { err ->
                Log.e(TAG, "Gemini SysFS explanation failed", err)
                Result.success(generateOfflineNodeExplanation(nodePath, currentValue))
            }
        )
    }

    private fun generateOfflineNodeExplanation(path: String, value: String): SysfsNodeAiExplanation {
        val isDangerous = path.contains("scaling_governor") || path.contains("voltage") || path.contains("frequency")
        return SysfsNodeAiExplanation(
            path = path,
            whatItControls = "SysFS entry '$path' exposes Linux kernel runtime parameters directly to userspace.",
            isSafeToEdit = !isDangerous,
            performanceImpact = "Modifying frequencies or governors directly impacts CPU clock speed scaling.",
            batteryImpact = "Higher active clock speeds increase overall mAh power draw.",
            temperatureImpact = "Increased CPU load elevates thermal zone temperatures.",
            possibleRisks = if (isDangerous) "Setting unsupported values may lead to system freezes or thermal throttling." else "Safe for standard runtime adjustments."
        )
    }
}
