package com.wifield.app.ui.screens.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifield.app.data.local.WiFieldDatabase
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ProjectEntity
import com.wifield.app.data.local.entity.SnapshotEntity
import com.wifield.app.data.repository.ProjectRepository
import com.wifield.app.data.repository.SnapshotRepository
import com.wifield.app.domain.analyzer.AlertAnalyzer
import com.wifield.app.domain.model.Alert
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.SignalQuality
import com.wifield.app.domain.model.WifiBand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SnapshotSummary(
    val snapshot: SnapshotEntity,
    val apCount: Int,
    val bestRssi: Int,
    val quality: SignalQuality,
    val alerts: List<Alert>
)

data class ProjectUiState(
    val project: ProjectEntity? = null,
    val snapshots: List<SnapshotSummary> = emptyList(),
    val isLoading: Boolean = true,
    val allAlerts: List<Alert> = emptyList()
)

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WiFieldDatabase.getInstance(application)
    private val projectRepository = ProjectRepository(db.projectDao())
    private val snapshotRepository = SnapshotRepository(
        db.snapshotDao(), db.accessPointDao(), db.activeTestResultDao()
    )

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            _uiState.value = _uiState.value.copy(project = project)

            snapshotRepository.getSnapshotsByProject(projectId).collect { snapshots ->
                val summaries = snapshots.map { snapshot ->
                    val aps = snapshotRepository.getAccessPointsBySnapshotOnce(snapshot.id)
                    val scannedAps = aps.map { it.toScannedAccessPoint() }
                    val alerts = AlertAnalyzer.analyze(scannedAps)
                    val bestRssi = aps.maxOfOrNull { it.rssi } ?: -100

                    SnapshotSummary(
                        snapshot = snapshot,
                        apCount = aps.size,
                        bestRssi = bestRssi,
                        quality = SignalQuality.fromRssi(bestRssi),
                        alerts = alerts
                    )
                }

                val allAlerts = summaries.flatMap { it.alerts }.distinctBy { it.title }

                _uiState.value = _uiState.value.copy(
                    snapshots = summaries,
                    isLoading = false,
                    allAlerts = allAlerts
                )
            }
        }
    }

    fun deleteSnapshot(snapshotId: Long) {
        viewModelScope.launch {
            snapshotRepository.deleteSnapshot(snapshotId)
        }
    }

    private fun AccessPointEntity.toScannedAccessPoint() = ScannedAccessPoint(
        ssid = ssid,
        bssid = bssid,
        rssi = rssi,
        channel = channel,
        frequency = frequency,
        band = WifiBand.fromFrequency(frequency),
        channelWidth = channelWidth,
        security = security,
        wpsEnabled = wpsEnabled,
        isConnected = isConnected
    )
}
