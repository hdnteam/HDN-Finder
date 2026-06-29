package com.snifinder.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snifinder.app.model.ConfigData
import com.snifinder.app.model.SniResult
import com.snifinder.app.model.SniStatus
import com.snifinder.app.model.SpeedResult
import com.snifinder.app.model.SpeedStatus
import com.snifinder.app.util.ConfigParser
import com.snifinder.app.util.SniListProvider
import com.snifinder.app.util.SniTester
import com.snifinder.app.util.SpeedTester
import com.snifinder.app.util.RemoteListUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScanMode {
    LATENCY,
    SPEED_TEST
}

enum class SortMode {
    DOWNLOAD,   // Highest download speed first
    UPLOAD,     // Highest upload speed first
    PING        // Lowest ping first
}

data class UiState(
    val configInput: String = "",
    val customSniInput: String = "",
    val parsedConfig: ConfigData? = null,
    val parseError: String? = null,
    val isScanning: Boolean = false,
    val scanMode: ScanMode = ScanMode.SPEED_TEST,
    val sortMode: SortMode = SortMode.DOWNLOAD,
    val latencyResults: List<SniResult> = emptyList(),
    val speedResults: List<SpeedResult> = emptyList(),
    val allSpeedResults: List<SpeedResult> = emptyList(),
    val progress: Int = 0,
    val totalCount: Int = 0,
    val useCustomList: Boolean = false,
    val timeoutSeconds: Int = 10,
    val scanJob: Job? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateConfigInput(input: String) {
        _uiState.value = _uiState.value.copy(configInput = input, parseError = null)
    }

    fun updateCustomSniInput(input: String) {
        _uiState.value = _uiState.value.copy(customSniInput = input)
    }

    fun toggleCustomList(use: Boolean) {
        _uiState.value = _uiState.value.copy(useCustomList = use)
    }

    fun updateTimeout(seconds: Int) {
        _uiState.value = _uiState.value.copy(timeoutSeconds = seconds)
    }

    fun setScanMode(mode: ScanMode) {
        _uiState.value = _uiState.value.copy(scanMode = mode)
    }

    fun setSortMode(mode: SortMode) {
        _uiState.value = _uiState.value.copy(sortMode = mode)
        // Re-sort existing results
        resortResults()
    }

    private fun resortResults() {
        val all = _uiState.value.allSpeedResults
        if (all.isEmpty()) return

        val sorted = when (_uiState.value.sortMode) {
            SortMode.DOWNLOAD -> all.sortedByDescending {
                if (it.status == SpeedStatus.SUCCESS) it.downloadSpeed else -1.0
            }
            SortMode.UPLOAD -> all.sortedByDescending {
                if (it.status == SpeedStatus.SUCCESS) it.uploadSpeed else -1.0
            }
            SortMode.PING -> all.sortedBy {
                if (it.handshakeMs > 0) it.handshakeMs else Long.MAX_VALUE
            }
        }
        _uiState.value = _uiState.value.copy(speedResults = sorted)
    }

    fun parseConfig() {
        val config = ConfigParser.parse(_uiState.value.configInput)
        if (config != null) {
            _uiState.value = _uiState.value.copy(
                parsedConfig = config,
                parseError = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                parsedConfig = null,
                parseError = "فرمت کانفیگ نامعتبر.\nپشتیبانی: VLESS, VMess, Trojan, SS, Hysteria2\nTransport: TCP, WS, gRPC, HTTP, QUIC\nSecurity: TLS, Reality"
            )
        }
    }

    fun startScan() {
        val config = _uiState.value.parsedConfig ?: return

        viewModelScope.launch {
            val sniList = if (_uiState.value.useCustomList) {
                SniListProvider.parseUserList(_uiState.value.customSniInput)
            } else {
                RemoteListUpdater.getSniList(getApplication())
            }

            if (sniList.isEmpty()) {
                _uiState.value = _uiState.value.copy(parseError = "لیست SNI خالی است")
                return@launch
            }

            when (_uiState.value.scanMode) {
                ScanMode.LATENCY -> startLatencyScan(config, sniList)
                ScanMode.SPEED_TEST -> startSpeedScan(config, sniList)
            }
        }
    }

    private fun startLatencyScan(config: ConfigData, sniList: List<String>) {
        val job = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                latencyResults = emptyList(),
                speedResults = emptyList(),
                allSpeedResults = emptyList(),
                progress = 0,
                totalCount = sniList.size
            )

            val results = mutableListOf<SniResult>()
            SniTester.testMultipleSnis(
                server = config.server,
                port = config.port,
                sniList = sniList,
                timeoutMs = _uiState.value.timeoutSeconds * 1000
            ) { result ->
                results.add(result)
                val sorted = results.sortedWith(compareBy {
                    if (it.latency == -1L) Long.MAX_VALUE else it.latency
                })
                _uiState.value = _uiState.value.copy(
                    latencyResults = sorted,
                    progress = results.size
                )
            }
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
        _uiState.value = _uiState.value.copy(scanJob = job)
    }

    private fun startSpeedScan(config: ConfigData, sniList: List<String>) {
        val job = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                latencyResults = emptyList(),
                speedResults = emptyList(),
                allSpeedResults = emptyList(),
                progress = 0,
                totalCount = sniList.size
            )

            val results = mutableListOf<SpeedResult>()
            SpeedTester.testAllSnis(
                config = config,
                sniList = sniList,
                timeoutMs = _uiState.value.timeoutSeconds * 1000
            ) { result ->
                results.add(result)
                _uiState.value = _uiState.value.copy(
                    allSpeedResults = results.toList(),
                    progress = results.size
                )
                resortResults()
            }
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
        _uiState.value = _uiState.value.copy(scanJob = job)
    }

    fun stopScan() {
        _uiState.value.scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun getConfigWithSni(sni: String): String? {
        val config = _uiState.value.parsedConfig ?: return null
        return ConfigParser.replaceSni(config, sni)
    }
}
