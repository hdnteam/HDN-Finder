package com.snifinder.app.util

import com.snifinder.app.model.SniResult
import com.snifinder.app.model.SniStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object SniTester {

    private const val CONNECT_TIMEOUT = 5000 // 5 seconds
    private const val READ_TIMEOUT = 5000

    /**
     * Tests a single SNI by performing a TLS handshake and measuring real delay.
     * This gives the actual latency of establishing a TLS connection with that SNI.
     */
    suspend fun testSni(
        server: String,
        port: Int,
        sni: String,
        timeoutMs: Int = CONNECT_TIMEOUT
    ): SniResult = withContext(Dispatchers.IO) {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(TrustAllManager()), null)
            val factory: SSLSocketFactory = sslContext.socketFactory

            val startTime = System.currentTimeMillis()

            val socket = factory.createSocket() as SSLSocket
            socket.soTimeout = READ_TIMEOUT

            // Set SNI
            val sslParams = socket.sslParameters
            sslParams.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
            socket.sslParameters = sslParams

            // Connect
            socket.connect(InetSocketAddress(server, port), timeoutMs)

            // Perform TLS handshake - this is the real delay measurement
            socket.startHandshake()

            val endTime = System.currentTimeMillis()
            val latency = endTime - startTime

            socket.close()

            SniResult(
                sni = sni,
                latency = latency,
                status = SniStatus.SUCCESS
            )
        } catch (e: java.net.SocketTimeoutException) {
            SniResult(
                sni = sni,
                latency = -1,
                status = SniStatus.TIMEOUT
            )
        } catch (e: Exception) {
            SniResult(
                sni = sni,
                latency = -1,
                status = SniStatus.FAILED
            )
        }
    }

    /**
     * Tests multiple SNIs in parallel (5 concurrent) and returns sorted results
     */
    suspend fun testMultipleSnis(
        server: String,
        port: Int,
        sniList: List<String>,
        timeoutMs: Int = CONNECT_TIMEOUT,
        concurrency: Int = 5,
        onProgress: (SniResult) -> Unit = {}
    ): List<SniResult> = coroutineScope {
        val results = mutableListOf<SniResult>()

        sniList.chunked(concurrency).forEach { batch ->
            val deferreds = batch.map { sni ->
                async(Dispatchers.IO) {
                    testSni(server, port, sni, timeoutMs)
                }
            }
            deferreds.awaitAll().forEach { result ->
                results.add(result)
                onProgress(result)
            }
        }

        results.sortedWith(compareBy {
            if (it.latency == -1L) Long.MAX_VALUE else it.latency
        })
    }
}
