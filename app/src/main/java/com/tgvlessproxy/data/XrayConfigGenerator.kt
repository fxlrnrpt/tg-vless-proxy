package com.tgvlessproxy.data

import com.tgvlessproxy.data.model.VlessServer
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigGenerator {

    const val SOCKS_PORT = 10808

    fun generate(server: VlessServer, useDirectOutbound: Boolean): String {
        return JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("inbounds", buildInbounds())
            put("outbounds", if (useDirectOutbound) buildDirectOutbound() else buildVlessOutbound(server))
        }.toString(2)
    }

    private fun buildInbounds(): JSONArray {
        return JSONArray().put(
            JSONObject().apply {
                put("protocol", "socks")
                put("port", SOCKS_PORT)
                put("listen", "127.0.0.1")
                put("tag", "socks-in")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                })
            }
        )
    }

    private fun buildDirectOutbound(): JSONArray {
        return JSONArray().put(
            JSONObject().apply {
                put("protocol", "freedom")
                put("tag", "direct")
            }
        )
    }

    private fun buildVlessOutbound(server: VlessServer): JSONArray {
        return JSONArray().put(
            JSONObject().apply {
                put("protocol", "vless")
                put("tag", "proxy")
                put("settings", buildVlessSettings(server))
                put("streamSettings", buildStreamSettings(server))
            }
        )
    }

    private fun buildVlessSettings(server: VlessServer): JSONObject {
        val user = JSONObject().apply {
            put("id", server.uuid)
            put("encryption", server.encryption.ifEmpty { "none" })
            if (!server.flow.isNullOrEmpty()) {
                put("flow", server.flow)
            }
        }
        return JSONObject().apply {
            put("vnext", JSONArray().put(
                JSONObject().apply {
                    put("address", server.address)
                    put("port", server.port)
                    put("users", JSONArray().put(user))
                }
            ))
        }
    }

    private fun buildStreamSettings(server: VlessServer): JSONObject {
        return JSONObject().apply {
            put("network", server.network)

            when (server.security) {
                "reality" -> {
                    put("security", "reality")
                    put("realitySettings", JSONObject().apply {
                        server.fingerprint?.let { put("fingerprint", it) }
                        server.sni?.let { put("serverName", it) }
                        server.publicKey?.let { put("publicKey", it) }
                        server.shortId?.let { put("shortId", it) }
                    })
                }
                "tls" -> {
                    put("security", "tls")
                    put("tlsSettings", JSONObject().apply {
                        server.sni?.let { put("serverName", it) }
                        server.fingerprint?.let { put("fingerprint", it) }
                        server.alpn?.let { alpnStr ->
                            put("alpn", JSONArray(alpnStr.split(",")))
                        }
                    })
                }
                else -> put("security", "none")
            }

            when (server.network) {
                "ws" -> put("wsSettings", JSONObject().apply {
                    server.wsPath?.let { put("path", it) }
                    server.wsHost?.let {
                        put("headers", JSONObject().put("Host", it))
                    }
                })
                "grpc" -> put("grpcSettings", JSONObject().apply {
                    server.grpcServiceName?.let { put("serviceName", it) }
                })
                "h2", "http" -> put("httpSettings", JSONObject().apply {
                    server.h2Path?.let { put("path", it) }
                    server.h2Host?.let { hostStr ->
                        put("host", JSONArray(hostStr.split(",")))
                    }
                })
            }
        }
    }
}
