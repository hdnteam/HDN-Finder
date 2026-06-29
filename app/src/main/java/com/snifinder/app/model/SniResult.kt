package com.snifinder.app.model

data class SniResult(
    val sni: String,
    val latency: Long, // in milliseconds, -1 means timeout/failed
    val status: SniStatus
)

enum class SniStatus {
    PENDING,
    TESTING,
    SUCCESS,
    TIMEOUT,
    FAILED
}

data class ConfigData(
    val rawConfig: String,
    val protocol: Protocol,
    val server: String,
    val port: Int,
    val uuid: String,
    val security: String,       // tls, reality, none
    val transport: Transport,   // tcp, ws, grpc, http, quic
    val originalSni: String,
    val serverName: String,     // for reality
    val flow: String,           // for vless+reality
    val pbk: String,            // reality public key
    val sid: String,            // reality short id
    val fp: String,             // fingerprint
    val alpn: String,
    val path: String,           // ws path or grpc serviceName
    val host: String            // ws host header
)

enum class Protocol {
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    HYSTERIA2,
    UNKNOWN
}

enum class Transport {
    TCP,
    WS,
    GRPC,
    HTTP,
    QUIC,
    HTTPUPGRADE,
    SPLITHTTP,
    UNKNOWN
}
