# OLM Android App Development Guide

## Overview

This document describes the Android implementation of OLM (WireGuard VPN client) following the tailscale-android architecture pattern. The app minimizes Java/Kotlin code by keeping VPN logic in Go (~65% Go, ~35% Kotlin).

## Architecture

### Two-Layer Design
- **Go Layer** (`libolm/`): Core VPN logic, WireGuard, networking, control plane
- **Kotlin Layer** (`android/`): UI (Jetpack Compose), Android VpnService integration, platform APIs

### Key Components

#### Go Side (`libolm/`)
- `interfaces.go`: gomobile-compatible interfaces for Android-Go communication
- `backend.go`: Main backend implementing Application interface, wraps OLM core
- `vpnfacade.go`: VPN setup and file descriptor handling for Android VpnService
- `dns/override/dns_override_android.go`: Android-specific no-op DNS override (DNS handled by VpnService)

#### Kotlin Side (`android/src/main/java/net/pangolin/olm/`)
- `App.kt`: Application class implementing AppContext and StatusCallback interfaces
- `OLMService.kt`: VpnService implementation for Android VPN
- `VPNServiceBuilder.kt`: Wrapper around VpnService.Builder
- `MainActivity.kt`: Main activity with Compose UI
- `ui/screens/`: Compose screens (MainScreen, PeerListScreen, SettingsScreen)
- `Models.kt`: Data models for config and state

### Communication Flow
1. Kotlin calls Go via gomobile-generated interfaces (libolm.Application)
2. Go calls back to Kotlin via StatusCallback for state updates
3. VPN setup: Kotlin creates VpnService.Builder → Go configures it → Kotlin establishes VPN → Go gets file descriptor

## Build System

### Prerequisites
- Go 1.25+
- curl, unzip
- Android SDK and NDK (auto-installed by Makefile)

### Build Process

```bash
cd /path/to/olm

# 1. Install Android SDK and NDK (first time only, ~2GB download)
make androidsdk

# 2. Initialize gomobile (first time only)
make gomobile-init

# 3. Build the Go library (creates AAR)
make android-lib

# 4. Build the APK
make apk

# The APK will be at: olm-debug.apk
```

### Key Makefile Targets
- `make androidsdk`: Install Android SDK 34, NDK 23.1.7779620, build-tools
- `make checkandroidsdk`: Verify SDK/NDK installation
- `make gomobile-init`: Initialize gomobile with Android SDK
- `make android-lib`: Build libolm.aar using gomobile bind
- `make apk`: Build debug APK using Gradle
- `make android-env`: Display Android environment variables
- `make clean`: Clean build artifacts

### Build Configuration
- **Gradle**: 8.7 (gradle-wrapper.properties)
- **Android Gradle Plugin**: 8.6.1
- **Kotlin**: 1.9.22
- **Compose**: 1.5.10
- **compileSdk**: 34
- **minSdk**: 26
- **targetSdk**: 34
- **NDK**: 23.1.7779620 (for 16KB page size support)

## Issues Encountered and Solutions

### 1. DNS Override Build Failure
**Error**: `build constraints exclude all Go files in dns/override`

**Cause**: Desktop-only build tags excluded Android (`//go:build (linux && !android)`)

**Solution**: Created `dns/override/dns_override_android.go` with no-op implementations:
```go
//go:build android
package olm
func SetupDNSOverride(interfaceName string, dnsProxy *dns.DNSProxy) error { return nil }
func RestoreDNSOverride() error { return nil }
```

### 2. Type Mismatches in backend.go
**Errors**:
- `cannot use pingInterval (int64) as time.Duration`
- `status.Peers undefined (should be status.PeerStatuses)`
- `cannot use parseLogLevel() as logger.LogLevel`

**Solution**:
- Added `time.Duration()` casts for ping intervals
- Changed `status.Peers` → `status.PeerStatuses`
- Used `util.ParseLogLevel()` instead of local function
- Added missing imports: `time`, `github.com/fosrl/newt/util`

### 3. Gradle Settings Conflict
**Error**: `Build was configured to prefer settings repositories but repository 'Google' was added by build file`

**Solution**: Removed `settings.gradle` entirely. Tailscale-android doesn't use it - uses legacy buildscript approach instead.

### 4. Gradle Plugin Version Issues
**Error**: `Plugin [id: 'com.android.application'] was not found`

**Solution**: Changed from new plugin DSL to buildscript style matching tailscale-android:
```gradle
buildscript {
    dependencies {
        classpath 'com.android.tools.build:gradle:8.6.1'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
apply plugin: 'com.android.application'
```

### 5. AndroidX Version Conflicts
**Error**: `Dependency 'androidx.core:core-ktx:1.15.0' requires compileSdk 35`

**Solution**: Downgraded to match tailscale-android: `androidx.core:core-ktx:1.13.1`

### 6. Missing Launcher Icons
**Error**: `resource mipmap/ic_launcher not found`

**Solution**: Created adaptive icon resources:
- `res/mipmap-anydpi-v26/ic_launcher.xml`
- `res/drawable/ic_launcher_foreground.xml`
- `res/values/colors.xml`

### 7. Interface Signature Mismatches
**Errors**: Multiple signature mismatches between Kotlin and gomobile-generated Java

