package com.wifield.app.ui.screens.active

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifield.app.service.SpeedTestPhase
import com.wifield.app.ui.components.InfoChip
import com.wifield.app.ui.components.SignalGauge
import com.wifield.app.ui.components.SnapshotDialog
import com.wifield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDiagnosticScreen(
    viewModel: ActiveDiagnosticViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Active Diagnostic", fontWeight = FontWeight.Bold)
                        if (uiState.connectionInfo != null) {
                            Text(
                                text = uiState.connectionInfo!!.ssid,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshConnection() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (uiState.isConnected) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showProjectPicker() },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text("Snapshot") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        if (!uiState.isConnected) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Not connected to WiFi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Connect to a WiFi network to use active diagnostic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshConnection() }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Signal gauge
                val connInfo = uiState.connectionInfo!!
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SignalGauge(
                            rssi = connInfo.rssi,
                            label = connInfo.ssid
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoChip(label = "BSSID: ${connInfo.bssid}")
                            InfoChip(label = "${connInfo.linkSpeed} Mbps")
                            InfoChip(label = "${connInfo.frequency} MHz")
                        }
                    }
                }

                // Speed test card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speed Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { viewModel.runSpeedTest() },
                                enabled = !uiState.isRunningSpeedTest
                            ) {
                                if (uiState.isRunningSpeedTest) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (uiState.isRunningSpeedTest) "Measuring..." else "Start")
                            }
                        }

                        if (uiState.isRunningSpeedTest) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.speedTestProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = when (uiState.speedTestPhase) {
                                    SpeedTestPhase.DOWNLOAD -> "Download: ${String.format("%.1f", uiState.currentSpeed)} Mbps"
                                    SpeedTestPhase.UPLOAD -> "Upload: ${String.format("%.1f", uiState.currentSpeed)} Mbps"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SpeedMetric(
                                label = "Download",
                                value = "${String.format("%.1f", uiState.testResults.downloadSpeed)} Mbps",
                                icon = Icons.Default.ArrowDownward,
                                color = SignalGood
                            )
                            SpeedMetric(
                                label = "Upload",
                                value = "${String.format("%.1f", uiState.testResults.uploadSpeed)} Mbps",
                                icon = Icons.Default.ArrowUpward,
                                color = SignalExcellent
                            )
                        }
                    }
                }

                // Ping test card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Latency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { viewModel.runPingTest() },
                                enabled = !uiState.isRunningPing
                            ) {
                                if (uiState.isRunningPing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (uiState.isRunningPing) "Measuring..." else "Ping (20x)")
                            }
                        }

                        if (uiState.isRunningPing && uiState.pingProgress.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = uiState.pingProgress,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PingMetric("Latency", "${String.format("%.1f", uiState.testResults.latency)} ms")
                            PingMetric("Jitter", "${String.format("%.1f", uiState.testResults.jitter)} ms")
                            PingMetric("Loss", "${String.format("%.1f", uiState.testResults.packetLoss)}%")
                            PingMetric("Gateway", "${String.format("%.1f", uiState.testResults.gatewayLatency)} ms")
                        }
                    }
                }

                // Monitoring card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monitoring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(onClick = { viewModel.toggleMonitoring() }) {
                                Icon(
                                    imageVector = if (uiState.monitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (uiState.monitoring) "Stop" else "Start")
                            }
                        }

                        if (uiState.rssiHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("RSSI", style = MaterialTheme.typography.labelMedium)
                            MiniLineChart(
                                data = uiState.rssiHistory.map { it.toFloat() },
                                color = SignalGood,
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            )
                        }

                        if (uiState.latencyHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Latency (ms)", style = MaterialTheme.typography.labelMedium)
                            MiniLineChart(
                                data = uiState.latencyHistory.map { it.toFloat() },
                                color = SignalFair,
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Project picker
    if (uiState.showProjectPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.hideProjectPicker() },
            title = { Text("Select project") },
            text = {
                if (uiState.projects.isEmpty()) {
                    Text("No projects. Create one from the home screen.")
                } else {
                    Column {
                        uiState.projects.forEach { project ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedProjectId == project.id,
                                    onClick = { viewModel.selectProject(project.id) }
                                )
                                Text(text = project.name, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hideProjectPicker()
                        viewModel.showSnapshotDialog()
                    },
                    enabled = uiState.selectedProjectId != null
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideProjectPicker() }) { Text("Cancel") }
            }
        )
    }

    // Snapshot label dialog (outside project picker scope)
    if (uiState.showSnapshotDialog && uiState.selectedProjectId != null) {
        SnapshotDialog(
            onDismiss = { viewModel.hideSnapshotDialog() },
            onSave = { label -> viewModel.saveSnapshot(label, uiState.selectedProjectId!!) }
        )
    }
}

@Composable
private fun SpeedMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun PingMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun MiniLineChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier) {
        val maxVal = data.max()
        val minVal = data.min()
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val padding = 4f

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - padding - ((value - minVal) / range) * (size.height - 2 * padding)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )

        // Draw dots on last point
        if (data.isNotEmpty()) {
            val lastX = (data.size - 1) * stepX
            val lastY = size.height - padding - ((data.last() - minVal) / range) * (size.height - 2 * padding)
            drawCircle(color = color, radius = 4f, center = Offset(lastX, lastY))
        }
    }
}
