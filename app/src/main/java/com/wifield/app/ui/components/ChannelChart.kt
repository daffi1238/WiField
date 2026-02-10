package com.wifield.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifield.app.domain.model.ChannelInfo
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.WifiBand
import com.wifield.app.ui.theme.Band24Color
import com.wifield.app.ui.theme.Band5Color

@Composable
fun ChannelChart(
    accessPoints: List<ScannedAccessPoint>,
    band: WifiBand,
    modifier: Modifier = Modifier
) {
    val bandAps = accessPoints.filter { it.band == band }
    val channelGroups = bandAps.groupBy { it.channel }
        .map { (channel, aps) ->
            ChannelInfo(
                channel = channel,
                band = band,
                apCount = aps.size,
                accessPoints = aps
            )
        }
        .sortedBy { it.channel }

    val channels = when (band) {
        WifiBand.BAND_2_4_GHZ -> (1..14).toList()
        WifiBand.BAND_5_GHZ -> listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 149, 153, 157, 161, 165)
        WifiBand.BAND_6_GHZ -> (1..233 step 4).toList()
    }

    val barColor = if (band == WifiBand.BAND_2_4_GHZ) Band24Color else Band5Color
    val maxAps = (channelGroups.maxOfOrNull { it.apCount } ?: 1).coerceAtLeast(1)
    val channelMap = channelGroups.associateBy { it.channel }

    Column(modifier = modifier) {
        Text(
            text = band.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val density = LocalDensity.current
        val barWidth = 32.dp
        val totalWidth = barWidth * channels.size + 8.dp * (channels.size - 1)

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .width(totalWidth.coerceAtLeast(300.dp))
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            channels.forEach { channel ->
                val info = channelMap[channel]
                val count = info?.apCount ?: 0
                val heightFraction = if (maxAps > 0) count.toFloat() / maxAps else 0f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(barWidth)
                ) {
                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = barColor
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (count > 0) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                        ) {
                            val saturated = count > 3
                            val color = if (saturated) {
                                Color(0xFFFF9800).copy(alpha = 0.8f)
                            } else {
                                barColor.copy(alpha = 0.7f)
                            }
                            drawRoundRect(
                                color = color,
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
