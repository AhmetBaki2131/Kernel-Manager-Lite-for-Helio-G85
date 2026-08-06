package com.example.data.repository

import android.content.Context
import com.example.data.local.LogDao
import com.example.data.local.ProfileDao
import com.example.data.model.*
import com.example.root.CommandResult
import com.example.root.RootShell
import com.example.root.SysfsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class KernelRepository(
    private val context: Context,
    private val logDao: LogDao,
    private val profileDao: ProfileDao
) {

    suspend fun applyProfile(profile: ProfileEntity): CommandResult = withContext(Dispatchers.IO) {
        val actionName = "Apply Profile: ${profile.name}"
        val results = mutableListOf<String>()
        var overallSuccess = true

        // Verify root first
        if (!RootShell.isRootAvailable()) {
            val msg = "Root access not granted. Cannot apply profile."
            logDao.insertLog(LogEntity(actionName = actionName, commandRun = "isRootAvailable", success = false, message = msg))
            return@withContext CommandResult(-1, emptyList(), listOf(msg))
        }

        // Apply CPU Governor if provided
        if (profile.cpuGovernor.isNotBlank() && profile.cpuGovernor != "N/A") {
            val res = SysfsHelper.setCpuGovernor(profile.cpuGovernor)
            if (res.isSuccess) results.add("CPU Gov -> ${profile.cpuGovernor}")
            else { overallSuccess = false; results.add("CPU Gov Failed: ${res.outputText}") }
        }

        // Apply CPU Min Freq
        if (profile.cpuMinFreqKHz > 0) {
            val res = SysfsHelper.setCpuMinFreq(profile.cpuMinFreqKHz)
            if (res.isSuccess) results.add("CPU Min Freq -> ${profile.cpuMinFreqKHz / 1000}MHz")
            else { overallSuccess = false; results.add("CPU Min Freq Failed: ${res.outputText}") }
        }

        // Apply CPU Max Freq
        if (profile.cpuMaxFreqKHz > 0) {
            val res = SysfsHelper.setCpuMaxFreq(profile.cpuMaxFreqKHz)
            if (res.isSuccess) results.add("CPU Max Freq -> ${profile.cpuMaxFreqKHz / 1000}MHz")
            else { overallSuccess = false; results.add("CPU Max Freq Failed: ${res.outputText}") }
        }

        // Apply GPU Governor
        val gpuInfo = SysfsHelper.getGpuInfo()
        if (gpuInfo.isSupported && profile.gpuGovernor.isNotBlank() && profile.gpuGovernor != "N/A") {
            val res = SysfsHelper.setGpuGovernor(gpuInfo.path, profile.gpuGovernor)
            if (res.isSuccess) results.add("GPU Gov -> ${profile.gpuGovernor}")
        }

        // Apply GPU Max Freq
        if (gpuInfo.isSupported && profile.gpuFreqHz > 0) {
            val res = SysfsHelper.setGpuMaxFreq(gpuInfo.path, profile.gpuFreqHz)
            if (res.isSuccess) results.add("GPU Freq -> ${profile.gpuFreqHz / 1_000_000}MHz")
        }

        // Apply ZRAM
        val zramInfo = SysfsHelper.getZramInfo()
        if (zramInfo.isSupported && profile.zramSizeMb > 0) {
            if (profile.zramSizeMb != zramInfo.diskSizeMb) {
                val res = SysfsHelper.resizeZram(profile.zramSizeMb)
                if (res.isSuccess) results.add("ZRAM Size -> ${profile.zramSizeMb}MB")
            }
        }

        // Apply I/O Scheduler
        val ioInfo = SysfsHelper.getIoSchedulerInfo()
        if (ioInfo.isSupported && profile.ioScheduler.isNotBlank()) {
            val res = SysfsHelper.setIoScheduler(profile.ioScheduler)
            if (res.isSuccess) results.add("I/O Scheduler -> ${profile.ioScheduler}")
        }

        val logMsg = results.joinToString(" | ")
        logDao.insertLog(
            LogEntity(
                actionName = actionName,
                commandRun = "applyProfile(${profile.name})",
                success = overallSuccess,
                message = logMsg.ifBlank { "Profile applied" }
            )
        )

        CommandResult(
            exitCode = if (overallSuccess) 0 else 1,
            stdout = results,
            stderr = if (overallSuccess) emptyList() else listOf("Some settings failed to apply")
        )
    }

    suspend fun applyCpuGovernor(governor: String): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setCpuGovernor(governor)
        logDao.insertLog(LogEntity(actionName = "CPU Governor", commandRun = "setCpuGovernor($governor)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyCpuMinFreq(freqKHz: Long): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setCpuMinFreq(freqKHz)
        logDao.insertLog(LogEntity(actionName = "CPU Min Freq", commandRun = "setCpuMinFreq($freqKHz)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyCpuMaxFreq(freqKHz: Long): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setCpuMaxFreq(freqKHz)
        logDao.insertLog(LogEntity(actionName = "CPU Max Freq", commandRun = "setCpuMaxFreq($freqKHz)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyCpuCoreOnline(coreId: Int, online: Boolean): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setCpuCoreOnline(coreId, online)
        logDao.insertLog(LogEntity(actionName = "CPU Core $coreId State", commandRun = "setCpuCoreOnline($coreId, $online)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyGpuGovernor(gpuPath: String, governor: String): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setGpuGovernor(gpuPath, governor)
        logDao.insertLog(LogEntity(actionName = "GPU Governor", commandRun = "setGpuGovernor($governor)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyGpuMaxFreq(gpuPath: String, maxFreqHz: Long): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setGpuMaxFreq(gpuPath, maxFreqHz)
        logDao.insertLog(LogEntity(actionName = "GPU Max Freq", commandRun = "setGpuMaxFreq($maxFreqHz)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyZramSize(sizeMb: Long): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.resizeZram(sizeMb)
        logDao.insertLog(LogEntity(actionName = "ZRAM Resize", commandRun = "resizeZram($sizeMb)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyZramState(enable: Boolean): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.toggleZram(enable)
        logDao.insertLog(LogEntity(actionName = "ZRAM Toggle", commandRun = "toggleZram($enable)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun applyIoScheduler(scheduler: String): CommandResult = withContext(Dispatchers.IO) {
        val res = SysfsHelper.setIoScheduler(scheduler)
        logDao.insertLog(LogEntity(actionName = "I/O Scheduler", commandRun = "setIoScheduler($scheduler)", success = res.isSuccess, message = res.outputText))
        res
    }

    suspend fun exportProfilesToJson(profiles: List<ProfileEntity>): String = withContext(Dispatchers.Default) {
        val array = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("name", p.name)
                put("description", p.description)
                put("cpuGovernor", p.cpuGovernor)
                put("cpuMinFreqKHz", p.cpuMinFreqKHz)
                put("cpuMaxFreqKHz", p.cpuMaxFreqKHz)
                put("gpuGovernor", p.gpuGovernor)
                put("gpuFreqHz", p.gpuFreqHz)
                put("zramSizeMb", p.zramSizeMb)
                put("ioScheduler", p.ioScheduler)
            }
            array.put(obj)
        }
        array.toString(2)
    }

    suspend fun importProfilesFromJson(jsonStr: String): Int = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray(jsonStr)
            var count = 0
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val profile = ProfileEntity(
                    name = obj.optString("name", "Imported Profile"),
                    description = obj.optString("description", "Custom imported profile"),
                    isBuiltIn = false,
                    cpuGovernor = obj.optString("cpuGovernor", "schedutil"),
                    cpuMinFreqKHz = obj.optLong("cpuMinFreqKHz", 0L),
                    cpuMaxFreqKHz = obj.optLong("cpuMaxFreqKHz", 0L),
                    gpuGovernor = obj.optString("gpuGovernor", "msm-adreno-tz"),
                    gpuFreqHz = obj.optLong("gpuFreqHz", 0L),
                    zramSizeMb = obj.optLong("zramSizeMb", 2048L),
                    ioScheduler = obj.optString("ioScheduler", "mq-deadline")
                )
                profileDao.insertProfile(profile)
                count++
            }
            count
        } catch (e: Exception) {
            0
        }
    }

    suspend fun applyHelioG85Profile(profile: HelioG85Profile): CommandResult = withContext(Dispatchers.IO) {
        val actionName = "Apply AI Profile: ${profile.title}"
        val results = mutableListOf<String>()
        var overallSuccess = true

        if (!RootShell.isRootAvailable()) {
            val msg = "Root access not granted. Cannot apply AI profile."
            logDao.insertLog(LogEntity(actionName = actionName, commandRun = "isRootAvailable", success = false, message = msg))
            return@withContext CommandResult(-1, emptyList(), listOf(msg))
        }

        // Apply CPU Governor
        if (profile.cpuGovernor.isNotBlank() && profile.cpuGovernor != "N/A") {
            val res = SysfsHelper.setCpuGovernor(profile.cpuGovernor)
            if (res.isSuccess) results.add("CPU Governor -> ${profile.cpuGovernor}")
            else { overallSuccess = false; results.add("CPU Gov Failed") }
        }

        // Apply CPU Min/Max Frequencies
        if (profile.cpuMinFreqKHz > 0) {
            val res = SysfsHelper.setCpuMinFreq(profile.cpuMinFreqKHz)
            if (res.isSuccess) results.add("CPU Min -> ${profile.cpuMinFreqKHz / 1000}MHz")
        }
        if (profile.cpuMaxFreqKHz > 0) {
            val res = SysfsHelper.setCpuMaxFreq(profile.cpuMaxFreqKHz)
            if (res.isSuccess) results.add("CPU Max -> ${profile.cpuMaxFreqKHz / 1000}MHz")
        }

        // Apply GPU
        val gpuInfo = SysfsHelper.getGpuInfo()
        if (gpuInfo.isSupported) {
            if (profile.gpuGovernor.isNotBlank() && profile.gpuGovernor != "N/A") {
                SysfsHelper.setGpuGovernor(gpuInfo.path, profile.gpuGovernor)
            }
            if (profile.gpuFreqHz > 0) {
                SysfsHelper.setGpuMaxFreq(gpuInfo.path, profile.gpuFreqHz)
                results.add("GPU Freq -> ${profile.gpuFreqHz / 1_000_000}MHz")
            }
        }

        // Apply ZRAM
        if (profile.zramSizeMb > 0) {
            SysfsHelper.resizeZram(profile.zramSizeMb)
            results.add("ZRAM -> ${profile.zramSizeMb}MB")
        }

        // Apply Swappiness
        if (profile.swappiness in 0..100) {
            RootShell.writeSysfsNode("/proc/sys/vm/swappiness", profile.swappiness.toString())
            results.add("Swappiness -> ${profile.swappiness}")
        }

        // Apply I/O Scheduler
        if (profile.ioScheduler.isNotBlank()) {
            SysfsHelper.setIoScheduler(profile.ioScheduler)
            results.add("I/O Scheduler -> ${profile.ioScheduler}")
        }

        val logMsg = results.joinToString(" | ")
        logDao.insertLog(LogEntity(actionName = actionName, commandRun = "applyHelioG85Profile", success = overallSuccess, message = logMsg))

        CommandResult(
            exitCode = if (overallSuccess) 0 else 1,
            stdout = results,
            stderr = if (overallSuccess) emptyList() else listOf("Some profile settings failed to apply")
        )
    }

    suspend fun applyAiRecommendations(recs: AiRecommendations): CommandResult = withContext(Dispatchers.IO) {
        val actionName = "Apply AI Recommendations"
        val results = mutableListOf<String>()
        var overallSuccess = true

        if (!RootShell.isRootAvailable()) {
            val msg = "Root access not granted. Cannot apply recommendations."
            logDao.insertLog(LogEntity(actionName = actionName, commandRun = "isRootAvailable", success = false, message = msg))
            return@withContext CommandResult(-1, emptyList(), listOf(msg))
        }

        // CPU Governor
        if (recs.cpuGovernor.isNotBlank()) {
            val res = SysfsHelper.setCpuGovernor(recs.cpuGovernor)
            if (res.isSuccess) results.add("CPU Gov -> ${recs.cpuGovernor}")
        }

        // CPU Min/Max
        if (recs.cpuMinFreqMhz > 0) {
            SysfsHelper.setCpuMinFreq(recs.cpuMinFreqMhz * 1000L)
            results.add("CPU Min -> ${recs.cpuMinFreqMhz}MHz")
        }
        if (recs.cpuMaxFreqMhz > 0) {
            SysfsHelper.setCpuMaxFreq(recs.cpuMaxFreqMhz * 1000L)
            results.add("CPU Max -> ${recs.cpuMaxFreqMhz}MHz")
        }

        // GPU Max Freq
        val gpuInfo = SysfsHelper.getGpuInfo()
        if (gpuInfo.isSupported && recs.gpuFreqMhz > 0) {
            SysfsHelper.setGpuMaxFreq(gpuInfo.path, recs.gpuFreqMhz * 1_000_000L)
            results.add("GPU Freq -> ${recs.gpuFreqMhz}MHz")
        }

        // ZRAM Size
        if (recs.zramSizeMb > 0) {
            SysfsHelper.resizeZram(recs.zramSizeMb.toLong())
            results.add("ZRAM -> ${recs.zramSizeMb}MB")
        }

        // Swappiness
        if (recs.swappiness in 0..100) {
            RootShell.writeSysfsNode("/proc/sys/vm/swappiness", recs.swappiness.toString())
            results.add("Swappiness -> ${recs.swappiness}")
        }

        // I/O Scheduler
        if (recs.ioScheduler.isNotBlank()) {
            SysfsHelper.setIoScheduler(recs.ioScheduler)
            results.add("I/O -> ${recs.ioScheduler}")
        }

        // Read Ahead Buffer
        if (recs.readAheadKb > 0) {
            val ioInfo = SysfsHelper.getIoSchedulerInfo()
            if (ioInfo.isSupported && ioInfo.targetBlockDevice.isNotBlank()) {
                RootShell.writeSysfsNode("/sys/block/${ioInfo.targetBlockDevice}/queue/read_ahead_kb", recs.readAheadKb.toString())
                results.add("ReadAhead -> ${recs.readAheadKb}KB")
            }
        }

        val logMsg = results.joinToString(" | ")
        logDao.insertLog(LogEntity(actionName = actionName, commandRun = "applyAiRecommendations", success = overallSuccess, message = logMsg))

        CommandResult(
            exitCode = if (overallSuccess) 0 else 1,
            stdout = results,
            stderr = if (overallSuccess) emptyList() else listOf("Some recommendations failed to apply")
        )
    }
}

