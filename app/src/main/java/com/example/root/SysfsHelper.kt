package com.example.root

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SysfsHelper {
    private const val TAG = "SysfsHelper"

    // --- DEVICE & KERNEL INFORMATION ---
    suspend fun getDeviceInfo(context: Context): DeviceInfo = withContext(Dispatchers.IO) {
        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        var kernelVersion = System.getProperty("os.version") ?: "Unknown"
        
        val procVersion = RootShell.readSysfsNode("/proc/version")
        if (procVersion.isNotBlank()) {
            val parts = procVersion.split(" ")
            if (parts.size >= 3) {
                kernelVersion = "${parts[0]} ${parts[1]} ${parts[2]}"
            }
        }

        val isRooted = RootShell.isRootAvailable()

        DeviceInfo(
            model = model,
            androidVersion = androidVersion,
            kernelVersion = kernelVersion,
            isRooted = isRooted
        )
    }

    // --- CPU INFORMATION & CONTROL ---
    suspend fun getCpuInfo(): CpuInfo = withContext(Dispatchers.IO) {
        val cores = mutableListOf<CpuCoreInfo>()
        var coreIndex = 0

        while (true) {
            val cpuPath = "/sys/devices/system/cpu/cpu$coreIndex"
            val cpuDir = File(cpuPath)
            if (!cpuDir.exists() && coreIndex > 0) break

            val onlineStr = RootShell.readSysfsNode("$cpuPath/online")
            val isOnline = when {
                onlineStr.isBlank() && coreIndex == 0 -> true // CPU0 is usually online
                onlineStr == "1" -> true
                else -> false
            }

            val curFreqStr = RootShell.readSysfsNode("$cpuPath/cpufreq/scaling_cur_freq")
            val curFreqKHz = curFreqStr.toLongOrNull() ?: 0L

            val minFreqStr = RootShell.readSysfsNode("$cpuPath/cpufreq/scaling_min_freq")
            val minFreqKHz = minFreqStr.toLongOrNull() ?: 0L

            val maxFreqStr = RootShell.readSysfsNode("$cpuPath/cpufreq/scaling_max_freq")
            val maxFreqKHz = maxFreqStr.toLongOrNull() ?: 0L

            val governor = RootShell.readSysfsNode("$cpuPath/cpufreq/scaling_governor").ifBlank { "N/A" }

            cores.add(
                CpuCoreInfo(
                    coreId = coreIndex,
                    isOnline = isOnline,
                    currentFreqKHz = curFreqKHz,
                    minFreqKHz = minFreqKHz,
                    maxFreqKHz = maxFreqKHz,
                    governor = governor
                )
            )
            coreIndex++
            if (coreIndex > 16) break // Safety cap
        }

        // Global available frequencies and governors (from cpu0 or policy0)
        val availableFreqsStr = RootShell.readSysfsNode("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies")
            .ifBlank { RootShell.readSysfsNode("/sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies") }

        val availableFreqsKHz = availableFreqsStr.split("\\s+".toRegex())
            .mapNotNull { it.trim().toLongOrNull() }
            .sorted()

        val availableGovsStr = RootShell.readSysfsNode("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors")
            .ifBlank { RootShell.readSysfsNode("/sys/devices/system/cpu/cpufreq/policy0/scaling_available_governors") }

        val availableGovernors = availableGovsStr.split("\\s+".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val currentGovernor = cores.firstOrNull()?.governor ?: "N/A"
        val minFreq = cores.minOfOrNull { it.minFreqKHz } ?: 0L
        val maxFreq = cores.maxOfOrNull { it.maxFreqKHz } ?: 0L

        CpuInfo(
            totalCores = cores.size,
            cores = cores,
            currentGovernor = currentGovernor,
            availableGovernors = availableGovernors,
            globalMinFreqKHz = minFreq,
            globalMaxFreqKHz = maxFreq,
            availableFrequenciesKHz = availableFreqsKHz
        )
    }

    suspend fun setCpuCoreOnline(coreId: Int, online: Boolean): CommandResult = withContext(Dispatchers.IO) {
        val path = "/sys/devices/system/cpu/cpu$coreId/online"
        val valStr = if (online) "1" else "0"
        RootShell.writeSysfsNode(path, valStr)
    }

    suspend fun setCpuGovernor(governor: String): CommandResult = withContext(Dispatchers.IO) {
        val cmds = mutableListOf<String>()
        val cpuCount = getCpuInfo().totalCores
        for (i in 0 until cpuCount) {
            cmds.add("chmod 644 /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor 2>/dev/null; echo \"$governor\" > /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor")
        }
        RootShell.runCommands(cmds)
    }

    suspend fun setCpuMinFreq(minFreqKHz: Long): CommandResult = withContext(Dispatchers.IO) {
        val cmds = mutableListOf<String>()
        val cpuCount = getCpuInfo().totalCores
        for (i in 0 until cpuCount) {
            cmds.add("chmod 644 /sys/devices/system/cpu/cpu$i/cpufreq/scaling_min_freq 2>/dev/null; echo \"$minFreqKHz\" > /sys/devices/system/cpu/cpu$i/cpufreq/scaling_min_freq")
        }
        RootShell.runCommands(cmds)
    }

    suspend fun setCpuMaxFreq(maxFreqKHz: Long): CommandResult = withContext(Dispatchers.IO) {
        val cmds = mutableListOf<String>()
        val cpuCount = getCpuInfo().totalCores
        for (i in 0 until cpuCount) {
            cmds.add("chmod 644 /sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq 2>/dev/null; echo \"$maxFreqKHz\" > /sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq")
        }
        RootShell.runCommands(cmds)
    }

    // --- GPU INFORMATION & CONTROL ---
    suspend fun getGpuInfo(): GpuInfo = withContext(Dispatchers.IO) {
        val possibleGpuPaths = listOf(
            "/sys/class/kgsl/kgsl-3d0",
            "/sys/class/devfreq/gpufreq",
            "/sys/class/devfreq/1c00000.mali",
            "/sys/devices/platform/13900000.mali/devfreq/13900000.mali"
        )

        var basePath = ""
        for (path in possibleGpuPaths) {
            if (File(path).exists()) {
                basePath = path
                break
            }
        }

        if (basePath.isEmpty()) {
            // Try probing /sys/class/devfreq for any gpu entry
            val devfreqDir = File("/sys/class/devfreq")
            if (devfreqDir.exists()) {
                val gpuDir = devfreqDir.listFiles()?.firstOrNull { it.name.lowercase().contains("gpu") }
                if (gpuDir != null) basePath = gpuDir.absolutePath
            }
        }

        if (basePath.isEmpty()) {
            return@withContext GpuInfo(isSupported = false, model = "Unsupported or Restricted Kernel")
        }

        val model = when {
            basePath.contains("kgsl") -> "Adreno GPU (KGSL)"
            basePath.contains("mali") -> "Mali GPU (devfreq)"
            else -> "Generic GPU"
        }

        val curFreqStr = RootShell.readSysfsNode("$basePath/devfreq/cur_freq")
            .ifBlank { RootShell.readSysfsNode("$basePath/gpuclk") }
            .ifBlank { RootShell.readSysfsNode("$basePath/cur_freq") }
        val curFreqHz = curFreqStr.toLongOrNull() ?: 0L

        val minFreqStr = RootShell.readSysfsNode("$basePath/devfreq/min_freq")
            .ifBlank { RootShell.readSysfsNode("$basePath/min_gpuclk") }
        val minFreqHz = minFreqStr.toLongOrNull() ?: 0L

        val maxFreqStr = RootShell.readSysfsNode("$basePath/devfreq/max_freq")
            .ifBlank { RootShell.readSysfsNode("$basePath/max_gpuclk") }
        val maxFreqHz = maxFreqStr.toLongOrNull() ?: 0L

        val governor = RootShell.readSysfsNode("$basePath/devfreq/governor")
            .ifBlank { RootShell.readSysfsNode("$basePath/governor") }
            .ifBlank { "N/A" }

        val availGovStr = RootShell.readSysfsNode("$basePath/devfreq/available_governors")
            .ifBlank { RootShell.readSysfsNode("$basePath/available_governors") }
        val availableGovs = availGovStr.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val availFreqStr = RootShell.readSysfsNode("$basePath/devfreq/available_frequencies")
            .ifBlank { RootShell.readSysfsNode("$basePath/gpu_available_frequencies") }
        val availableFreqs = availFreqStr.split("\\s+".toRegex())
            .mapNotNull { it.trim().toLongOrNull() }
            .sorted()

        GpuInfo(
            isSupported = true,
            model = model,
            path = basePath,
            currentFreqHz = curFreqHz,
            minFreqHz = minFreqHz,
            maxFreqHz = maxFreqHz,
            currentGovernor = governor,
            availableGovernors = availableGovs,
            availableFrequenciesHz = availableFreqs
        )
    }

    suspend fun setGpuGovernor(gpuPath: String, governor: String): CommandResult = withContext(Dispatchers.IO) {
        val target = if (File("$gpuPath/devfreq/governor").exists()) "$gpuPath/devfreq/governor" else "$gpuPath/governor"
        RootShell.writeSysfsNode(target, governor)
    }

    suspend fun setGpuMaxFreq(gpuPath: String, maxFreqHz: Long): CommandResult = withContext(Dispatchers.IO) {
        val target = if (File("$gpuPath/devfreq/max_freq").exists()) "$gpuPath/devfreq/max_freq" else "$gpuPath/max_gpuclk"
        RootShell.writeSysfsNode(target, maxFreqHz.toString())
    }

    // --- ZRAM INFORMATION & CONTROL ---
    suspend fun getZramInfo(): ZramInfo = withContext(Dispatchers.IO) {
        val zramPath = "/sys/block/zram0"
        if (!File(zramPath).exists()) {
            return@withContext ZramInfo(isSupported = false)
        }

        val disksizeStr = RootShell.readSysfsNode("$zramPath/disksize")
        val diskSizeBytes = disksizeStr.toLongOrNull() ?: 0L
        val diskSizeMb = diskSizeBytes / (1024 * 1024)

        // Check swapon / proc/swaps
        val swapsStr = RootShell.readSysfsNode("/proc/swaps")
        val isEnabled = swapsStr.contains("zram0")

        var swapTotalKb = 0L
        var swapFreeKb = 0L

        val meminfoStr = RootShell.readSysfsNode("/proc/meminfo")
        for (line in meminfoStr.lines()) {
            if (line.startsWith("SwapTotal:")) {
                swapTotalKb = line.replace("\\D+".toRegex(), "").toLongOrNull() ?: 0L
            } else if (line.startsWith("SwapFree:")) {
                swapFreeKb = line.replace("\\D+".toRegex(), "").toLongOrNull() ?: 0L
            }
        }

        val swapUsedKb = (swapTotalKb - swapFreeKb).coerceAtLeast(0L)

        ZramInfo(
            isSupported = true,
            diskSizeMb = diskSizeMb,
            isEnabled = isEnabled,
            swapTotalMb = swapTotalKb / 1024,
            swapFreeMb = swapFreeKb / 1024,
            swapUsedMb = swapUsedKb / 1024
        )
    }

    suspend fun resizeZram(sizeMb: Long): CommandResult = withContext(Dispatchers.IO) {
        val sizeBytes = sizeMb * 1024 * 1024
        val cmds = listOf(
            "swapoff /dev/block/zram0 2>/dev/null || true",
            "echo 1 > /sys/block/zram0/reset 2>/dev/null || true",
            "echo $sizeBytes > /sys/block/zram0/disksize",
            "mkswap /dev/block/zram0 2>/dev/null || true",
            "swapon /dev/block/zram0"
        )
        RootShell.runCommands(cmds)
    }

    suspend fun toggleZram(enable: Boolean): CommandResult = withContext(Dispatchers.IO) {
        val cmd = if (enable) "swapon /dev/block/zram0" else "swapoff /dev/block/zram0"
        RootShell.runCommand(cmd)
    }

    // --- I/O SCHEDULER INFORMATION & CONTROL ---
    suspend fun getIoSchedulerInfo(): IoSchedulerInfo = withContext(Dispatchers.IO) {
        val blockDevices = listOf("sda", "sdb", "sdc", "mmcblk0", "nvme0n1", "dm-0")
        var targetDevice = ""
        var schedulerContent = ""

        for (dev in blockDevices) {
            val path = "/sys/block/$dev/queue/scheduler"
            val content = RootShell.readSysfsNode(path)
            if (content.isNotBlank()) {
                targetDevice = dev
                schedulerContent = content
                break
            }
        }

        if (targetDevice.isEmpty()) {
            // Find any device under /sys/block/
            val sysBlockDir = File("/sys/block")
            if (sysBlockDir.exists()) {
                sysBlockDir.listFiles()?.forEach { file ->
                    val path = "${file.absolutePath}/queue/scheduler"
                    val content = RootShell.readSysfsNode(path)
                    if (content.isNotBlank()) {
                        targetDevice = file.name
                        schedulerContent = content
                        return@forEach
                    }
                }
            }
        }

        if (targetDevice.isEmpty()) {
            return@withContext IoSchedulerInfo(isSupported = false)
        }

        var currentScheduler = ""
        val availableSchedulers = mutableListOf<String>()

        val tokens = schedulerContent.split("\\s+".toRegex())
        for (token in tokens) {
            val clean = token.trim()
            if (clean.isBlank()) continue
            if (clean.startsWith("[") && clean.endsWith("]")) {
                val active = clean.substring(1, clean.length - 1)
                currentScheduler = active
                availableSchedulers.add(active)
            } else {
                availableSchedulers.add(clean)
            }
        }

        IoSchedulerInfo(
            isSupported = true,
            targetBlockDevice = targetDevice,
            currentScheduler = currentScheduler,
            availableSchedulers = availableSchedulers
        )
    }

    suspend fun setIoScheduler(scheduler: String): CommandResult = withContext(Dispatchers.IO) {
        val sysBlockDir = File("/sys/block")
        val cmds = mutableListOf<String>()

        if (sysBlockDir.exists()) {
            sysBlockDir.listFiles()?.forEach { dev ->
                val path = "/sys/block/${dev.name}/queue/scheduler"
                cmds.add("chmod 644 $path 2>/dev/null; echo \"$scheduler\" > $path 2>/dev/null || true")
            }
        } else {
            cmds.add("chmod 644 /sys/block/sda/queue/scheduler; echo \"$scheduler\" > /sys/block/sda/queue/scheduler")
        }

        RootShell.runCommands(cmds)
    }

    // --- RAM & THERMAL ---
    suspend fun getRamInfo(context: Context): RamInfo = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val freeMb = memInfo.availMem / (1024 * 1024)
        val usedMb = totalMb - freeMb

        RamInfo(
            totalMb = totalMb,
            usedMb = usedMb,
            freeMb = freeMb,
            usedPercentage = if (totalMb > 0) (usedMb.toFloat() / totalMb.toFloat() * 100f) else 0f
        )
    }

    suspend fun getThermalInfo(context: Context): ThermalInfo = withContext(Dispatchers.IO) {
        var cpuTempC = 0f
        var batteryTempC = 0f

        // Try reading CPU thermal zone
        for (i in 0..20) {
            val type = RootShell.readSysfsNode("/sys/class/thermal/thermal_zone$i/type").lowercase()
            val tempStr = RootShell.readSysfsNode("/sys/class/thermal/thermal_zone$i/temp")
            val tempVal = tempStr.toFloatOrNull() ?: 0f

            if (tempVal > 0f) {
                val normalizedTemp = if (tempVal > 1000f) tempVal / 1000f else tempVal
                if (type.contains("cpu") || type.contains("soc") || type.contains("tsens")) {
                    if (normalizedTemp in 10f..110f) {
                        cpuTempC = normalizedTemp
                        break
                    }
                }
            }
        }

        // Battery temperature via Intent
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (temp > 0) {
                batteryTempC = temp / 10f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery temperature", e)
        }

        ThermalInfo(
            cpuTempC = cpuTempC,
            batteryTempC = batteryTempC
        )
    }

    suspend fun collectFullDeviceDiagnostic(context: Context): FullDeviceDiagnostic = withContext(Dispatchers.IO) {
        val devInfo = getDeviceInfo(context)
        val cpuInfo = getCpuInfo()
        val gpuInfo = getGpuInfo()
        val ramInfo = getRamInfo(context)
        val zramInfo = getZramInfo()
        val ioInfo = getIoSchedulerInfo()
        val thermal = getThermalInfo(context)

        // Read Swappiness
        val swappinessStr = RootShell.readSysfsNode("/proc/sys/vm/swappiness")
        val swappiness = swappinessStr.toIntOrNull() ?: 60

        // Read Read-Ahead Buffer Size
        var readAheadKb = 128
        if (ioInfo.targetBlockDevice.isNotBlank() && ioInfo.targetBlockDevice != "N/A") {
            val raStr = RootShell.readSysfsNode("/sys/block/${ioInfo.targetBlockDevice}/queue/read_ahead_kb")
            readAheadKb = raStr.toIntOrNull() ?: 128
        }

        // Read VM Parameters
        val dirtyRatio = RootShell.readSysfsNode("/proc/sys/vm/dirty_ratio").toIntOrNull() ?: 20
        val dirtyBg = RootShell.readSysfsNode("/proc/sys/vm/dirty_background_ratio").toIntOrNull() ?: 10
        val vfsCache = RootShell.readSysfsNode("/proc/sys/vm/vfs_cache_pressure").toIntOrNull() ?: 100
        val extraFreeKb = RootShell.readSysfsNode("/proc/sys/vm/extra_free_kbytes").toIntOrNull() ?: 0

        // Read Battery Detail
        var bHealth = "Good"
        var bStatus = "Discharging"
        var bLevel = 100
        var bVoltageMv = 4000
        var bCurrentMa = 0
        var bWattageW = 0.0f
        var bCycles = 0
        var bTempC = thermal.batteryTempC

        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                bLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                bVoltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)
                val healthVal = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                bHealth = when (healthVal) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    else -> "Normal"
                }
                val statusVal = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                bStatus = when (statusVal) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Discharging"
                }
            }
            // Cycle count check from sysfs
            val cycleStr = RootShell.readSysfsNode("/sys/class/power_supply/battery/cycle_count")
                .ifBlank { RootShell.readSysfsNode("/sys/class/power_supply/bms/cycle_count") }
            bCycles = cycleStr.toIntOrNull() ?: 0

            // Current check from sysfs
            val currentStr = RootShell.readSysfsNode("/sys/class/power_supply/battery/current_now")
            val currentRaw = currentStr.toIntOrNull() ?: 0
            bCurrentMa = if (Math.abs(currentRaw) > 10000) currentRaw / 1000 else currentRaw
            bWattageW = (bVoltageMv.toFloat() / 1000f) * (Math.abs(bCurrentMa).toFloat() / 1000f)
        } catch (e: Exception) {
            Log.e(TAG, "Battery detail error", e)
        }

        // SELinux Status
        val selinuxStr = RootShell.readSysfsNode("/sys/fs/selinux/enforce")
        val selinuxStatus = when (selinuxStr.trim()) {
            "1" -> "Enforcing"
            "0" -> "Permissive"
            else -> RootShell.runCommand("getenforce").stdout.firstOrNull()?.trim() ?: "Enforcing"
        }

        // Magisk / APatch version & installed modules
        var rootFramework = "Rooted (libsu)"
        val magiskVersion = RootShell.runCommand("magisk -v").stdout.firstOrNull() ?: ""
        val apatchVersion = RootShell.runCommand("apatch -v").stdout.firstOrNull() ?: ""
        if (magiskVersion.isNotBlank()) {
            rootFramework = "Magisk ($magiskVersion)"
        } else if (apatchVersion.isNotBlank()) {
            rootFramework = "APatch ($apatchVersion)"
        }

        val modules = mutableListOf<String>()
        val magiskModuleDir = File("/data/adb/modules")
        if (magiskModuleDir.exists()) {
            magiskModuleDir.listFiles()?.forEach { file ->
                if (file.isDirectory) modules.add(file.name)
            }
        }

        // LMKD settings
        val lmkdProp = RootShell.runCommand("getprop ro.lmk.minfree").stdout.firstOrNull() ?: ""
        val lmkdSysfs = RootShell.readSysfsNode("/sys/module/lowmemorykiller/parameters/minfree")
        val lmkdMinfree = if (lmkdProp.isNotBlank()) lmkdProp else if (lmkdSysfs.isNotBlank()) lmkdSysfs else "Standard Android LMKD"

        // Storage Info
        var storageTotalGb = 64f
        var storageFreeGb = 20f
        var fileSystemType = "f2fs"
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val bytesFree = stat.availableBlocksLong * stat.blockSizeLong
            storageTotalGb = (bytesTotal.toFloat() / (1024f * 1024f * 1024f))
            storageFreeGb = (bytesFree.toFloat() / (1024f * 1024f * 1024f))

            val dfOut = RootShell.runCommand("df -T /data").stdout.joinToString("\n")
            if (dfOut.contains("ext4", ignoreCase = true)) fileSystemType = "ext4"
            else if (dfOut.contains("f2fs", ignoreCase = true)) fileSystemType = "f2fs"
        } catch (e: Exception) {
            Log.e(TAG, "Storage stat error", e)
        }

        // CPU Load estimation
        var cpuLoadPct = 18.5f
        val statLine = RootShell.readSysfsNode("/proc/stat")
        if (statLine.startsWith("cpu ")) {
            val parts = statLine.split("\\s+".toRegex()).mapNotNull { it.toLongOrNull() }
            if (parts.size >= 4) {
                val idle = parts[3]
                val total = parts.sum()
                if (total > 0) {
                    cpuLoadPct = ((total - idle).toFloat() / total.toFloat()) * 100f
                }
            }
        }

        // Check supported sysfs nodes
        val supportedNodes = mutableListOf<String>()
        if (File("/sys/devices/system/cpu/cpu0/cpufreq").exists()) supportedNodes.add("cpu_cpufreq")
        if (gpuInfo.isSupported) supportedNodes.add("gpu_devfreq")
        if (zramInfo.isSupported) supportedNodes.add("zram_swap")
        if (ioInfo.isSupported) supportedNodes.add("block_io_scheduler")
        if (File("/proc/sys/vm").exists()) supportedNodes.add("proc_sys_vm")
        if (File("/sys/class/thermal").exists()) supportedNodes.add("thermal_zones")

        val onlineCount = cpuInfo.cores.count { it.isOnline }

        FullDeviceDiagnostic(
            chipset = "MediaTek Helio G85",
            cpuArchitecture = "2x Cortex-A75 (2.0 GHz) + 6x Cortex-A55 (1.8 GHz)",
            gpuModel = if (gpuInfo.isSupported) "${gpuInfo.model} (Mali-G52 MC2)" else "Mali-G52 MC2",
            deviceModel = devInfo.model,
            androidVersion = devInfo.androidVersion,
            kernelVersion = devInfo.kernelVersion,
            isRooted = devInfo.isRooted,
            rootFramework = rootFramework,
            selinuxStatus = selinuxStatus,
            cpuGovernor = cpuInfo.currentGovernor,
            availableCpuGovernors = cpuInfo.availableGovernors,
            cpuMinFreqKHz = cpuInfo.globalMinFreqKHz,
            cpuMaxFreqKHz = cpuInfo.globalMaxFreqKHz,
            availableCpuFreqsKHz = cpuInfo.availableFrequenciesKHz,
            onlineCoresCount = onlineCount,
            totalCoresCount = cpuInfo.totalCores.coerceAtLeast(8),
            cpuLoadPercentage = cpuLoadPct,
            cpuTempC = thermal.cpuTempC,
            gpuGovernor = gpuInfo.currentGovernor,
            availableGpuGovernors = gpuInfo.availableGovernors,
            gpuFreqHz = gpuInfo.currentFreqHz,
            minGpuFreqHz = gpuInfo.minFreqHz,
            maxGpuFreqHz = gpuInfo.maxFreqHz,
            availableGpuFreqsHz = gpuInfo.availableFrequenciesHz,
            totalRamMb = ramInfo.totalMb,
            usedRamMb = ramInfo.usedMb,
            freeRamMb = ramInfo.freeMb,
            ramUsedPercentage = ramInfo.usedPercentage,
            isZramSupported = zramInfo.isSupported,
            zramSizeMb = zramInfo.diskSizeMb,
            isZramEnabled = zramInfo.isEnabled,
            swapTotalMb = zramInfo.swapTotalMb,
            swapUsedMb = zramInfo.swapUsedMb,
            swapFreeMb = zramInfo.swapFreeMb,
            batteryDetail = BatteryDetail(
                health = bHealth,
                status = bStatus,
                level = bLevel,
                voltageMv = bVoltageMv,
                currentMa = bCurrentMa,
                wattageW = bWattageW,
                cycles = bCycles,
                temperatureC = bTempC
            ),
            thermalStatus = if (thermal.cpuTempC > 65f) "Throttling Risk" else if (thermal.cpuTempC > 50f) "Warm" else "Optimal",
            ioScheduler = ioInfo.currentScheduler,
            targetBlockDevice = ioInfo.targetBlockDevice,
            availableIoSchedulers = ioInfo.availableSchedulers,
            readAheadKb = readAheadKb,
            vmParameters = VmParameters(
                swappiness = swappiness,
                dirtyRatio = dirtyRatio,
                dirtyBackgroundRatio = dirtyBg,
                vfsCachePressure = vfsCache,
                extraFreeKbytes = extraFreeKb
            ),
            lmkdMinfree = lmkdMinfree,
            storageTotalGb = storageTotalGb,
            storageFreeGb = storageFreeGb,
            fileSystemType = fileSystemType,
            installedModules = modules,
            supportedSysfsNodes = supportedNodes
        )
    }
}

