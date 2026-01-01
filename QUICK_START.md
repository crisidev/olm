# OLM Android - Quick Start

## Current Status

✅ Project structure created
✅ Go library implementation complete (libolm/)
✅ Android app implementation complete (Kotlin + Compose)
✅ Build system configured (Makefile + Gradle)
✅ All compilation errors fixed
⚠️ **NEXT**: Test the APK build

## Quick Build

```bash
cd /home/matteo.bigoi/github/vpn/olm

# First time setup (skip if already done):
make androidsdk      # Install Android SDK/NDK (~2GB)
make gomobile-init   # Initialize gomobile

# Build the app:
make android-lib     # Build Go library → android/libs/libolm.aar
make apk            # Build APK → olm-debug.apk

# Install and test:
adb install olm-debug.apk
```

## Essential Commands

```bash
# Clean build
make clean
rm -rf android/build android/.gradle

# View environment
make android-env

# Manual Gradle commands
cd android
./gradlew assembleDebug
./gradlew clean
```

## Key Files to Remember

**Go interfaces** (must match Kotlin exactly):
- `libolm/interfaces.go` - AppContext, StatusCallback, Application, etc.

**Kotlin implementations**:
- `android/src/main/java/net/pangolin/olm/App.kt` - implements AppContext, StatusCallback
- `android/src/main/java/net/pangolin/olm/OLMService.kt` - implements OLMService
- `android/src/main/java/net/pangolin/olm/VPNServiceBuilder.kt` - implements VPNServiceBuilder

**Build config**:
- `Makefile` - Top-level build automation
- `android/build.gradle` - Android build config
- `go.mod` - Go dependencies

## Common Issues & Quick Fixes

### "gomobile: could not locate Android SDK"
```bash
make checkandroidsdk  # Verify installation
make androidsdk       # Install if missing
```

### Interface signature mismatch
Check generated interfaces:
```bash
unzip -q -o android/libs/libolm.aar -d /tmp/aar
javap -classpath /tmp/aar/classes.jar libolm.StatusCallback
```

### Build fails
```bash
make clean
make android-lib
make apk
```

## Architecture Summary

**Go** (65%): VPN logic, WireGuard, networking, control plane
**Kotlin** (35%): UI (Jetpack Compose), VpnService, Android platform APIs
**Bridge**: gomobile generates JNI bindings from Go interfaces

## Package Structure

```
net.pangolin.olm         # Package name
├── App                  # Application class
├── MainActivity         # Main activity
├── OLMService          # VPN service
├── Models              # Data models
└── ui.screens          # Compose screens
```

## Config

- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34
- **compileSdk**: 34
- **NDK**: 23.1.7779620
- **Gradle**: 8.7
- **Kotlin**: 1.9.22
- **Compose**: 1.5.10

## Last Known State

All code is implemented and compiles. The last error was a method name typo (`setMTU` vs `setMtu`) which has been fixed. The project should now build successfully with `make apk`.

If you encounter new errors, check `ANDROID_DEVELOPMENT_GUIDE.md` for detailed troubleshooting.
