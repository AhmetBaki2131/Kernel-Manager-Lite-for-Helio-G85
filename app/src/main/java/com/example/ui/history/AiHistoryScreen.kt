package com.example.ui.history

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHistoryScreen(
    viewModel: AiHistoryViewModel
) {
    val historyList by viewModel.historyFlow.collectAsState(initial = emptyList())
    val selectedForComp by viewModel.selectedForComparison.collectAsState()
    var showCompareModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Diagnosis History", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedForComp.size == 2) {
                        IconButton(onClick = { showCompareModal = true }) {
                            Icon(Icons.Default.Compare, contentDescription = "Compare 2 Runs")
                        }
                    }
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.HistoryEdu,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No AI History Saved Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Run diagnoses in AI Doctor or AI Analyzer to automatically record performance scores, CPU temperatures, and battery drain statistics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (selectedForComp.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${selectedForComp.size} run(s) selected for comparison",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedForComp.size == 2) {
                                    Button(onClick = { showCompareModal = true }) {
                                        Text("Compare Now")
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
                    ) {
                        items(historyList) { item ->
                            val isSelected = selectedForComp.any { it.id == item.id }
                            HistoryItemCard(
                                item = item,
                                isSelected = isSelected,
                                onSelectToggle = { viewModel.toggleComparisonSelection(item) }
                            )
                        }
                    }
                }
            }
        }

        if (showCompareModal && selectedForComp.size == 2) {
            val itemA = selectedForComp[0]
            val itemB = selectedForComp[1]

            AlertDialog(
                onDismissRequest = { showCompareModal = false },
                title = { Text("AI History Comparison", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Comparing Run #${itemA.id} vs Run #${itemB.id}")

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Overall AI Health Score", fontWeight = FontWeight.Bold)
                                Text("Run A: ${itemA.overallScore}%  vs  Run B: ${itemB.overallScore}%")
                                val diffScore = itemB.overallScore - itemA.overallScore
                                Text(
                                    if (diffScore >= 0) "+$diffScore% Health Improvement" else "$diffScore% Regression",
                                    fontWeight = FontWeight.Bold,
                                    color = if (diffScore >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CPU Temperature & Battery", fontWeight = FontWeight.Bold)
                                Text("Run A Temp: ${itemA.cpuTempC}°C | Battery: ${itemA.batteryLevel}%")
                                Text("Run B Temp: ${itemB.cpuTempC}°C | Battery: ${itemB.batteryLevel}%")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showCompareModal = false }) {
                        Text("Close Comparison")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: AiHistoryEntity,
    isSelected: Boolean,
    onSelectToggle: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Diagnosis #${item.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("AI Score: ${item.overallScore}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("CPU Temp: ${item.cpuTempC}°C")
                Text("Battery: ${item.batteryLevel}%")
            }

            if (item.deviceOverview.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.deviceOverview, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}
