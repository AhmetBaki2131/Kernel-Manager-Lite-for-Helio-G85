package com.example.ui.screens.aianalyzer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalyzerScreen(
    viewModel: AiAnalyzerViewModel = viewModel()
) {
    val diagnostic by viewModel.diagnostic.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isOnlineAi by viewModel.isOnlineAi.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val applyMessage by viewModel.applyMessage.collectAsState()
    val isApplying by viewModel.isApplying.collectAsState()

    var showConfirmRecsDialog by remember { mutableStateOf(false) }
    var selectedProfileToApply by remember { mutableStateOf<HelioG85Profile?>(null) }
    var showTelemetryDetails by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(applyMessage) {
        applyMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Device Analyzer",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Optimized for MediaTek Helio G85",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshDiagnostics() },
                        modifier = Modifier.testTag("refresh_telemetry_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Telemetry")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // --- CHIPSET BADGE & HARDWARE STATUS ---
            item {
                HelioG85HeaderCard(diagnostic = diagnostic)
            }

            // --- AI ANALYZE ACTION BUTTON ---
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Intelligent Helio G85 Optimization Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Analyzes CPU Cortex-A75/A55 clusters, Mali-G52 GPU, ZRAM compression, memory pressure, and thermals to generate custom Helio G85 tuning parameters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.runAiAnalysis() },
                            enabled = !isAnalyzing && !isApplying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("analyze_with_ai_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Analyzing Live Hardware State...")
                            } else {
                                Icon(Icons.Default.Psychology, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyze with AI",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (analysisResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(if (isOnlineAi) "Powered by Gemini 3.5 Flash API" else "Helio G85 Offline Optimization Engine")
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isOnlineAi) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // --- TELEMETRY DATA COLLAPSIBLE CARD ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Equalizer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "Collected Live Diagnostic Telemetry",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            TextButton(
                                onClick = { showTelemetryDetails = !showTelemetryDetails },
                                modifier = Modifier.testTag("toggle_telemetry_button")
                            ) {
                                Text(if (showTelemetryDetails) "Hide" else "View All")
                                Icon(
                                    if (showTelemetryDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        diagnostic?.let { d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RAM Used: ${d.usedRamMb}MB / ${d.totalRamMb}MB (${d.ramUsedPercentage.toInt()}%)", style = MaterialTheme.typography.bodyMedium)
                                Text("CPU Temp: ${d.cpuTempC}°C", style = MaterialTheme.typography.bodyMedium, color = if (d.cpuTempC > 50f) Color(0xFFE57373) else MaterialTheme.colorScheme.primary)
                            }

                            AnimatedVisibility(
                                visible = showTelemetryDetails,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider()
                                    TelemetryItem("Chipset", "${d.chipset} (${d.cpuArchitecture})")
                                    TelemetryItem("GPU Model", "${d.gpuModel} @ ${d.gpuFreqHz / 1_000_000} MHz (${d.gpuGovernor})")
                                    TelemetryItem("CPU Governor", "${d.cpuGovernor} (${d.onlineCoresCount}/${d.totalCoresCount} cores online)")
                                    TelemetryItem("CPU Min/Max Freq", "${d.cpuMinFreqKHz / 1000} MHz / ${d.cpuMaxFreqKHz / 1000} MHz")
                                    TelemetryItem("ZRAM / Swap", "${d.zramSizeMb} MB ZRAM | Swap Used: ${d.swapUsedMb} MB / ${d.swapTotalMb} MB")
                                    TelemetryItem("Swappiness", "${d.vmParameters.swappiness} (dirty_ratio: ${d.vmParameters.dirtyRatio})")
                                    TelemetryItem("Battery State", "${d.batteryDetail.level}% (${d.batteryDetail.status}, ${d.batteryDetail.health}) @ ${d.batteryDetail.temperatureC}°C")
                                    TelemetryItem("Battery Wattage", "${String.format("%.2f", d.batteryDetail.wattageW)} W (${d.batteryDetail.currentMa} mA, ${d.batteryDetail.cycles} cycles)")
                                    TelemetryItem("I/O Scheduler", "${d.ioScheduler} (Read-Ahead: ${d.readAheadKb} KB on ${d.targetBlockDevice})")
                                    TelemetryItem("Android / Root", "${d.androidVersion} | ${d.rootFramework} (${d.selinuxStatus})")
                                    TelemetryItem("Storage & FS", "${String.format("%.1f", d.storageFreeGb)} GB free / ${String.format("%.1f", d.storageTotalGb)} GB total (${d.fileSystemType})")
                                    if (d.installedModules.isNotEmpty()) {
                                        TelemetryItem("Magisk/APatch Modules", d.installedModules.joinToString(", "))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- AI ANALYSIS RESULTS ---
            analysisResult?.let { res ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Device Overview & Diagnostic Summary",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = res.deviceOverview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // --- AI RECOMMENDATIONS & APPLY BUTTON ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "AI Recommended Helio G85 Tuning",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val rec = res.recommendations
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                RecGridRow("CPU Governor", rec.cpuGovernor, "CPU Frequencies", "${rec.cpuMinFreqMhz} - ${rec.cpuMaxFreqMhz} MHz")
                                RecGridRow("GPU Frequency", "${rec.gpuFreqMhz} MHz", "ZRAM Size", "${rec.zramSizeMb} MB")
                                RecGridRow("Swappiness", "${rec.swappiness}", "I/O Scheduler", rec.ioScheduler)
                                RecGridRow("Read-Ahead Buffer", "${rec.readAheadKb} KB", "VM Parameters", rec.vmParameters)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Helio G85 Optimization Justifications:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            res.explanations.forEach { exp ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "• ${exp.target}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = exp.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showConfirmRecsDialog = true },
                                enabled = !isApplying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_ai_recommendations_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply All AI Recommendations")
                            }
                        }
                    }
                }

                // --- 4 DYNAMIC HELIO G85 PROFILES ---
                item {
                    Text(
                        text = "Dynamic MediaTek Helio G85 Profiles",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(res.profiles) { profile ->
                    HelioG85ProfileCard(
                        profile = profile,
                        onApply = { selectedProfileToApply = profile },
                        isApplying = isApplying
                    )
                }

                // --- SAFETY NOTES ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2216)),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
                            Column {
                                Text(
                                    text = "Thermal & Compatibility Guardrails",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFB74D)
                                )
                                Text(
                                    text = res.safetyNotes.ifBlank { "Never recommend unsupported kernel parameters. All profile changes are verified against active sysfs nodes before execution." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // --- CONFIRMATION DIALOG FOR RECOMMENDATIONS ---
    if (showConfirmRecsDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRecsDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Confirm Helio G85 Optimization") },
            text = {
                Text("Are you sure you want to apply the AI recommended parameters? Settings will be applied via root to CPU, GPU, ZRAM, and I/O scheduler sysfs nodes.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRecsDialog = false
                        analysisResult?.recommendations?.let { recs ->
                            viewModel.applyRecommendations(recs)
                        }
                    },
                    modifier = Modifier.testTag("confirm_apply_recommendations_button")
                ) {
                    Text("Apply Changes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmRecsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- CONFIRMATION DIALOG FOR PROFILE APPLY ---
    selectedProfileToApply?.let { profile ->
        AlertDialog(
            onDismissRequest = { selectedProfileToApply = null },
            icon = { Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            title = { Text("Apply ${profile.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You are about to apply this dynamic profile:")
                    Text(profile.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    Text("• CPU Gov: ${profile.cpuGovernor} (${profile.cpuMinFreqKHz/1000} - ${profile.cpuMaxFreqKHz/1000} MHz)")
                    Text("• GPU Gov: ${profile.gpuGovernor} (${profile.gpuFreqHz/1_000_000} MHz)")
                    Text("• ZRAM Size: ${profile.zramSizeMb} MB | Swappiness: ${profile.swappiness}")
                    Text("• I/O Scheduler: ${profile.ioScheduler}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = profile
                        selectedProfileToApply = null
                        viewModel.applyProfile(p)
                    },
                    modifier = Modifier.testTag("confirm_apply_profile_button")
                ) {
                    Text("Apply Profile")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedProfileToApply = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HelioG85HeaderCard(diagnostic: FullDeviceDiagnostic?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MediaTek Helio G85",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "2x Cortex-A75 (2.0GHz) + 6x Cortex-A55 (1.8GHz) | Mali-G52 MC2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (diagnostic?.isRooted == true) Color(0xFF66BB6A) else Color(0xFFFFA726))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusBadge(
                    label = "Cores Online",
                    value = "${diagnostic?.onlineCoresCount ?: 8}/${diagnostic?.totalCoresCount ?: 8}"
                )
                StatusBadge(
                    label = "Thermal Zone",
                    value = "${diagnostic?.cpuTempC ?: 38f}°C",
                    isWarning = (diagnostic?.cpuTempC ?: 0f) > 55f
                )
                StatusBadge(
                    label = "RAM Load",
                    value = "${diagnostic?.ramUsedPercentage?.toInt() ?: 50}%"
                )
                StatusBadge(
                    label = "Root",
                    value = if (diagnostic?.isRooted == true) "Active" else "No Root"
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, value: String, isWarning: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isWarning) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun RecGridRow(label1: String, val1: String, label2: String, val2: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = label1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = val1, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = label2, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = val2, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun HelioG85ProfileCard(
    profile: HelioG85Profile,
    onApply: () -> Unit,
    isApplying: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_card_${profile.title}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CPU Gov: ${profile.cpuGovernor}", style = MaterialTheme.typography.bodySmall)
                    Text("Clocks: ${profile.cpuMinFreqKHz / 1000} - ${profile.cpuMaxFreqKHz / 1000} MHz", style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text("GPU: ${profile.gpuGovernor} (${profile.gpuFreqHz / 1_000_000}MHz)", style = MaterialTheme.typography.bodySmall)
                    Text("ZRAM: ${profile.zramSizeMb}MB (Swap=${profile.swappiness})", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onApply,
                enabled = !isApplying,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_profile_button_${profile.title}")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply ${profile.title}")
            }
        }
    }
}
