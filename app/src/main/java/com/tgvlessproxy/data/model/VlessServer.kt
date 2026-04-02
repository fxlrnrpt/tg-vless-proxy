package com.tgvlessproxy.data.model

data class VlessServer(
    val name: String,
    val uuid: String,
    val address: String,
    val port: Int,
    val encryption: String = "none",
    val flow: String? = null,
    val network: String = "tcp",
    val security: String = "none",
    val sni: String? = null,
    val alpn: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val wsPath: String? = null,
    val wsHost: String? = null,
    val grpcServiceName: String? = null,
    val h2Path: String? = null,
    val h2Host: String? = null,
)
