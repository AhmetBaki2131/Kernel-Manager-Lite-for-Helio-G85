package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object AiAnalyzer : Screen("ai_analyzer", "AI Analyzer", Icons.Default.AutoAwesome)
    object AiDoctor : Screen("ai_doctor", "AI Doctor", Icons.Default.MedicalServices)
    object SysfsExplorer : Screen("sysfs_explorer", "SysFS Explorer", Icons.Default.Folder)
    object BackupRestore : Screen("backup_restore", "Backups", Icons.Default.Backup)
    object Benchmark : Screen("benchmark", "Benchmark", Icons.Default.Assessment)
    object AiHistory : Screen("ai_history", "AI History", Icons.Default.HistoryEdu)
    object Cpu : Screen("cpu", "CPU Control", Icons.Default.Memory)
    object Gpu : Screen("gpu", "GPU Control", Icons.Default.Speed)
    object Zram : Screen("zram", "ZRAM", Icons.Default.Storage)
    object Scheduler : Screen("scheduler", "I/O Scheduler", Icons.Default.SdCard)
    object Profiles : Screen("profiles", "Profiles", Icons.Default.Tune)
    object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    object Logs : Screen("logs", "Logs", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavScreens = listOf(
            Dashboard,
            AiAnalyzer,
            AiDoctor,
            SysfsExplorer,
            Benchmark,
            BackupRestore,
            AiHistory,
            Cpu,
            Gpu,
            Zram,
            Scheduler,
            Profiles,
            Terminal,
            Logs,
            Settings
        )
    }

}
