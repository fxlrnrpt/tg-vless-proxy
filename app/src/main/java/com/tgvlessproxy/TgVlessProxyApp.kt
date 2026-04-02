package com.tgvlessproxy

import android.app.Application
import java.io.File

class TgVlessProxyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        copyAssetIfNeeded("geoip.dat")
        copyAssetIfNeeded("geosite.dat")
    }

    private fun copyAssetIfNeeded(filename: String) {
        val dest = File(filesDir, filename)
        if (!dest.exists()) {
            try {
                assets.open(filename).use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {
                // Asset may not exist yet; xray may work without geo files for simple configs
            }
        }
    }
}
