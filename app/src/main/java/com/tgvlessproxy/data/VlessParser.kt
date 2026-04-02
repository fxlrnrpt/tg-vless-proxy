package com.tgvlessproxy.data

import android.util.Base64
import com.tgvlessproxy.data.model.VlessServer
import java.net.URI
import java.net.URLDecoder

object VlessParser {

    fun parseSubscription(response: String): List<VlessServer> {
        val decoded = try {
            String(Base64.decode(response.trim(), Base64.DEFAULT))
        } catch (_: Exception) {
            response.trim()
        }
        return decoded.lines()
            .map { it.trim() }
            .filter { it.startsWith("vless://") }
            .mapNotNull { parseVlessUri(it) }
    }

    fun parseVlessUri(uriString: String): VlessServer? {
        return try {
            val fragmentIndex = uriString.indexOf('#')
            val name = if (fragmentIndex >= 0) {
                URLDecoder.decode(uriString.substring(fragmentIndex + 1), "UTF-8")
            } else {
                ""
            }

            val uriWithoutFragment = if (fragmentIndex >= 0) {
                uriString.substring(0, fragmentIndex)
            } else {
                uriString
            }

            val uri = URI(uriWithoutFragment)
            val uuid = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else 443

            val params = parseQueryParams(uri.rawQuery ?: "")

            val network = params["type"] ?: "tcp"

            VlessServer(
                name = name.ifEmpty { "$host:$port" },
                uuid = uuid,
                address = host,
                port = port,
                encryption = params["encryption"] ?: "none",
                flow = params["flow"],
                network = network,
                security = params["security"] ?: "none",
                sni = params["sni"],
                alpn = params["alpn"],
                fingerprint = params["fp"],
                publicKey = params["pbk"],
                shortId = params["sid"],
                wsPath = if (network == "ws") params["path"]?.let { URLDecoder.decode(it, "UTF-8") } else null,
                wsHost = if (network == "ws") params["host"] else null,
                grpcServiceName = if (network == "grpc") params["serviceName"] else null,
                h2Path = if (network in listOf("h2", "http")) params["path"]?.let { URLDecoder.decode(it, "UTF-8") } else null,
                h2Host = if (network in listOf("h2", "http")) params["host"] else null,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        return query.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }
}
