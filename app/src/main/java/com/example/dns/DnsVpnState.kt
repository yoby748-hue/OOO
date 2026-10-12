package com.example.dns

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class DnsState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activePrimaryDns: String = "",
    val activeSecondaryDns: String = "",
    val connectedAtMillis: Long = 0L,
    val resolvedQueriesCount: Int = 0,
    val lastPingMs: Long? = null,
    val errorMessage: String? = null,
    val activeServerTitle: String = "",
    val isVpnMode: Boolean = false,
    val activeV2rayLink: String = "",
    val vpnProtocol: String = ""
)

object DnsVpnState {
    private val _state = MutableStateFlow(DnsState())
    val state: StateFlow<DnsState> = _state.asStateFlow()

    fun updateConnecting(
        primary: String,
        secondary: String,
        serverTitle: String = "",
        isVpnMode: Boolean = false,
        v2rayLink: String = "",
        protocol: String = ""
    ) {
        _state.value = _state.value.copy(
            status = ConnectionStatus.CONNECTING,
            activePrimaryDns = primary,
            activeSecondaryDns = secondary,
            activeServerTitle = serverTitle,
            isVpnMode = isVpnMode,
            activeV2rayLink = v2rayLink,
            vpnProtocol = protocol,
            errorMessage = null
        )
    }

    fun updateConnected(
        primary: String,
        secondary: String,
        serverTitle: String = "",
        isVpnMode: Boolean = false,
        v2rayLink: String = "",
        protocol: String = ""
    ) {
        _state.value = _state.value.copy(
            status = ConnectionStatus.CONNECTED,
            activePrimaryDns = primary,
            activeSecondaryDns = secondary,
            activeServerTitle = serverTitle,
            isVpnMode = isVpnMode,
            activeV2rayLink = v2rayLink,
            vpnProtocol = protocol,
            connectedAtMillis = System.currentTimeMillis(),
            errorMessage = null
        )
    }

    fun updateDisconnected(errorMessage: String? = null) {
        _state.value = _state.value.copy(
            status = if (errorMessage != null) ConnectionStatus.ERROR else ConnectionStatus.DISCONNECTED,
            connectedAtMillis = 0L,
            errorMessage = errorMessage
        )
    }

    fun incrementQueries() {
        _state.value = _state.value.copy(
            resolvedQueriesCount = _state.value.resolvedQueriesCount + 1
        )
    }

    fun updatePing(pingMs: Long?) {
        _state.value = _state.value.copy(
            lastPingMs = pingMs
        )
    }
}
