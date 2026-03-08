package com.wifield.app.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "SpeedTest"

data class SpeedTestResult(
    val downloadSpeed: Double,  // Mbps
    val uploadSpeed: Double     // Mbps
)

data class SpeedTestProgress(
    val phase: SpeedTestPhase,
    val progressPercent: Int,
    val currentSpeed: Double // Mbps
)

enum class SpeedTestPhase {
    IDLE, DOWNLOAD, UPLOAD, COMPLETE, ERROR
}

object SpeedTestService {

    private const val PARALLEL_CONNECTIONS = 4
    private const val BUFFER_SIZE = 64 * 1024
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 30_000
    private const val UPLOAD_CHUNK_SIZE = 5 * 1024 * 1024

    private val DOWNLOAD_URLS = listOf(
        "http://speedtest.tele2.net/10MB.zip",
        "http://proof.ovh.net/files/10Mb.dat",
        "http://ipv4.download.thinkbroadband.com/10MB.zip"
    )

    private val UPLOAD_URLS = listOf(
        "http://speedtest.tele2.net/upload.php",
        "http://ipv4.download.thinkbroadband.com/upload.php"
    )

    fun runSpeedTest(): Flow<SpeedTestProgress> = flow {
        emit(SpeedTestProgress(SpeedTestPhase.DOWNLOAD, 0, 0.0))

        val downloadSpeed = try {
            measureDownloadParallel { progress, speed ->
                emit(SpeedTestProgress(SpeedTestPhase.DOWNLOAD, progress, speed))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download test failed", e)
            0.0
        }

        emit(SpeedTestProgress(SpeedTestPhase.UPLOAD, 0, 0.0))

        val uploadSpeed = try {
            measureUploadParallel { progress, speed ->
                emit(SpeedTestProgress(SpeedTestPhase.UPLOAD, progress, speed))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload test failed", e)
            0.0
        }

        emit(SpeedTestProgress(SpeedTestPhase.COMPLETE, 100, 0.0))
    }.flowOn(Dispatchers.IO)

    suspend fun runDownloadOnly(): Double = withContext(Dispatchers.IO) {
        try { measureDownloadParallel { _, _ -> } } catch (e: Exception) { 0.0 }
    }

    suspend fun runUploadOnly(): Double = withContext(Dispatchers.IO) {
        try { measureUploadParallel { _, _ -> } } catch (e: Exception) { 0.0 }
    }

    private fun findWorkingUrl(urls: List<String>): String? {
        for (urlString in urls) {
            try {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.requestMethod = "HEAD"
                connection.connect()
                val code = connection.responseCode
                connection.disconnect()
                if (code in 200..299 || code == 405) {
                    Log.d(TAG, "Working URL: $urlString ($code)")
                    return urlString
                }
            } catch (_: Exception) { continue }
        }
        return null
    }

    private suspend fun measureDownloadParallel(
        onProgress: suspend (Int, Double) -> Unit
    ): Double {
        val urlString = findWorkingUrl(DOWNLOAD_URLS) ?: return 0.0
        val totalBytes = AtomicLong(0L)
        val startTime = System.nanoTime()

        // Launch download threads and poll progress from the flow's coroutine
        coroutineScope {
            val jobs = (1..PARALLEL_CONNECTIONS).map {
                async(Dispatchers.IO) { downloadStream(urlString, totalBytes) }
            }

            // Poll progress from this coroutine (same context as flow)
            while (jobs.any { it.isActive }) {
                delay(400)
                val bytes = totalBytes.get()
                val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
                if (elapsed > 0 && bytes > 0) {
                    val speedMbps = (bytes * 8.0) / (elapsed * 1_000_000)
                    onProgress(minOf((elapsed * 10).toInt(), 95), speedMbps)
                }
            }

            jobs.awaitAll()
        }

        val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
        val bytes = totalBytes.get()
        val speedMbps = if (elapsed > 0) (bytes * 8.0) / (elapsed * 1_000_000) else 0.0
        Log.d(TAG, "Download: ${bytes / 1024}KB in ${String.format("%.1f", elapsed)}s = ${String.format("%.1f", speedMbps)} Mbps")
        onProgress(100, speedMbps)
        return speedMbps
    }

    private fun downloadStream(urlString: String, counter: AtomicLong) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.requestMethod = "GET"
        try {
            connection.connect()
            val inputStream = connection.inputStream
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                counter.addAndGet(bytesRead.toLong())
            }
            inputStream.close()
        } catch (e: Exception) {
            Log.w(TAG, "Download stream error: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun measureUploadParallel(
        onProgress: suspend (Int, Double) -> Unit
    ): Double {
        val uploadUrl = findWorkingUploadUrl() ?: return 0.0
        val totalBytes = AtomicLong(0L)
        val startTime = System.nanoTime()

        coroutineScope {
            val jobs = (1..PARALLEL_CONNECTIONS).map {
                async(Dispatchers.IO) { uploadStream(uploadUrl, UPLOAD_CHUNK_SIZE, totalBytes) }
            }

            while (jobs.any { it.isActive }) {
                delay(400)
                val bytes = totalBytes.get()
                val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
                if (elapsed > 0 && bytes > 0) {
                    val speedMbps = (bytes * 8.0) / (elapsed * 1_000_000)
                    onProgress(minOf((elapsed * 10).toInt(), 95), speedMbps)
                }
            }

            jobs.awaitAll()
        }

        val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
        val bytes = totalBytes.get()
        val speedMbps = if (elapsed > 0 && bytes > 0) (bytes * 8.0) / (elapsed * 1_000_000) else 0.0
        Log.d(TAG, "Upload: ${bytes / 1024}KB in ${String.format("%.1f", elapsed)}s = ${String.format("%.1f", speedMbps)} Mbps")
        onProgress(100, speedMbps)
        return speedMbps
    }

    private fun findWorkingUploadUrl(): String? {
        for (urlString in UPLOAD_URLS) {
            try {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.doOutput = true
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/octet-stream")
                connection.setFixedLengthStreamingMode(1024)
                val os = connection.outputStream
                os.write(ByteArray(1024))
                os.flush()
                os.close()
                val code = connection.responseCode
                connection.disconnect()
                if (code in 200..299) {
                    Log.d(TAG, "Working upload URL: $urlString")
                    return urlString
                }
            } catch (_: Exception) { continue }
        }
        return null
    }

    private fun uploadStream(urlString: String, size: Int, counter: AtomicLong) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        connection.setChunkedStreamingMode(BUFFER_SIZE)
        try {
            val outputStream: OutputStream = connection.outputStream
            val chunk = ByteArray(BUFFER_SIZE)
            var bytesSent = 0
            while (bytesSent < size) {
                val toSend = minOf(BUFFER_SIZE, size - bytesSent)
                outputStream.write(chunk, 0, toSend)
                outputStream.flush()
                bytesSent += toSend
                counter.addAndGet(toSend.toLong())
            }
            outputStream.close()
            connection.responseCode
        } catch (e: Exception) {
            Log.w(TAG, "Upload stream error: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }
}
