package com.wifield.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wifield.app.domain.model.SignalQuality

@Composable
fun SignalStrengthIndicator(
    rssi: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val quality = SignalQuality.fromRssi(rssi)
    val barCount = 4
    val activeBars = when (quality) {
        SignalQuality.EXCELLENT -> 4
        SignalQuality.GOOD -> 3
        SignalQuality.FAIR -> 2
        SignalQuality.WEAK -> 1
        SignalQuality.CRITICAL -> 1
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 28.dp, height = 20.dp)) {
            val barWidth = size.width / (barCount * 2 - 1)
            val maxHeight = size.height

            for (i in 0 until barCount) {
                val barHeight = maxHeight * (i + 1) / barCount
                val x = i * barWidth * 2
                val y = maxHeight - barHeight
                val color = if (i < activeBars) quality.color else Color.Gray.copy(alpha = 0.3f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$rssi dBm",
                style = MaterialTheme.typography.labelSmall,
                color = quality.color
            )
        }
    }
}

@Composable
fun SignalStrengthBar(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val quality = SignalQuality.fromRssi(rssi)
    // Map RSSI to 0-100 percentage (-100 to -30 range)
    val percentage = ((rssi + 100).toFloat() / 70f).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = quality.label,
                style = MaterialTheme.typography.labelSmall,
                color = quality.color
            )
            Text(
                text = "$rssi dBm",
                style = MaterialTheme.typography.labelSmall,
                color = quality.color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.3f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = quality.color,
                size = Size(size.width * percentage, size.height),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}
