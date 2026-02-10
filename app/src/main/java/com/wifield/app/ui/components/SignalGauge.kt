package com.wifield.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifield.app.domain.model.SignalQuality
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SignalGauge(
    rssi: Int,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val quality = SignalQuality.fromRssi(rssi)
    // Map RSSI from -100..-30 to 0..1
    val progress = ((rssi + 100f) / 70f).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val strokeWidth = 12f
                val startAngle = 135f
                val sweepAngle = 270f

                // Background arc
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Colored segments
                val segments = listOf(
                    0f to 0.15f to Color(0xFFF44336),
                    0.15f to 0.30f to Color(0xFFFF9800),
                    0.30f to 0.55f to Color(0xFFFFC107),
                    0.55f to 0.75f to Color(0xFF2196F3),
                    0.75f to 1f to Color(0xFF4CAF50)
                )

                for ((range, color) in segments) {
                    val (start, end) = range
                    val segStart = startAngle + sweepAngle * start
                    val segSweep = sweepAngle * (end - start).coerceAtMost(progress - start).coerceAtLeast(0f)
                    if (progress > start) {
                        drawArc(
                            color = color,
                            startAngle = segStart,
                            sweepAngle = segSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Needle indicator
                val needleAngle = startAngle + sweepAngle * progress
                val needleRadians = needleAngle * PI.toFloat() / 180f
                val radius = size.minDimension / 2 - strokeWidth
                val needleLength = radius * 0.7f
                val centerX = size.width / 2
                val centerY = size.height / 2

                drawLine(
                    color = quality.color,
                    start = Offset(centerX, centerY),
                    end = Offset(
                        centerX + needleLength * cos(needleRadians),
                        centerY + needleLength * sin(needleRadians)
                    ),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = quality.color,
                    radius = 6f,
                    center = Offset(centerX, centerY)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 16.dp)
            ) {
                Text(
                    text = "$rssi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = quality.color
                )
                Text(
                    text = "dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = quality.color,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = quality.label,
            style = MaterialTheme.typography.bodySmall,
            color = quality.color.copy(alpha = 0.7f)
        )
    }
}

private infix fun <A, B, C> Pair<A, B>.to(that: C): Pair<Pair<A, B>, C> = Pair(this, that)
