package com.tgvlessproxy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tgvlessproxy.MainActivity
import com.tgvlessproxy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

class ProxyForegroundService : Service() {

    companion object {
        const val TAG = "ProxyService"
        const val ACTION_START = "com.tgvlessproxy.START"
        const val ACTION_STOP = "com.tgvlessproxy.STOP"
        const val ACTION_RESTART = "com.tgvlessproxy.RESTART"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "proxy_channel"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON)
                if (configJson != null) {
                    startProxy(configJson)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> stopProxy()
            ACTION_RESTART -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON)
                if (configJson != null) {
                    restartProxy(configJson)
                }
            }
        }
        return START_STICKY
    }

    private fun runXray(configJson: String) {
        val datDir = filesDir.absolutePath
        val requestBase64 = libXray.LibXray.newXrayRunFromJSONRequest(datDir, configJson)
        val responseBase64 = libXray.LibXray.runXrayFromJSON(requestBase64)
        val response = JSONObject(String(Base64.decode(responseBase64, Base64.DEFAULT)))
        if (!response.optBoolean("success", false)) {
            throw RuntimeException(response.optString("error", "Unknown xray error"))
        }
    }

    private fun stopXray() {
        val responseBase64 = libXray.LibXray.stopXray()
        val response = JSONObject(String(Base64.decode(responseBase64, Base64.DEFAULT)))
        if (!response.optBoolean("success", false)) {
            Log.w(TAG, "stopXray: ${response.optString("error")}")
        }
    }

    private fun startProxy(configJson: String) {
        startForeground(NOTIFICATION_ID, buildNotification("Connecting..."))
        serviceScope.launch {
            try {
                runXray(configJson)
                updateNotification("Connected")
                Log.i(TAG, "Xray started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start xray", e)
                updateNotification("Error: ${e.message}")
            }
        }
    }

    private fun restartProxy(configJson: String) {
        serviceScope.launch {
            try { stopXray() } catch (_: Exception) {}
            try {
                runXray(configJson)
                updateNotification("Connected")
                Log.i(TAG, "Xray restarted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart xray", e)
                updateNotification("Error: ${e.message}")
            }
        }
    }

    private fun stopProxy() {
        serviceScope.launch {
            try { stopXray() } catch (_: Exception) {}
        }
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Proxy Service",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TgVlessProxy")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(status: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(status))
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
