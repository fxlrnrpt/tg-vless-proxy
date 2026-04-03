package com.tgvlessproxy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tgvlessproxy.data.PreferencesManager
import com.tgvlessproxy.data.SubscriptionRepository
import com.tgvlessproxy.data.VlessParser
import com.tgvlessproxy.data.XrayConfigGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = PreferencesManager(context)
        val cached = runBlocking { prefs.cachedSubscription.first() } ?: return
        val servers = VlessParser.parseSubscription(cached)
        if (servers.isEmpty()) return

        val index = runBlocking { prefs.selectedServerIndex.first() }.coerceIn(0, servers.size - 1)
        val vpnMonitor = VpnStateMonitor(context)
        val isVpn = vpnMonitor.isVpnActive.value
        val config = XrayConfigGenerator.generate(servers[index], useDirectOutbound = isVpn)

        val serviceIntent = Intent(context, ProxyForegroundService::class.java).apply {
            action = ProxyForegroundService.ACTION_START
            putExtra(ProxyForegroundService.EXTRA_CONFIG_JSON, config)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