**Solution**: Inspected generated AAR with `javap` and fixed all signatures:
```kotlin
// AppContext
override fun encryptToPref(key: String, value: String) // was: (): Exception?
override fun decryptFromPref(key: String): String // was: (): String?

// StatusCallback
override fun onAuthError(statusCode: Int, message: String) // was: (Long, String)

// OLMService
override fun id(): String // was: iD()
override fun protect(fd: Int): Boolean // was: (Long): Boolean

// VPNServiceBuilder
override fun setMTU(mtu: Int) // was: (Long): Exception?, also setMtu not setMTU
override fun addAddress(addr: String, prefixLen: Int) // was: (String, Long): Exception?
override fun addRoute(route: String, prefixLen: Int) // was: (String, Long): Exception?
override fun addDNSServer(dns: String) // was: (String): Exception?

// ParcelFileDescriptor
override fun detach(): Int // was: (): Long
```

## Project Structure

```
olm/
├── libolm/                      # Go library for Android
│   ├── interfaces.go            # gomobile interfaces
│   ├── backend.go               # Main backend implementation
│   └── vpnfacade.go             # VPN integration
├── android/                     # Android app
│   ├── build.gradle             # Gradle build config
│   ├── gradle/wrapper/          # Gradle wrapper (v8.7)
│   ├── libs/                    # AAR files (libolm.aar)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/net/pangolin/olm/
│       │   ├── App.kt           # Application class
│       │   ├── MainActivity.kt
│       │   ├── OLMService.kt    # VPN service
│       │   ├── VPNServiceBuilder.kt
│       │   ├── Models.kt
│       │   └── ui/screens/      # Compose UI
│       └── res/
│           ├── mipmap-*/        # App icons
│           └── values/
├── Makefile                     # Build automation
├── go.mod                       # Go dependencies
└── .gitignore                   # Git ignore rules
```

## Key Files Modified/Created

### Created Files
1. `libolm/interfaces.go` - gomobile interface definitions
2. `libolm/backend.go` - Backend implementation
3. `libolm/vpnfacade.go` - VPN facade
4. `dns/override/dns_override_android.go` - Android DNS stub
5. `android/build.gradle` - Build configuration
6. `android/src/main/AndroidManifest.xml` - App manifest
7. All Kotlin source files in `android/src/main/java/net/pangolin/olm/`
8. Icon resources in `android/src/main/res/`

### Modified Files
1. `go.mod` - Added `golang.org/x/mobile` dependency
2. `Makefile` - Added Android SDK/NDK installation, gomobile targets
3. `.gitignore` - Added Android build artifacts

## Dependencies

### Go Dependencies
```
golang.org/x/mobile v0.0.0-20251209145715-2553ed8ce294
github.com/fosrl/newt v1.8.0 (for logger, util)
```

### Android Dependencies
```
androidx.core:core-ktx:1.13.1
androidx.compose:compose-bom:2024.09.03
androidx.activity:activity-compose:1.9.3
androidx.navigation:navigation-compose:2.8.5
androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0
kotlinx.coroutines:kotlinx-coroutines-android:1.8.1
kotlinx.serialization:kotlinx-serialization-json:1.6.3
androidx.security:security-crypto:1.1.0-alpha06
```

## Next Steps

1. **Test the build**: Run `make apk` and verify it completes successfully
2. **Install and test**: `adb install olm-debug.apk`
3. **Test VPN functionality**: Verify connect/disconnect works
4. **Test UI flows**: Main screen, settings, peer list
5. **Handle edge cases**: Network changes, VPN revocation, errors
6. **Add release signing**: Configure signing for release builds
7. **Optimize**: ProGuard/R8 rules, icon optimization

## Debugging Tips

### Check gomobile-generated interfaces
```bash
unzip -q -o android/libs/libolm.aar -d /tmp/aar_extract
javap -classpath /tmp/aar_extract/classes.jar libolm.StatusCallback
javap -classpath /tmp/aar_extract/classes.jar libolm.AppContext
```

### View Android logs
```bash
adb logcat -s OLMApp:* OLMService:* VPNServiceBuilder:* AndroidRuntime:E
```

### Clean rebuild
```bash
make clean
rm -rf android/build android/.gradle android/libs/*.aar
make android-lib
make apk
```

### Check Gradle wrapper
```bash
cd android && ./gradlew --version
```

## Important Notes

1. **gomobile limitations**:
   - No generics support
   - Limited type support (int32, int64, string, error, interfaces)
   - All interfaces must be in one package
   - Methods must return (value, error) or just value

2. **Android VPN requirements**:
   - User must grant VPN permission
   - Service must run as foreground service
   - File descriptor must be detached for Go to use it

3. **Build order matters**:
   - Always build AAR before APK
   - AAR must be in `android/libs/` before Gradle build
   - Gradle wrapper must exist before running gradlew

4. **Version compatibility**:
   - NDK 23.1.7779620 required for 16KB page size support
   - Gradle 8.7 matches tailscale-android
   - Android Gradle Plugin 8.6.1 compatible with Gradle 8.7

## References

- tailscale-android: `/home/matteo.bigoi/github/vpn/tailscale-android`
- olm core: `/home/matteo.bigoi/github/vpn/olm/olm`
- gomobile docs: https://pkg.go.dev/golang.org/x/mobile
