package com.wifield.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AlertSeverity(val label: String, val color: Color) {
    INFO("Info", Color(0xFF2196F3)),
    WARNING("Warning", Color(0xFFFFC107)),
    CRITICAL("Crítico", Color(0xFFF44336))
}

enum class AlertType {
    WEAK_SIGNAL,
    SATURATED_CHANNEL,
    CHANNEL_OVERLAP,
    WEAK_SECURITY,
    SUBOPTIMAL_CHANNEL_WIDTH,
    EXCESSIVE_AP_DENSITY
}

data class Alert(
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val recommendation: String,
    val icon: ImageVector = when (type) {
        AlertType.WEAK_SIGNAL -> Icons.Filled.WifiOff
        AlertType.SATURATED_CHANNEL -> Icons.Filled.ViewColumn
        AlertType.CHANNEL_OVERLAP -> Icons.Filled.Layers
        AlertType.WEAK_SECURITY -> Icons.Filled.LockOpen
        AlertType.SUBOPTIMAL_CHANNEL_WIDTH -> Icons.Filled.SettingsEthernet
        AlertType.EXCESSIVE_AP_DENSITY -> Icons.Filled.CellTower
    }
)
