package com.tgvlessproxy.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tgvlessproxy.data.PreferencesManager
import com.tgvlessproxy.data.SubscriptionRepository
import com.tgvlessproxy.data.XrayConfigGenerator
import com.tgvlessproxy.data.model.ProxyState
import com.tgvlessproxy.data.model.UiState
import com.tgvlessproxy.service.ProxyForegroundService
import com.tgvlessproxy.service.VpnStateMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val repo = SubscriptionRepository(prefs)
    private val vpnMonitor = VpnStateMonitor(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        restoreState()
        monitorVpnState()
    }

    private fun restoreState() {
        viewModelScope.launch {
            val url = prefs.subscriptionUrl.first()
            val servers = repo.getCachedServers()
            val index = prefs.selectedServerIndex.first()

            if (url != null && servers != null && servers.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        subscriptionUrl = url,
                        servers = servers,
                        selectedServerIndex = index.coerceIn(0, servers.size - 1),
                    )
                }
                startProxy()
                scheduleSubscriptionRefresh()
            }
        }
    }

    private fun monitorVpnState() {
        vpnMonitor.start()
        viewModelScope.launch {
            vpnMonitor.isVpnActive.collect { isVpn ->
                val changed = _uiState.value.isVpnActive != isVpn
                _uiState.update { it.copy(isVpnActive = isVpn) }
                if (changed && _uiState.value.proxyState == ProxyState.CONNECTED) {
                    restartProxyWithCurrentConfig()
                }
            }
        }
    }

    private fun scheduleSubscriptionRefresh() {
        viewModelScope.launch {
            while (true) {
                if (repo.shouldRefresh()) {
                    refreshSubscription()
                }
                delay(60 * 60 * 1000L) // Check every hour
            }
        }
    }

    private suspend fun refreshSubscription() {
        val url = _uiState.value.subscriptionUrl ?: return
        repo.fetchServers(url).onSuccess { newServers ->
            if (newServers.isEmpty()) return@onSuccess
            val currentServer = _uiState.value.let { state ->
                state.servers.getOrNull(state.selectedServerIndex)
            }
            val newIndex = if (currentServer != null) {
                newServers.indexOfFirst {
                    it.address == currentServer.address && it.port == currentServer.port
                }.takeIf { it >= 0 } ?: 0
            } else 0

            val needsRestart = newIndex != _uiState.value.selectedServerIndex ||
                newServers != _uiState.value.servers
            _uiState.update {
                it.copy(servers = newServers, selectedServerIndex = newIndex)
            }
            prefs.saveSelectedServerIndex(newIndex)
            if (needsRestart && _uiState.value.proxyState == ProxyState.CONNECTED) {
                restartProxyWithCurrentConfig()
            }
        }.onFailure {
            Log.w("MainViewModel", "Subscription refresh failed", it)
        }
    }

    fun login(subscriptionUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(proxyState = ProxyState.CONNECTING, errorMessage = null) }
            repo.fetchServers(subscriptionUrl).fold(
                onSuccess = { servers ->
                    if (servers.isEmpty()) {
                        _uiState.update {
                            it.copy(proxyState = ProxyState.ERROR, errorMessage = "No servers found")
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            servers = servers,
                            selectedServerIndex = 0,
                            subscriptionUrl = subscriptionUrl,
                        )
                    }
                    startProxy()
                    scheduleSubscriptionRefresh()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(proxyState = ProxyState.ERROR, errorMessage = e.message)
                    }
                },
            )
        }
    }

    fun selectServer(index: Int) {
        viewModelScope.launch {
            prefs.saveSelectedServerIndex(index)
            _uiState.update { it.copy(selectedServerIndex = index) }
            if (_uiState.value.proxyState == ProxyState.CONNECTED) {
                restartProxyWithCurrentConfig()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            stopProxy()
            prefs.clearAll()
            _uiState.value = UiState()
        }
    }

    fun getTelegramLink(): String = "tg://socks?server=127.0.0.1&port=${XrayConfigGenerator.SOCKS_PORT}"

    private fun startProxy() {
        val state = _uiState.value
        val server = state.servers.getOrNull(state.selectedServerIndex) ?: return
        val config = XrayConfigGenerator.generate(server, useDirectOutbound = state.isVpnActive)

        val intent = Intent(getApplication(), ProxyForegroundService::class.java).apply {
            action = ProxyForegroundService.ACTION_START
            putExtra(ProxyForegroundService.EXTRA_CONFIG_JSON, config)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
        _uiState.update { it.copy(proxyState = ProxyState.CONNECTED) }
    }

    private fun stopProxy() {
        val intent = Intent(getApplication(), ProxyForegroundService::class.java).apply {
            action = ProxyForegroundService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _uiState.update { it.copy(proxyState = ProxyState.DISCONNECTED) }
    }

    private fun restartProxyWithCurrentConfig() {
        val state = _uiState.value
        val server = state.servers.getOrNull(state.selectedServerIndex) ?: return
        val config = XrayConfigGenerator.generate(server, useDirectOutbound = state.isVpnActive)

        val intent = Intent(getApplication(), ProxyForegroundService::class.java).apply {
            action = ProxyForegroundService.ACTION_RESTART
            putExtra(ProxyForegroundService.EXTRA_CONFIG_JSON, config)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    override fun onCleared() {
        vpnMonitor.stop()
    }
}
