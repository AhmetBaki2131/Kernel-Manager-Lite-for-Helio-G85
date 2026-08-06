package com.example.ui.benchmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BenchmarkResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Helio G85 Built-in Benchmark", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Helio G85 Synthetic Speed Test",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Measures Cortex-A75/A55 CPU floating-point operations, LPDDR4X memory throughput, and storage I/O bandwidth.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (uiState.isRunning) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    progress = uiState.progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(uiState.currentStep, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.runBenchmark() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Benchmark Test")
                            }
                        }
                    }
                }
            }

            uiState.lastResult?.let { res ->
                item {
                    Text("Benchmark Results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                item {
                    BenchmarkResultCard(res = res)
                }

                uiState.previousResult?.let { prev ->
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Before vs After Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                val gain = res.totalScore - prev.totalScore
                                val pct = (gain.toFloat() / prev.totalScore.toFloat()) * 100f
                                Text(
                                    text = if (gain >= 0) "Performance Gain: +$gain pts (+${String.format("%.1f", pct)}%)"
                                    else "Performance Impact: $gain pts (${String.format("%.1f", pct)}%)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (gain >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenchmarkResultCard(res: BenchmarkResult) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Score", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${res.totalScore}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BenchmarkItemRow("CPU Single-Core Score", "${res.cpuSingleCoreScore} pts")
                BenchmarkItemRow("CPU Multi-Core (8-Thread) Score", "${res.cpuMultiCoreScore} pts")
                BenchmarkItemRow("RAM Memory Bandwidth", "${res.ramReadWriteMbps} MB/s")
                BenchmarkItemRow("Storage Read/Write Bandwidth", "${res.storageReadWriteMbps} MB/s")
                BenchmarkItemRow("Peak Thermal Temperature", "${res.peakTempC}°C")
            }
        }
    }
}

@Composable
fun BenchmarkItemRow(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(valStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
