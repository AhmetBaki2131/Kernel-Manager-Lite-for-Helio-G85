package com.example.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    if (state.showEditorDialog && state.editingProfile != null) {
        ProfileEditorDialog(
            initialProfile = state.editingProfile!!,
            onSave = { viewModel.saveProfile(it) },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Profiles JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = { Text("Paste JSON Profiles Array") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("import_json_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importProfiles(importJsonText)
                        showImportDialog = false
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Profiles", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportProfiles() },
                        modifier = Modifier.testTag("export_profiles_button")
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export")
                    }
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_profiles_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Profile") },
                modifier = Modifier.testTag("add_profile_fab")
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
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Built-in & Custom Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(state.profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = (profile.id == state.selectedProfileId),
                        isApplying = state.isApplying,
                        onApply = { viewModel.applyProfile(profile) },
                        onEdit = { viewModel.openEditDialog(profile) },
                        onDuplicate = { viewModel.duplicateProfile(profile) },
                        onDelete = { viewModel.deleteProfile(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileEntity,
    isSelected: Boolean,
    isApplying: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_card_${profile.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (profile.name.lowercase()) {
                            "game mode" -> Icons.Default.SportsEsports
                            "battery saver" -> Icons.Default.BatterySaver
                            else -> Icons.Default.Tune
                        },
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Specs Overview Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "CPU: ${profile.cpuGovernor}", style = MaterialTheme.typography.labelSmall)
                Text(text = "GPU: ${profile.gpuGovernor}", style = MaterialTheme.typography.labelSmall)
                Text(text = "ZRAM: ${profile.zramSizeMb}MB", style = MaterialTheme.typography.labelSmall)
                Text(text = "I/O: ${profile.ioScheduler}", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.testTag("duplicate_profile_${profile.id}")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_profile_${profile.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }

                if (!profile.isBuiltIn) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_profile_${profile.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onApply,
                    enabled = !isApplying,
                    modifier = Modifier.testTag("apply_profile_${profile.id}")
                ) {
                    Text(if (isSelected) "Re-Apply" else "Apply")
                }
            }
        }
    }
}
