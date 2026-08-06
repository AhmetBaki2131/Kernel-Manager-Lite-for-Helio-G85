package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.aidoctor.AiDoctorScreen
import com.example.ui.aidoctor.AiDoctorViewModel
import com.example.ui.backup.BackupRestoreScreen
import com.example.ui.backup.BackupViewModel
import com.example.ui.benchmark.BenchmarkScreen
import com.example.ui.benchmark.BenchmarkViewModel
import com.example.ui.history.AiHistoryScreen
import com.example.ui.history.AiHistoryViewModel
import com.example.ui.screens.aianalyzer.AiAnalyzerScreen
import com.example.ui.screens.cpu.CpuControlScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.gpu.GpuControlScreen
import com.example.ui.screens.logs.LogsScreen
import com.example.ui.screens.profiles.ProfilesScreen
import com.example.ui.screens.scheduler.IoSchedulerScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.terminal.TerminalScreen
import com.example.ui.screens.zram.ZramScreen
import com.example.ui.sysfsexplorer.SysfsExplorerScreen
import com.example.ui.sysfsexplorer.SysfsExplorerViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                    .testTag("bottom_navigation_bar")
            ) {
                Screen.bottomNavScreens.forEach { screen ->
                    val isSelected = (currentRoute == screen.route)
                    NavigationBarItem(
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color(0xFF3E4759),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        alwaysShowLabel = false,
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.AiAnalyzer.route) {
                AiAnalyzerScreen()
            }
            composable(Screen.AiDoctor.route) {
                val vm: AiDoctorViewModel = viewModel()
                AiDoctorScreen(viewModel = vm)
            }
            composable(Screen.SysfsExplorer.route) {
                val vm: SysfsExplorerViewModel = viewModel()
                SysfsExplorerScreen(viewModel = vm)
            }
            composable(Screen.Benchmark.route) {
                val vm: BenchmarkViewModel = viewModel()
                BenchmarkScreen(viewModel = vm)
            }
            composable(Screen.BackupRestore.route) {
                val vm: BackupViewModel = viewModel()
                BackupRestoreScreen(viewModel = vm)
            }
            composable(Screen.AiHistory.route) {
                val vm: AiHistoryViewModel = viewModel()
                AiHistoryScreen(viewModel = vm)
            }
            composable(Screen.Cpu.route) {
                CpuControlScreen()
            }
            composable(Screen.Gpu.route) {
                GpuControlScreen()
            }
            composable(Screen.Zram.route) {
                ZramScreen()
            }
            composable(Screen.Scheduler.route) {
                IoSchedulerScreen()
            }
            composable(Screen.Profiles.route) {
                ProfilesScreen()
            }
            composable(Screen.Terminal.route) {
                TerminalScreen()
            }
            composable(Screen.Logs.route) {
                LogsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
