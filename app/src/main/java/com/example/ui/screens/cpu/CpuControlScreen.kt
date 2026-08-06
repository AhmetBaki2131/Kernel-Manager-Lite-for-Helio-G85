package com.example.ui.screens.cpu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CpuCoreInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuControlScreen(
    viewModel: CpuViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Text(
                            text = "HARDWARE",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "CPU Control",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadCpuData() },
                        modifier = Modifier.testTag("refresh_cpu_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.applyCpuSettings() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text(if (state.isApplying) "Applying..." else "Apply Changes", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("apply_cpu_fab")
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // CPU Governor Selector
                item {
                    Text(
                        text = "CPU Scaling Governor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.cpuInfo.availableGovernors.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.cpuInfo.availableGovernors) { gov ->
                                FilterChip(
                                    selected = (state.selectedGovernor == gov),
                                    onClick = { viewModel.onGovernorSelected(gov) },
                                    label = { Text(gov) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("gov_chip_$gov")
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Current Governor: ${state.cpuInfo.currentGovernor}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Minimum Frequency Selector
                item {
                    Text(
                        text = "Minimum Frequency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FreqSelectorChips(
                        availableFreqs = state.cpuInfo.availableFrequenciesKHz,
                        selectedFreqKHz = state.selectedMinFreqKHz,
                        onSelect = { viewModel.onMinFreqSelected(it) },
                        testTagPrefix = "min_freq"
                    )
                }

                // Maximum Frequency Selector
                item {
                    Text(
                        text = "Maximum Frequency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FreqSelectorChips(
                        availableFreqs = state.cpuInfo.availableFrequenciesKHz,
                        selectedFreqKHz = state.selectedMaxFreqKHz,
                        onSelect = { viewModel.onMaxFreqSelected(it) },
                        testTagPrefix = "max_freq"
                    )
                }

                // Individual Cores Section
                item {
                    Text(
                        text = "CPU Cores (${state.cpuInfo.totalCores} Total)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(state.cpuInfo.cores) { core ->
                    CpuCoreCard(
                        core = core,
                        onToggle = { online -> viewModel.toggleCoreState(core.coreId, online) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreqSelectorChips(
    availableFreqs: List<Long>,
    selectedFreqKHz: Long,
    onSelect: (Long) -> Unit,
    testTagPrefix: String
) {
    if (availableFreqs.isEmpty()) {
        Text(
            text = "Frequency scaling not reported by kernel.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(availableFreqs) { freq ->
                val freqMhz = freq / 1000
                FilterChip(
                    selected = (selectedFreqKHz == freq),
                    onClick = { onSelect(freq) },
                    label = { Text("${freqMhz} MHz") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_$freqMhz")
                )
            }
        }
    }
}

@Composable
private fun CpuCoreCard(
    core: CpuCoreInfo,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("core_card_${core.coreId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = if (core.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CPU Core ${core.coreId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (core.isOnline) "${core.currentFreqMhz} MHz (${core.governor})" else "Offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (core.isOnline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Core 0 should generally remain enabled
            Switch(
                checked = core.isOnline,
                onCheckedChange = onToggle,
                enabled = core.coreId != 0,
                modifier = Modifier.testTag("core_switch_${core.coreId}")
            )
        }
    }
}
