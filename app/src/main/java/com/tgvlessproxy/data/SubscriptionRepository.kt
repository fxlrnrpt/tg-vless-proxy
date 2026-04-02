package com.tgvlessproxy.data

import com.tgvlessproxy.data.model.VlessServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class SubscriptionRepository(private val prefs: PreferencesManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchServers(subscriptionUrl: String): Result<List<VlessServer>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(subscriptionUrl)
                    .header("User-Agent", "TgVlessProxy/1.0")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw IOException("Empty response")
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

                prefs.saveSubscription(subscriptionUrl, body)
                VlessParser.parseSubscription(body)
            }
        }

    suspend fun getCachedServers(): List<VlessServer>? {
        val cached = prefs.cachedSubscription.first() ?: return null
        val servers = VlessParser.parseSubscription(cached)
        return servers.ifEmpty { null }
    }

    suspend fun shouldRefresh(): Boolean = prefs.shouldRefresh()
}
