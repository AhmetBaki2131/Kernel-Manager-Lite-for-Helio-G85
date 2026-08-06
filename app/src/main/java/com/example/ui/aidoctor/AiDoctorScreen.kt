package com.example.ui.aidoctor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiScoreDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDoctorScreen(
    viewModel: AiDoctorViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Kernel Doctor", fontWeight = FontWeight.Bold)
                        Text(
                            "MediaTek Helio G85 Live Telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runDoctorDiagnosis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Diagnosis")
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "AI Report")
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
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "AI Kernel Doctor Analyzing Device...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Evaluating 2x A75 + 6x A55 core frequencies, Mali-G52 GPU, thermal zones, and ZRAM parameters.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Diagnosis Failed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.runDoctorDiagnosis() }) {
                        Text("Retry Diagnosis")
                    }
                }
            } else {
                val doctor = uiState.doctorAnalysis
                val scores = doctor.scores

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
                ) {
                    // Gemini / Offline Banner
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isLiveGeminiUsed)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isLiveGeminiUsed) Icons.Default.AutoAwesome else Icons.Default.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = if (uiState.isLiveGeminiUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (uiState.isLiveGeminiUsed) "Gemini AI Online Diagnosis" else "Helio G85 Offline Engine Active",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = doctor.summaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Score Section Header
                    item {
                        Text(
                            "AI Live Kernel Scores",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // AI Scores Grid
                    item {
                        val scoreList = listOf(
                            scores.overallHealth,
                            scores.battery,
                            scores.performance,
                            scores.gaming,
                            scores.thermal,
                            scores.stability,
                            scores.memory,
                            scores.rootConfig
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            scoreList.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { score ->
                                        ScoreCard(score = score, modifier = Modifier.weight(1f))
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Doctor Diagnosis Details Header
                    item {
                        Text(
                            "Medical Kernel Diagnosis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "What is Happening?",
                            icon = Icons.Default.Visibility,
                            content = doctor.whatIsHappening
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "Why is it Happening?",
                            icon = Icons.Default.Psychology,
                            content = doctor.whyItIsHappening
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "Is it Normal?",
                            icon = Icons.Default.CheckCircle,
                            content = doctor.isItNormal
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "Performance Impact",
                            icon = Icons.Default.Speed,
                            content = doctor.performanceImpact
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "Battery Impact",
                            icon = Icons.Default.BatteryChargingFull,
                            content = doctor.batteryImpact
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "Gaming Impact (Mali-G52)",
                            icon = Icons.Default.SportsEsports,
                            content = doctor.gamingImpact
                        )
                    }

                    item {
                        DiagnosisDetailCard(
                            title = "Doctor Recommendations",
                            icon = Icons.Default.Lightbulb,
                            content = doctor.shouldBeChanged,
                            isHighlight = true
                        )
                    }
                }
            }
        }

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("AI Kernel Report", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = uiState.reportText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Kernel Report", uiState.reportText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                            showReportDialog = false
                        }
                    ) {
                        Text("Copy Report")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun ScoreCard(score: AiScoreDetail, modifier: Modifier = Modifier) {
    val gradeColor = when (score.grade) {
        "A+", "A" -> Color(0xFF2E7D32)
        "B+", "B" -> Color(0xFF1565C0)
        "C" -> Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    score.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(gradeColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        score.grade,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = score.percentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = gradeColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "${score.percentage}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (score.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    score.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun DiagnosisDetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String,
    isHighlight: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}
