package com.tgvlessproxy.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.tgvlessproxy.R
import androidx.compose.ui.unit.dp
import com.tgvlessproxy.data.model.ProxyState
import com.tgvlessproxy.data.model.UiState

@Composable
fun MainScreen(
    uiState: UiState,
    telegramLink: String,
    onConfigClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        when (uiState.proxyState) {
            ProxyState.CONNECTED -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Connected",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            ProxyState.ERROR -> Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            else -> {}
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (uiState.proxyState) {
                ProxyState.CONNECTED -> if (uiState.isVpnActive) "Direct mode (VPN detected)" else "VLESS proxy active"
                ProxyState.CONNECTING -> "Connecting..."
                ProxyState.ERROR -> uiState.errorMessage ?: "Error"
                ProxyState.DISCONNECTED -> "Disconnected"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when (uiState.proxyState) {
                ProxyState.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )

        Spacer(Modifier.height(8.dp))

        if (uiState.servers.isNotEmpty()) {
            val server = uiState.servers[uiState.selectedServerIndex]
            Text(
                text = server.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${server.address}:${server.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramLink)))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_telegram),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Install")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onConfigClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Server List (${uiState.servers.size})")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Logout", color = MaterialTheme.colorScheme.error)
        }
    }
}
