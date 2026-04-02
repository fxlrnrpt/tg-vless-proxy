package com.tgvlessproxy.data.model

enum class ProxyState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

data class UiState(
    val proxyState: ProxyState = ProxyState.DISCONNECTED,
    val isVpnActive: Boolean = false,
    val servers: List<VlessServer> = emptyList(),
    val selectedServerIndex: Int = 0,
    val errorMessage: String? = null,
    val subscriptionUrl: String? = null,
)
