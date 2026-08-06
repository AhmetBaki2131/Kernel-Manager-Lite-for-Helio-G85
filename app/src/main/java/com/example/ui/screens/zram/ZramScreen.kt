package com.example.ui.screens.zram

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.StatCard
import com.example.ui.components.SysfsStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZramScreen(
    viewModel: ZramViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    if (state.showConfirmDialog) {
        ConfirmationDialog(
            title = "Resize ZRAM Disk",
            message = "Changing ZRAM size will briefly execute swapoff & swapon, resetting memory cache. Target: ${state.targetSizeMb} MB. Continue?",
            onConfirm = { viewModel.applyResize() },
            onDismiss = { viewModel.dismissConfirm() }
        )
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
                            text = "MEMORY",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ZRAM Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadZramData() },
                        modifier = Modifier.testTag("refresh_zram_button")
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
            if (state.zramInfo.isSupported) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.requestResizeConfirm() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text(if (state.isApplying) "Applying..." else "Apply Size", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("apply_zram_fab")
                )
            }
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
                // ZRAM Status Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zram_status_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "ZRAM Swap Device",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                SysfsStatusBadge(isSupported = state.zramInfo.isSupported)
                            }

                            if (state.zramInfo.isSupported) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Current Disk Size",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${state.zramInfo.diskSizeMb} MB",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (state.zramInfo.isEnabled) "Enabled" else "Disabled",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Switch(
                                            checked = state.zramInfo.isEnabled,
                                            onCheckedChange = { viewModel.toggleZram(it) },
                                            modifier = Modifier.testTag("zram_toggle_switch")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.zramInfo.isSupported) {
                    // Swap Usage Stats
                    item {
                        Text(
                            text = "Swap Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatCard(
                                title = "Used Swap",
                                value = "${state.zramInfo.swapUsedMb} MB",
                                subtitle = "Total: ${state.zramInfo.swapTotalMb} MB",
                                progress = if (state.zramInfo.swapTotalMb > 0) state.zramInfo.swapUsedMb.toFloat() / state.zramInfo.swapTotalMb.toFloat() else 0f,
                                iconTint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                testTag = "zram_used_stat_card"
                            )
                            StatCard(
                                title = "Free Swap",
                                value = "${state.zramInfo.swapFreeMb} MB",
                                subtitle = "Unallocated Swap",
                                iconTint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                testTag = "zram_free_stat_card"
                            )
                        }
                    }

                    // Preset Size Selector Chips
                    item {
                        Text(
                            text = "Target Disk Size",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val presetSizes = listOf(512L, 1024L, 2048L, 3072L, 4096L, 6144L, 8192L)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetSizes) { sizeMb ->
                                FilterChip(
                                    selected = (state.targetSizeMb == sizeMb),
                                    onClick = { viewModel.onSizeSelected(sizeMb) },
                                    label = { Text("${sizeMb} MB") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("zram_chip_${sizeMb}MB")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
