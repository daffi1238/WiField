package com.wifield.app.ui.screens.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.wifield.app.ui.theme.SignalExcellent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scanner WiFi", fontWeight = FontWeight.Bold)
                        if (uiState.lastScanTime > 0) {
                            Text(
                                text = "${uiState.accessPoints.size} APs - Último: ${dateFormat.format(Date(uiState.lastScanTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setAutoRefresh(!uiState.autoRefresh) }) {
                        Icon(
                            imageVector = if (uiState.autoRefresh) Icons.Default.Sync else Icons.Default.SyncProblem,
                            contentDescription = "Auto-refresh",
                            tint = if (uiState.autoRefresh) SignalExcellent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { viewModel.triggerScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Escanear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (uiState.accessPoints.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showProjectPicker() },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text("Snapshot") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Alerts summary
            if (uiState.alerts.isNotEmpty()) {
                AlertSummary(
                    alerts = uiState.alerts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == ScannerTab.LIST,
                    onClick = { viewModel.selectTab(ScannerTab.LIST) },
                    text = { Text("Lista") },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == ScannerTab.GROUPS,
                    onClick = { viewModel.selectTab(ScannerTab.GROUPS) },
                    text = { Text("SSID") },
                    icon = { Icon(Icons.Default.GroupWork, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == ScannerTab.CHANNELS,
                    onClick = { viewModel.selectTab(ScannerTab.CHANNELS) },
                    text = { Text("Canales") },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (uiState.selectedTab) {
                ScannerTab.LIST -> {
                    if (uiState.accessPoints.isEmpty() && !uiState.isScanning) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Escaneando redes...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.accessPoints, key = { it.bssid }) { ap ->
                                AccessPointCard(ap = ap)
                            }
                        }
                    }
                }

                ScannerTab.GROUPS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.ssidGroups, key = { it.ssid }) { group ->
                            SsidGroupCard(group = group)
                        }
                    }
                }

                ScannerTab.CHANNELS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ChannelChart(
                            accessPoints = uiState.accessPoints,
                            band = WifiBand.BAND_2_4_GHZ
                        )
                        ChannelChart(
                            accessPoints = uiState.accessPoints,
                            band = WifiBand.BAND_5_GHZ
                        )

                        // Alerts section
                        if (uiState.alerts.isNotEmpty()) {
                            Text(
                                text = "Alertas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            uiState.alerts.forEach { alert ->
                                AlertCard(alert = alert)
                            }
                        }
                    }
                }
            }
        }
    }

    // Project picker dialog
    if (uiState.showProjectPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.hideProjectPicker() },
            title = { Text("Seleccionar proyecto") },
            text = {
                if (uiState.projects.isEmpty()) {
                    Text("No hay proyectos. Crea uno desde la pantalla de inicio.")
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
                                Text(
                                    text = project.name,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
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
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideProjectPicker() }) {
                    Text("Cancelar")
                }
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
fun SsidGroupCard(
    group: com.wifield.app.domain.model.SsidGroup,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.ssid.ifEmpty { "(Oculto)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${group.apCount} AP${if (group.apCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        group.bands.forEach { band ->
                            InfoChip(label = band.label)
                        }
                    }
                }
                SignalStrengthIndicator(rssi = group.bestRssi)
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    group.accessPoints.sortedByDescending { it.rssi }.forEach { ap ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = ap.bssid,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("CH ${ap.channel}", style = MaterialTheme.typography.labelSmall)
                                    Text("${ap.channelWidth}MHz", style = MaterialTheme.typography.labelSmall)
                                    Text(ap.security, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            SignalStrengthIndicator(rssi = ap.rssi)
                        }
                        if (ap != group.accessPoints.last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
