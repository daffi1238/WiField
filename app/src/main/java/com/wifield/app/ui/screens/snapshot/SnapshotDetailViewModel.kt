package com.wifield.app.ui.screens.snapshot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifield.app.data.local.WiFieldDatabase
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ActiveTestResultEntity
import com.wifield.app.data.local.entity.SnapshotEntity
import com.wifield.app.data.repository.SnapshotRepository
import com.wifield.app.domain.analyzer.AlertAnalyzer
import com.wifield.app.domain.model.Alert
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.WifiBand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SnapshotDetailUiState(
    val snapshot: SnapshotEntity? = null,
    val accessPoints: List<ScannedAccessPoint> = emptyList(),
    val activeTestResult: ActiveTestResultEntity? = null,
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = true
)

class SnapshotDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WiFieldDatabase.getInstance(application)
    private val snapshotRepository = SnapshotRepository(
        db.snapshotDao(), db.accessPointDao(), db.activeTestResultDao()
    )

    private val _uiState = MutableStateFlow(SnapshotDetailUiState())
    val uiState: StateFlow<SnapshotDetailUiState> = _uiState.asStateFlow()

    fun loadSnapshot(snapshotId: Long) {
        viewModelScope.launch {
            val snapshot = snapshotRepository.getSnapshotById(snapshotId)
            val aps = snapshotRepository.getAccessPointsBySnapshotOnce(snapshotId)
            val activeResult = snapshotRepository.getActiveTestResultOnce(snapshotId)

            val scannedAps = aps.map { it.toScannedAccessPoint() }
            val alerts = AlertAnalyzer.analyze(scannedAps)

            _uiState.value = SnapshotDetailUiState(
                snapshot = snapshot,
                accessPoints = scannedAps,
                activeTestResult = activeResult,
                alerts = alerts,
                isLoading = false
            )
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
