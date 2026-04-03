# TgVlessProxy

An Android app that runs a local SOCKS5 proxy for Telegram using VLESS servers.

## What is this?

TgVlessProxy lets you route your Telegram traffic through a VLESS proxy server. You provide a subscription link (given to you by your proxy provider), and the app handles the rest.

**How to use it:**

1. Install the APK on your Android phone
2. Open the app and paste your subscription URL
3. Tap **Connect** — the app fetches your server list and starts the proxy
4. Tap **Install** — this opens Telegram and configures it to use the proxy
5. That's it. The app runs in the background and auto-starts on reboot

**Switching servers:** Tap "Server List" to pick a different server from your subscription.

**Logging out:** Tap "Logout" to disconnect and clear your subscription data.

## How it works

### Architecture

```
Telegram → SOCKS5 (127.0.0.1:51080) → xray-core → VLESS server → internet
```

The app runs [xray-core](https://github.com/XTLS/Xray-core) via [libXray](https://github.com/XTLS/libXray) (Go mobile bindings) as an in-process library. Xray provides a built-in SOCKS5 inbound listener on `127.0.0.1:51080`.

**VPN-aware routing:** The app monitors the active network via `ConnectivityManager.NetworkCallback`. When an external VPN is detected, xray switches to a `freedom` (direct) outbound — traffic bypasses the VLESS tunnel since the VPN already handles it. When the VPN disconnects, xray restarts with the VLESS outbound. VPN state is read directly from the monitor (not UI state) to avoid race conditions at startup.

**Subscription format:** The app fetches the subscription URL, base64-decodes the response, and parses `vless://` URIs. Supported transports: TCP, WebSocket, gRPC, H2. Supported security: Reality, TLS, none. Subscriptions auto-refresh every 24 hours.

**Background operation:** A foreground service (`specialUse` type) keeps xray running. A `BOOT_COMPLETED` broadcast receiver restarts the proxy after reboot if the user was logged in.

### Key components

| File | Role |
|------|------|
| `VlessParser.kt` | Parses subscription responses and `vless://` URIs |
| `XrayConfigGenerator.kt` | Builds xray JSON config (SOCKS5 inbound + VLESS/freedom outbound) |
| `ProxyForegroundService.kt` | Manages xray lifecycle via libXray's base64-encoded request/response API |
| `VpnStateMonitor.kt` | Event-driven VPN detection via `NetworkCallback` |
| `BootReceiver.kt` | Starts proxy on device boot |
| `MainViewModel.kt` | Orchestrates login, server switching, VPN state, subscription refresh |

### Building from source

**Prerequisites:** Android SDK (API 35), NDK 27+, Go 1.22+, Python 3

1. **Build libXray AAR:**
   ```sh
   git clone https://github.com/XTLS/libXray && cd libXray
   git checkout v26.1.23
   python3 build/main.py android
   cp libXray.aar /path/to/tg-vless-proxy/app/libs/
   ```

2. **Download geo data:**
   ```sh
   cd app/src/main/assets
   curl -LO https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat
   curl -LO https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat
   ```

3. **Build:**
   ```sh
   ./gradlew assembleRelease
   ```

   Per-ABI APKs appear in `app/build/outputs/apk/release/`.
