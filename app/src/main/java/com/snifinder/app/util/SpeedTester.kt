package com.snifinder.app.util

import com.snifinder.app.model.ConfigData
import com.snifinder.app.model.SpeedResult
import com.snifinder.app.model.SpeedStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * SpeedTester - Tests real download/upload speed for each SNI.
 *
 * How it works:
 * 1. Replaces SNI in the config
 * 2. Connects to the server with that SNI via TLS
 * 3. Sends HTTP request through the TLS tunnel
 * 4. Measures actual bytes transferred and calculates speed
 */
object SpeedTester {

    // Test URLs for download measurement
    private const val DOWNLOAD_HOST = "speed.cloudflare.com"
    private const val DOWNLOAD_PATH = "/__down?bytes=524288" // 512KB
    private const val UPLOAD_SIZE = 131072 // 128KB upload test

    /**
     * Full speed test: connects with given SNI, measures download and upload
     */
    suspend fun testSpeed(
        config: ConfigData,
        sni: String,
        timeoutMs: Int = 15000
    ): SpeedResult = withContext(Dispatchers.IO) {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(TrustAllManager()), null)
            val factory: SSLSocketFactory = sslContext.socketFactory

            // === Step 1: TLS Handshake with this SNI ===
            val handshakeStart = System.currentTimeMillis()

            val socket = factory.createSocket() as SSLSocket
            socket.soTimeout = timeoutMs
            socket.tcpNoDelay = true

            // Set SNI on the TLS connection
            val sslParams = socket.sslParameters
            sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
            socket.sslParameters = sslParams

            // Connect to the actual server from config
            socket.connect(InetSocketAddress(config.server, config.port), timeoutMs)
            socket.startHandshake()

            val handshakeMs = System.currentTimeMillis() - handshakeStart

            // === Step 2: Download Speed Test ===
            val downloadResult = measureDownload(socket, sni, timeoutMs)
            socket.close()

            // === Step 3: Upload Speed Test (new connection) ===
            val uploadResult = measureUpload(factory, config, sni, timeoutMs)

