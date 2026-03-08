package com.wifield.app.ui.screens.snapshot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifield.app.domain.model.WifiBand
import com.wifield.app.ui.components.*
import com.wifield.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotDetailScreen(
    snapshotId: Long,
    onBackClick: () -> Unit,
    viewModel: SnapshotDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(snapshotId) {
        viewModel.loadSnapshot(snapshotId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.snapshot?.label ?: "Snapshot",
                            fontWeight = FontWeight.Bold
                        )
                        uiState.snapshot?.let {
                            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                            Text(
                                text = dateFormat.format(Date(it.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode indicator
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(
                            label = if (uiState.snapshot?.isActiveMode == true) "Active Mode" else "Passive Mode",
                            color = if (uiState.snapshot?.isActiveMode == true) SignalGood else MaterialTheme.colorScheme.secondary
                        )
                        InfoChip(label = "${uiState.accessPoints.size} APs detected")
                    }
                }

                // Active test results
                if (uiState.activeTestResult != null) {
                    item {
                        val result = uiState.activeTestResult!!
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Diagnostic results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MetricItem("Download", "${String.format("%.1f", result.downloadSpeed)} Mbps", SignalGood)
                                    MetricItem("Upload", "${String.format("%.1f", result.uploadSpeed)} Mbps", SignalExcellent)
                                    MetricItem("Link Speed", "${result.linkSpeed} Mbps", WiFieldSecondary)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MetricItem("Latency", "${String.format("%.1f", result.latency)} ms", SignalFair)
                                    MetricItem("Jitter", "${String.format("%.1f", result.jitter)} ms", SignalWeak)
                                    MetricItem("Loss", "${String.format("%.1f", result.packetLoss)}%",
                                        if (result.packetLoss > 5) SignalCritical else SignalExcellent)
                                    MetricItem("Gateway", "${String.format("%.1f", result.gatewayLatency)} ms", WiFieldSecondary)
                                }
                            }
                        }
                    }
                }

                // Alerts section
                if (uiState.alerts.isNotEmpty()) {
                    item {
                        Text("Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(uiState.alerts) { alert ->
                        AlertCard(alert = alert)
                    }
                }

                // Channel distribution
                item {
                    Text("Channel distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    ChannelChart(accessPoints = uiState.accessPoints, band = WifiBand.BAND_2_4_GHZ)
                }
                item {
                    ChannelChart(accessPoints = uiState.accessPoints, band = WifiBand.BAND_5_GHZ)
                }

                // AP list
                item {
                    Text("Access Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(uiState.accessPoints, key = { it.bssid }) { ap ->
                    AccessPointCard(ap = ap)
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
