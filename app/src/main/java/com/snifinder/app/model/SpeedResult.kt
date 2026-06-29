package com.snifinder.app.model

data class SpeedResult(
    val sni: String,
    val handshakeMs: Long,       // TLS handshake time in ms
    val downloadSpeed: Double,   // Download speed in KB/s
    val downloadBytes: Long,     // Total bytes downloaded
    val downloadTimeMs: Long,    // Time taken for download in ms
    val uploadSpeed: Double,     // Upload speed in KB/s
    val status: SpeedStatus
) {
    /**
     * Formatted download speed string
     */
    fun formattedDownloadSpeed(): String {
        return when {
            downloadSpeed >= 1024 -> String.format("%.1f MB/s", downloadSpeed / 1024.0)
            downloadSpeed > 0 -> String.format("%.0f KB/s", downloadSpeed)
            else -> "—"
        }
    }

    /**
     * Formatted upload speed string
     */
    fun formattedUploadSpeed(): String {
        return when {
            uploadSpeed >= 1024 -> String.format("%.1f MB/s", uploadSpeed / 1024.0)
            uploadSpeed > 0 -> String.format("%.0f KB/s", uploadSpeed)
            else -> "—"
        }
    }

    /**
     * Overall score (higher is better)
     * Combines download speed and latency
     */
    fun score(): Double {
        if (status != SpeedStatus.SUCCESS) return -1.0
        val latencyScore = if (handshakeMs > 0) (1000.0 / handshakeMs) else 0.0
        return downloadSpeed + (latencyScore * 10)
    }
}

enum class SpeedStatus {
    PENDING,
    TESTING,
    SUCCESS,
    NO_DATA,    // Connected but no data received
    TIMEOUT,
    FAILED
}
