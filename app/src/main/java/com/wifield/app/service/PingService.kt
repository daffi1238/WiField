package com.wifield.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class PingResult(
    val latency: Double,       // average ms
    val jitter: Double,        // ms
    val packetLoss: Double,    // percentage
    val minLatency: Double,
    val maxLatency: Double,
    val individual: List<Double> // individual ping times
)

object PingService {

    suspend fun ping(
        host: String,
        count: Int = 20,
        timeoutMs: Int = 2000
    ): PingResult = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Double>()
        var lost = 0

        for (i in 0 until count) {
            try {
                val startTime = System.nanoTime()
                val reachable = InetAddress.getByName(host).isReachable(timeoutMs)
                val endTime = System.nanoTime()

                if (reachable) {
                    val latency = (endTime - startTime) / 1_000_000.0
                    latencies.add(latency)
                } else {
                    lost++
                }
            } catch (e: Exception) {
                lost++
            }
        }

        if (latencies.isEmpty()) {
            return@withContext PingResult(
                latency = 0.0,
                jitter = 0.0,
                packetLoss = 100.0,
                minLatency = 0.0,
                maxLatency = 0.0,
                individual = emptyList()
            )
        }

        val avgLatency = latencies.average()
        val minLat = latencies.min()
        val maxLat = latencies.max()

        // Jitter = average of absolute differences between consecutive latencies
        val jitter = if (latencies.size > 1) {
            latencies.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average()
        } else {
            0.0
        }

        val packetLoss = (lost.toDouble() / count) * 100.0

        PingResult(
            latency = avgLatency,
            jitter = jitter,
            packetLoss = packetLoss,
            minLatency = minLat,
            maxLatency = maxLat,
            individual = latencies
        )
    }

    suspend fun pingGateway(gatewayIp: String): PingResult = ping(gatewayIp, count = 10)

    suspend fun pingExternal(): PingResult = ping("8.8.8.8", count = 20)
}
