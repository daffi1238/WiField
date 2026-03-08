package com.wifield.app.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "PingService"

data class PingResult(
    val latency: Double,       // average ms
    val jitter: Double,        // ms
    val packetLoss: Double,    // percentage
    val minLatency: Double,
    val maxLatency: Double,
    val individual: List<Double> // individual ping times
)

data class PingProgress(
    val sent: Int,
    val total: Int,
    val currentLatency: Double,
    val averageLatency: Double
)

object PingService {

    /**
     * Ping using system ping command (ICMP) for accurate results.
     * Falls back to InetAddress.isReachable() if ping command fails.
     */
    suspend fun ping(
        host: String,
        count: Int = 20,
        timeoutMs: Int = 2000
    ): PingResult = withContext(Dispatchers.IO) {
        // Try system ping first (ICMP - more accurate)
        try {
            val result = systemPing(host, count, timeoutMs / 1000)
            if (result != null) return@withContext result
        } catch (e: Exception) {
            Log.w(TAG, "System ping failed, falling back to Java ping", e)
        }

        // Fallback to Java ping
        javaPing(host, count, timeoutMs)
    }

    /**
     * Ping with progress updates via Flow.
     */
    fun pingWithProgress(
        host: String,
        count: Int = 20,
        timeoutMs: Int = 2000
    ): Flow<PingProgress> = flow {
        val latencies = mutableListOf<Double>()

        for (i in 1..count) {
            val latency = singlePing(host, timeoutMs / 1000)
            if (latency != null) {
                latencies.add(latency)
            }
            emit(PingProgress(
                sent = i,
                total = count,
                currentLatency = latency ?: -1.0,
                averageLatency = if (latencies.isNotEmpty()) latencies.average() else 0.0
            ))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Single ICMP ping using system command, returns latency in ms or null if failed.
     */
    private fun singlePing(host: String, timeoutSec: Int): Double? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-W", "$timeoutSec", host))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var latency: Double? = null

            reader.forEachLine { line ->
                // Parse "time=12.3 ms" from ping output
                val timeMatch = Regex("time=(\\d+\\.?\\d*)\\s*ms").find(line)
                if (timeMatch != null) {
                    latency = timeMatch.groupValues[1].toDoubleOrNull()
                }
            }

            process.waitFor()
            reader.close()
            latency
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Uses system ping command for ICMP-based measurement.
     */
    private fun systemPing(host: String, count: Int, timeoutSec: Int): PingResult? {
        val latencies = mutableListOf<Double>()
        var lost = 0

        for (i in 0 until count) {
            val latency = singlePing(host, timeoutSec)
            if (latency != null) {
                latencies.add(latency)
            } else {
                lost++
            }
        }

        if (latencies.isEmpty()) return null

        return buildPingResult(latencies, lost, count)
    }

    /**
     * Fallback using Java InetAddress.isReachable (TCP-based, less accurate).
     */
    private fun javaPing(host: String, count: Int, timeoutMs: Int): PingResult {
        val latencies = mutableListOf<Double>()
        var lost = 0

        for (i in 0 until count) {
            try {
                val startTime = System.nanoTime()
                val reachable = java.net.InetAddress.getByName(host).isReachable(timeoutMs)
                val endTime = System.nanoTime()

                if (reachable) {
                    latencies.add((endTime - startTime) / 1_000_000.0)
                } else {
                    lost++
                }
            } catch (_: Exception) {
                lost++
            }
        }

        return buildPingResult(latencies, lost, count)
    }

    private fun buildPingResult(latencies: List<Double>, lost: Int, total: Int): PingResult {
        if (latencies.isEmpty()) {
            return PingResult(0.0, 0.0, 100.0, 0.0, 0.0, emptyList())
        }

        val avg = latencies.average()
        val jitter = if (latencies.size > 1) {
            latencies.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average()
        } else 0.0

        return PingResult(
            latency = avg,
            jitter = jitter,
            packetLoss = (lost.toDouble() / total) * 100.0,
            minLatency = latencies.min(),
            maxLatency = latencies.max(),
            individual = latencies
        )
    }

    suspend fun pingGateway(gatewayIp: String): PingResult = ping(gatewayIp, count = 10)

    suspend fun pingExternal(): PingResult = ping("8.8.8.8", count = 20)
}
