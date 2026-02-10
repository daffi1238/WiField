package com.wifield.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

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

    // Public speed test file URLs (various sizes)
    private val DOWNLOAD_URLS = listOf(
        "http://speedtest.tele2.net/10MB.zip",
        "http://proof.ovh.net/files/10Mb.dat",
        "http://ipv4.download.thinkbroadband.com/10MB.zip"
    )

    private const val UPLOAD_URL = "http://speedtest.tele2.net/upload.php"

    fun runSpeedTest(): Flow<SpeedTestProgress> = flow {
        emit(SpeedTestProgress(SpeedTestPhase.DOWNLOAD, 0, 0.0))

        // Download test
        val downloadSpeed = try {
            measureDownload { progress, speed ->
                emit(SpeedTestProgress(SpeedTestPhase.DOWNLOAD, progress, speed))
            }
        } catch (e: Exception) {
            0.0
        }

        emit(SpeedTestProgress(SpeedTestPhase.UPLOAD, 0, 0.0))

        // Upload test
        val uploadSpeed = try {
            measureUpload { progress, speed ->
                emit(SpeedTestProgress(SpeedTestPhase.UPLOAD, progress, speed))
            }
        } catch (e: Exception) {
            0.0
        }

        emit(SpeedTestProgress(SpeedTestPhase.COMPLETE, 100, 0.0))
    }.flowOn(Dispatchers.IO)

    suspend fun runDownloadOnly(): Double = withContext(Dispatchers.IO) {
        try {
            measureDownload { _, _ -> }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun runUploadOnly(): Double = withContext(Dispatchers.IO) {
        try {
            measureUpload { _, _ -> }
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun measureDownload(
        onProgress: suspend (Int, Double) -> Unit
    ): Double {
        for (url in DOWNLOAD_URLS) {
            try {
                return downloadFromUrl(url, onProgress)
            } catch (e: Exception) {
                continue
            }
        }
        return 0.0
    }

    private suspend fun downloadFromUrl(
        urlString: String,
        onProgress: suspend (Int, Double) -> Unit
    ): Double {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.requestMethod = "GET"

        try {
            connection.connect()

            val totalBytes = connection.contentLength.toLong()
            val inputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            val startTime = System.nanoTime()
            var lastReportTime = startTime

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead

                val currentTime = System.nanoTime()
                val elapsedSinceReport = (currentTime - lastReportTime) / 1_000_000_000.0

                if (elapsedSinceReport >= 0.5) {
                    val elapsedTotal = (currentTime - startTime) / 1_000_000_000.0
                    val speedMbps = if (elapsedTotal > 0) {
                        (totalBytesRead * 8.0) / (elapsedTotal * 1_000_000)
                    } else 0.0

                    val progress = if (totalBytes > 0) {
                        ((totalBytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                    } else 50

                    onProgress(progress, speedMbps)
                    lastReportTime = currentTime
                }
            }

            inputStream.close()
            val endTime = System.nanoTime()
            val elapsed = (endTime - startTime) / 1_000_000_000.0

            return if (elapsed > 0) {
                (totalBytesRead * 8.0) / (elapsed * 1_000_000)
            } else 0.0
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun measureUpload(
        onProgress: suspend (Int, Double) -> Unit
    ): Double {
        val uploadSize = 2 * 1024 * 1024 // 2 MB
        val data = ByteArray(uploadSize)

        try {
            val url = URL(UPLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(uploadSize)

            val startTime = System.nanoTime()
            val outputStream: OutputStream = connection.outputStream
            val chunkSize = 8192
            var bytesSent = 0
            var lastReportTime = startTime

            while (bytesSent < uploadSize) {
                val remaining = uploadSize - bytesSent
                val toSend = minOf(chunkSize, remaining)
                outputStream.write(data, bytesSent, toSend)
                bytesSent += toSend

                val currentTime = System.nanoTime()
                val elapsedSinceReport = (currentTime - lastReportTime) / 1_000_000_000.0

                if (elapsedSinceReport >= 0.5) {
                    val elapsedTotal = (currentTime - startTime) / 1_000_000_000.0
                    val speedMbps = if (elapsedTotal > 0) {
                        (bytesSent * 8.0) / (elapsedTotal * 1_000_000)
                    } else 0.0

                    val progress = ((bytesSent * 100) / uploadSize).coerceIn(0, 100)
                    onProgress(progress, speedMbps)
                    lastReportTime = currentTime
                }
            }

            outputStream.flush()
            outputStream.close()
            connection.responseCode // trigger the upload

            val endTime = System.nanoTime()
            val elapsed = (endTime - startTime) / 1_000_000_000.0

            connection.disconnect()

            return if (elapsed > 0) {
                (bytesSent * 8.0) / (elapsed * 1_000_000)
            } else 0.0
        } catch (e: Exception) {
            return 0.0
        }
    }
}
