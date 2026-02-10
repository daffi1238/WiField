package com.wifield.app.ui.screens.comparator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wifield.app.domain.model.SignalQuality
import com.wifield.app.ui.components.InfoChip
import com.wifield.app.ui.components.SignalStrengthBar
import com.wifield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparatorScreen(
    projectId: Long,
    onBackClick: () -> Unit,
    viewModel: ComparatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.loadSnapshots(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparador", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showPicker() }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar snapshot")
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
        } else if (uiState.selectedSnapshots.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Selecciona snapshots para comparar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.showPicker() }) {
                        Text("Seleccionar")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row with snapshot names
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.selectedSnapshots.forEach { comparison ->
                            val quality = SignalQuality.fromRssi(comparison.bestRssi)
                            Card(
                                modifier = Modifier.width(150.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = quality.color.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = comparison.snapshot.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${comparison.bestRssi} dBm",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = quality.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = quality.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = quality.color
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${comparison.accessPoints.size} APs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Signal comparison
                item {
                    Text("Comparación de señal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.selectedSnapshots.forEach { comparison ->
                                Text(
                                    text = comparison.snapshot.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SignalStrengthBar(rssi = comparison.bestRssi)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                // Active test comparison if any have active results
                val activeComparisons = uiState.selectedSnapshots.filter { it.activeTestResult != null }
                if (activeComparisons.isNotEmpty()) {
                    item {
                        Text("Comparación de rendimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Table header
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("Ubicación", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Down", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Up", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Ping", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("Loss", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                activeComparisons.forEach { comparison ->
                                    val r = comparison.activeTestResult!!
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text(comparison.snapshot.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        Text("${String.format("%.1f", r.downloadSpeed)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                        Text("${String.format("%.1f", r.uploadSpeed)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                        Text("${String.format("%.0f", r.latency)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                        Text("${String.format("%.1f", r.packetLoss)}%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }

                // Common SSIDs across snapshots
                item {
                    Text("SSIDs comunes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                val allSsids = uiState.selectedSnapshots.flatMap { it.accessPoints.map { ap -> ap.ssid } }.distinct().sorted()
                items(allSsids.filter { it.isNotEmpty() }) { ssid ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = ssid,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.selectedSnapshots.forEach { comparison ->
                                    val ap = comparison.accessPoints
                                        .filter { it.ssid == ssid }
                                        .maxByOrNull { it.rssi }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = comparison.snapshot.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (ap != null) {
                                            val quality = SignalQuality.fromRssi(ap.rssi)
                                            Text(
                                                text = "${ap.rssi} dBm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = quality.color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "N/D",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Snapshot picker dialog
    if (uiState.showPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.hidePicker() },
            title = { Text("Seleccionar snapshots") },
            text = {
                Column {
                    Text(
                        "Selecciona los snapshots que quieres comparar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.availableSnapshots.forEach { snapshot ->
                        val isSelected = uiState.selectedSnapshots.any { it.snapshot.id == snapshot.id }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleSnapshot(snapshot) }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = snapshot.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (snapshot.isActiveMode) "Modo activo" else "Modo pasivo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.hidePicker() }) {
                    Text("Listo")
                }
            }
        )
    }
}
