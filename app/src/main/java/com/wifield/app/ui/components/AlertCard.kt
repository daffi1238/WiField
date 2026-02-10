package com.wifield.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifield.app.domain.model.Alert

@Composable
fun AlertCard(
    alert: Alert,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = alert.severity.color.copy(alpha = 0.1f)
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = alert.icon,
                    contentDescription = null,
                    tint = alert.severity.color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = alert.severity.color
                    )
                    Text(
                        text = alert.severity.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = alert.severity.color.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "Recomendación: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = alert.recommendation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertSummary(
    alerts: List<Alert>,
    modifier: Modifier = Modifier
) {
    val criticalCount = alerts.count { it.severity == com.wifield.app.domain.model.AlertSeverity.CRITICAL }
    val warningCount = alerts.count { it.severity == com.wifield.app.domain.model.AlertSeverity.WARNING }
    val infoCount = alerts.count { it.severity == com.wifield.app.domain.model.AlertSeverity.INFO }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (criticalCount > 0) {
            InfoChip(
                label = "$criticalCount Crítico${if (criticalCount > 1) "s" else ""}",
                color = com.wifield.app.ui.theme.AlertCritical
            )
        }
        if (warningCount > 0) {
            InfoChip(
                label = "$warningCount Warning${if (warningCount > 1) "s" else ""}",
                color = com.wifield.app.ui.theme.AlertWarning
            )
        }
        if (infoCount > 0) {
            InfoChip(
                label = "$infoCount Info",
                color = com.wifield.app.ui.theme.AlertInfo
            )
        }
        if (alerts.isEmpty()) {
            InfoChip(
                label = "Sin alertas",
                color = com.wifield.app.ui.theme.SignalExcellent
            )
        }
    }
}
