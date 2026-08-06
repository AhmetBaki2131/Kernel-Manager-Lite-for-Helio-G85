package com.example.ui.sysfsexplorer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SysfsNodeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SysfsExplorerScreen(
    viewModel: SysfsExplorerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SysFS Explorer", fontWeight = FontWeight.Bold)
                        Text(
                            uiState.currentPath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.currentPath != "/" && uiState.currentPath != "/sys") {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Up")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateToPath(uiState.currentPath) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search sysfs nodes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Recent Shortcuts Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.recentPaths) { path ->
                    FilterChip(
                        selected = path == uiState.currentPath,
                        onClick = { viewModel.navigateToPath(path) },
                        label = { Text(path.substringAfterLast('/'), fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.isLoadingDirectory) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sysfs files or subdirectories found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.filteredItems) { item ->
                        SysfsItemRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.navigateToPath(item.path)
                                } else {
                                    viewModel.selectNodeForEdit(item)
                                }
                            },
                            onCopyPath = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SysFS Path", item.path))
                                Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // Edit Node Modal Dialog
        uiState.selectedNodeForEdit?.let { selectedNode ->
            SysfsNodeEditDialog(
                node = selectedNode,
                uiState = uiState,
                onDismiss = { viewModel.dismissEditDialog() },
                onSave = { newValue -> viewModel.writeNodeValue(selectedNode.path, newValue) }
            )
        }
    }
}

@Composable
fun SysfsItemRow(
    item: SysfsNodeItem,
    onClick: () -> Unit,
    onCopyPath: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!item.isDirectory && item.currentValue.isNotBlank()) {
                Text(
                    "Value: ${item.currentValue}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (item.isWritable) {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text("RW", fontSize = 10.sp, modifier = Modifier.padding(2.dp))
            }
        }

        IconButton(onClick = onCopyPath) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Path", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SysfsNodeEditDialog(
    node: SysfsNodeItem,
    uiState: SysfsExplorerUiState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var editValue by remember { mutableStateOf(node.currentValue) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("SysFS Node Control", fontWeight = FontWeight.Bold)
                Text(
                    node.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("Current Node Path:", style = MaterialTheme.typography.labelSmall)
                    Text(node.path, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }

                item {
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("Value to Write") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editValue = node.currentValue },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Original", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Node Value", node.currentValue))
                                Toast.makeText(context, "Value copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copy Value", fontSize = 11.sp)
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Node Doctor Explanation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (uiState.isLoadingExplanation) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            } else {
                                val expl = uiState.selectedNodeExplanation
                                if (expl != null) {
                                    Text(expl.whatItControls, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Performance: ${expl.performanceImpact}", style = MaterialTheme.typography.bodySmall)
                                    Text("Battery: ${expl.batteryImpact}", style = MaterialTheme.typography.bodySmall)
                                    Text("Risks: ${expl.possibleRisks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("AI Explanation unavailable for this sysfs node.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Warning: Writing unsupported sysfs values may freeze the kernel or cause an unexpected reboot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(editValue) }) {
                Text("Apply Value")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
