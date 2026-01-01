# OLM VPN Android App

Android application for the OLM WireGuard VPN client, following the tailscale-android architecture pattern with minimal Java/Kotlin code.

## Architecture

- **Go Layer (`libolm/`)**: All VPN logic (WireGuard, peers, DNS, WebSocket control plane)
- **Kotlin Layer**: UI (Jetpack Compose) + Android platform integration (VpnService, permissions)
- **Integration**: gomobile auto-generates JNI bindings

**Code Distribution**: ~65% Go (~1,000 lines), ~35% Kotlin (~1,700 lines)

## Prerequisites

1. **Go 1.25+**
2. **curl** and **unzip** (for Android SDK installation)
3. **adb** (optional, for device installation)

**That's it!** The Makefile will automatically install Android SDK, NDK, and gomobile.

## Building

### Quick Start (First Time Setup)

The Makefile handles everything automatically:

```bash
cd /path/to/olm

# 1. Install Android SDK and NDK (automatic, ~2GB download)
make androidsdk

# 2. Initialize gomobile (automatic)
make gomobile-init

# 3. Build the Go library (creates android/libs/libolm.aar)
make android-lib

# 4. Build the Android APK
make apk
```

### Subsequent Builds

After initial setup, just run:

```bash
make apk
```

This will rebuild only what's changed.

### Alternative: Android Studio

1. Complete steps 1-3 above (SDK setup and AAR build)
2. Open the `android/` directory in Android Studio
3. Sync Gradle
4. Build → Make Project

## Makefile Targets

### Setup Targets
- `make androidsdk` - Install Android SDK and NDK (~2GB, first time only)
- `make checkandroidsdk` - Verify Android SDK is installed
- `make gomobile-init` - Install and initialize gomobile
- `make android-env` - Show Android environment variables

### Build Targets
- `make android-lib` - Build the Go AAR library (libolm.aar)
- `make apk` - Build the debug APK
- `make install` - Install APK on connected device (requires adb)
- `make run` - Install and run the app on device

### Utility Targets
- `make android-clean` - Clean build artifacts
- `make android-help` - Show detailed help

## Troubleshooting

### "gomobile: could not locate Android SDK"

Run:
```bash
make androidsdk
make gomobile-init
```

### "Android NDK not installed"

The NDK should be installed automatically with `make androidsdk`. If not:
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager 'ndk;23.1.7779620'
```

### Check Environment

```bash
make android-env
```

This shows all Android-related paths and environment variables.

## Project Structure

```
olm/
├── libolm/              # Go library for Android
│   ├── interfaces.go    # Gomobile interfaces (AppContext, Application, etc.)
│   ├── backend.go       # OLM core wrapper
│   └── vpnfacade.go     # VPN setup helpers
├── android/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/net/pangolin/olm/
│   │       ├── App.kt                    # Application class (Go bridge)
│   │       ├── MainActivity.kt           # Entry point with navigation
│   │       ├── OLMService.kt             # VPN service
│   │       ├── VPNServiceBuilder.kt      # Builder wrapper
│   │       ├── Models.kt                 # Data models
│   │       ├── ui/
│   │       │   ├── screens/              # Jetpack Compose screens
│   │       │   │   ├── MainScreen.kt     # Connect/disconnect
│   │       │   │   ├── PeerListScreen.kt # Peer list
│   │       │   │   ├── PeerDetailScreen.kt
│   │       │   │   └── SettingsScreen.kt # Advanced settings
│   │       │   ├── viewmodel/            # State management
│   │       │   │   ├── MainViewModel.kt
│   │       │   │   ├── PeerViewModel.kt
│   │       │   │   └── SettingsViewModel.kt
│   │       │   └── theme/
│   │       │       └── Theme.kt          # Material3 theme
│   │       └── util/
│   │           └── PreferenceStore.kt    # Encrypted storage
│   ├── build.gradle                      # Gradle config
│   └── libs/
│       └── libolm.aar                   # Generated Go library
└── Makefile
```

## Features

### v1.0
- ✅ Basic VPN connect/disconnect
- ✅ Advanced settings UI (MTU, DNS, holepunch, etc.)
- ✅ Peer management UI (list, details, connection status)
- ✅ Organization switching
- ✅ Real-time peer RTT monitoring
- ✅ Jetpack Compose Material Design 3 UI

## Configuration

Settings are stored in encrypted SharedPreferences and include:

**Connection:**
- Endpoint URL
- OLM ID & Secret
- User Token (optional)
- Organization ID

**Network:**
- MTU (default: 1420)
- DNS Server (default: 9.9.9.9)
- Upstream DNS servers

**Advanced:**
- Holepunch (NAT traversal)
- Tunnel DNS
- Override DNS
- Ping interval/timeout
- Log level

## Development

### Clean Build

```bash
make android-clean
```

### Debug Logs

View logs in Android Studio Logcat or via adb:

```bash
adb logcat -s OLM:* OLMApp:* OLMService:*
```

### Gomobile Binding Updates

After modifying Go interfaces in `libolm/`, rebuild the AAR:

```bash
make android-lib
```

## Minimum Requirements

- **Android**: 8.0 (API 26)
- **Permissions**: VPN, Internet, Network State, Foreground Service

## License

Apache 2.0