            SpeedResult(
                sni = sni,
                handshakeMs = handshakeMs,
                downloadSpeed = downloadResult.first,   // KB/s
                downloadBytes = downloadResult.second,
                downloadTimeMs = downloadResult.third,
                uploadSpeed = uploadResult,             // KB/s
                status = when {
                    downloadResult.first > 0 || uploadResult > 0 -> SpeedStatus.SUCCESS
                    handshakeMs > 0 -> SpeedStatus.NO_DATA
                    else -> SpeedStatus.FAILED
                }
            )

        } catch (e: java.net.SocketTimeoutException) {
            SpeedResult(sni = sni, handshakeMs = -1, downloadSpeed = 0.0,
                downloadBytes = 0, downloadTimeMs = -1, uploadSpeed = 0.0, status = SpeedStatus.TIMEOUT)
        } catch (e: javax.net.ssl.SSLException) {
            SpeedResult(sni = sni, handshakeMs = -1, downloadSpeed = 0.0,
                downloadBytes = 0, downloadTimeMs = -1, uploadSpeed = 0.0, status = SpeedStatus.FAILED)
        } catch (e: Exception) {
            SpeedResult(sni = sni, handshakeMs = -1, downloadSpeed = 0.0,
                downloadBytes = 0, downloadTimeMs = -1, uploadSpeed = 0.0, status = SpeedStatus.FAILED)
        }
    }

    /**
     * Measures download speed through established TLS socket
     * Returns Triple(speedKBps, totalBytes, timeMs)
     */
    private fun measureDownload(
        socket: SSLSocket,
        sni: String,
        timeoutMs: Int
    ): Triple<Double, Long, Long> {
        return try {
            val output = socket.outputStream
            val input = socket.inputStream

            // Send HTTP GET through the tunnel to download data
            val request = buildString {
                append("GET / HTTP/1.1\r\n")
                append("Host: $sni\r\n")
                append("Accept: */*\r\n")
                append("Connection: keep-alive\r\n")
                append("\r\n")
            }
            output.write(request.toByteArray(Charsets.UTF_8))
            output.flush()

            // Read and measure
            val buffer = ByteArray(16384)
            var totalBytes = 0L
            val startTime = System.currentTimeMillis()
            val deadline = startTime + (timeoutMs / 2)

            while (System.currentTimeMillis() < deadline) {
                val available = input.available()
                if (available > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size, available))
                    if (read == -1) break
                    totalBytes += read
                } else {
                    // Try one blocking read with short timeout
                    try {
                        socket.soTimeout = 2000
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalBytes += read
                    } catch (e: java.net.SocketTimeoutException) {
                        break // No more data
                    }
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            val speedKBps = if (elapsed > 0 && totalBytes > 0) {
                (totalBytes.toDouble() / 1024.0) / (elapsed.toDouble() / 1000.0)
            } else 0.0

            Triple(speedKBps, totalBytes, elapsed)
        } catch (e: Exception) {
            Triple(0.0, 0L, 0L)
        }
    }

    /**
     * Measures upload speed with a new connection
     * Returns upload speed in KB/s
     */
    private fun measureUpload(
        factory: SSLSocketFactory,
        config: ConfigData,
        sni: String,
        timeoutMs: Int
    ): Double {
        return try {
            val socket = factory.createSocket() as SSLSocket
            socket.soTimeout = timeoutMs
            socket.tcpNoDelay = true

            val sslParams = socket.sslParameters
            sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
            socket.sslParameters = sslParams

            socket.connect(InetSocketAddress(config.server, config.port), timeoutMs)
            socket.startHandshake()

            val output = socket.outputStream

            // Send HTTP POST with body for upload test
            val header = buildString {
                append("POST / HTTP/1.1\r\n")
                append("Host: $sni\r\n")
                append("Content-Type: application/octet-stream\r\n")
                append("Content-Length: $UPLOAD_SIZE\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            output.write(header.toByteArray(Charsets.UTF_8))

            // Write upload data and measure
            val chunk = ByteArray(8192) { 0x58 } // 'X' bytes
            var totalUploaded = 0L
            val startTime = System.currentTimeMillis()

            while (totalUploaded < UPLOAD_SIZE) {
                val toWrite = minOf(chunk.size.toLong(), UPLOAD_SIZE - totalUploaded).toInt()
                output.write(chunk, 0, toWrite)
                totalUploaded += toWrite
            }
            output.flush()

            val elapsed = System.currentTimeMillis() - startTime
            socket.close()

            if (elapsed > 0 && totalUploaded > 0) {
                (totalUploaded.toDouble() / 1024.0) / (elapsed.toDouble() / 1000.0)
            } else 0.0

        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Tests all SNIs in parallel (3 concurrent for speed tests)
     */
    suspend fun testAllSnis(
        config: ConfigData,
        sniList: List<String>,
        timeoutMs: Int = 15000,
        concurrency: Int = 3,
        onProgress: (SpeedResult) -> Unit = {}
    ): List<SpeedResult> = coroutineScope {
        val results = mutableListOf<SpeedResult>()

        sniList.chunked(concurrency).forEach { batch ->
            val deferreds = batch.map { sni ->
                async(Dispatchers.IO) {
                    testSpeed(config, sni, timeoutMs)
                }
            }
            deferreds.awaitAll().forEach { result ->
                results.add(result)
                onProgress(result)
            }
        }

        results.sortedWith(
            compareByDescending<SpeedResult> {
                if (it.status == SpeedStatus.SUCCESS) it.downloadSpeed else -1.0
            }.thenBy {
                if (it.handshakeMs > 0) it.handshakeMs else Long.MAX_VALUE
            }
        )
    }
}
