package com.wifield.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.SignalQuality
import com.wifield.app.ui.theme.*

@Composable
fun AccessPointCard(
    ap: ScannedAccessPoint,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val quality = SignalQuality.fromRssi(ap.rssi)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ap.isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = { onClick?.invoke() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (ap.isConnected) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Connected",
                            tint = SignalExcellent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = ap.ssid.ifEmpty { "(Hidden)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                SignalStrengthIndicator(rssi = ap.rssi)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(label = ap.band.label, color = when (ap.band.label) {
                    "2.4 GHz" -> Band24Color
                    "5 GHz" -> Band5Color
                    else -> Band6Color
                })
                InfoChip(label = "CH ${ap.channel}")
                InfoChip(label = "${ap.channelWidth} MHz")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ap.security == "Open") Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Security",
                        modifier = Modifier.size(14.dp),
                        tint = if (ap.security == "Open" || ap.security == "WEP") SignalCritical
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ap.security,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (ap.wpsEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "WPS",
                            modifier = Modifier.size(14.dp),
                            tint = SignalWeak
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "WPS",
                            style = MaterialTheme.typography.bodySmall,
                            color = SignalWeak
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = ap.bssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
