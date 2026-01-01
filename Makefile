.PHONY: all local docker-build-release

all: local

local:
	CGO_ENABLED=0 go build -o ./bin/olm

docker-build-release:
	@if [ -z "$(tag)" ]; then \
		echo "Error: tag is required. Usage: make docker-build-release tag=<tag>"; \
		exit 1; \
	fi
	docker buildx build . \
		--platform linux/arm/v7,linux/arm64,linux/amd64 \
		-t fosrl/olm:latest \
		-t fosrl/olm:$(tag) \
		-f Dockerfile \
		--push

.PHONY: go-build-release \
        go-build-release-linux-arm64 go-build-release-linux-arm32-v7 \
        go-build-release-linux-arm32-v6 go-build-release-linux-amd64 \
        go-build-release-linux-riscv64 go-build-release-darwin-arm64 \
        go-build-release-darwin-amd64 go-build-release-windows-amd64

go-build-release: \
    go-build-release-linux-arm64 \
    go-build-release-linux-arm32-v7 \
    go-build-release-linux-arm32-v6 \
    go-build-release-linux-amd64 \
    go-build-release-linux-riscv64 \
    go-build-release-darwin-arm64 \
    go-build-release-darwin-amd64 \
    go-build-release-windows-amd64 \

go-build-release-linux-arm64:
	CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o bin/olm_linux_arm64

go-build-release-linux-arm32-v7:
	CGO_ENABLED=0 GOOS=linux GOARCH=arm GOARM=7 go build -o bin/olm_linux_arm32

go-build-release-linux-arm32-v6:
	CGO_ENABLED=0 GOOS=linux GOARCH=arm GOARM=6 go build -o bin/olm_linux_arm32v6

go-build-release-linux-amd64:
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o bin/olm_linux_amd64

go-build-release-linux-riscv64:
	CGO_ENABLED=0 GOOS=linux GOARCH=riscv64 go build -o bin/olm_linux_riscv64

go-build-release-darwin-arm64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -o bin/olm_darwin_arm64

go-build-release-darwin-amd64:
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -o bin/olm_darwin_amd64

go-build-release-windows-amd64:
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 go build -o bin/olm_windows_amd64.exe

# ==============================================================================
# Android Build Configuration
# ==============================================================================

# Android SDK & Tools settings
ifeq ($(shell uname),Linux)
    ANDROID_TOOLS_URL := "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
    ANDROID_TOOLS_SUM := "bd1aa17c7ef10066949c88dc6c9c8d536be27f992a1f3b5a584f9bd2ba5646a0  commandlinetools-linux-9477386_latest.zip"
else ifeq ($(shell uname),Darwin)
    ANDROID_TOOLS_URL := "https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip"
    ANDROID_TOOLS_SUM := "2072ffce4f54cdc0e6d2074d2f381e7e579b7d63e915c220b96a7db95b2900ee  commandlinetools-mac-9477386_latest.zip"
endif

# Required Android SDK packages
ANDROID_SDK_PACKAGES := 'platforms;android-34' 'extras;android;m2repository' 'ndk;23.1.7779620' 'platform-tools' 'build-tools;34.0.0'

# Attempt to find ANDROID_SDK_ROOT from environment or common locations
export ANDROID_SDK_ROOT ?= $(shell find $$ANDROID_SDK_ROOT $$ANDROID_HOME $$HOME/Library/Android/sdk $$HOME/Android/Sdk $$HOME/AppData/Local/Android/Sdk /usr/lib/android-sdk -maxdepth 1 -type d 2>/dev/null | head -n 1)

# Set default SDK location if not found
ifeq ($(ANDROID_SDK_ROOT),)
    ifeq ($(shell uname),Linux)
        export ANDROID_SDK_ROOT := $(HOME)/Android/Sdk
    else ifeq ($(shell uname),Darwin)
        export ANDROID_SDK_ROOT := $(HOME)/Library/Android/sdk
    else
        export ANDROID_SDK_ROOT := $(PWD)/android-sdk
    endif
endif

export ANDROID_HOME ?= $(ANDROID_SDK_ROOT)

# Auto-select NDK (choose highest version available)
NDK_ROOT ?= $(shell ls -1d $(ANDROID_HOME)/ndk/* 2>/dev/null | sort -V | tail -n 1)

# Detect host OS and architecture
HOST_OS := $(shell uname | tr A-Z a-z)
ifeq ($(HOST_OS),linux)
    STRIP_TOOL := $(NDK_ROOT)/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objcopy
else ifeq ($(HOST_OS),darwin)
    STRIP_TOOL := $(NDK_ROOT)/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objcopy
endif

# Attempt to find Android Studio for bundled JDK
ANDROID_STUDIO_ROOT ?= $(shell find ~/android-studio /usr/local/android-studio /opt/android-studio /Applications/Android\ Studio.app -type d -maxdepth 1 2>/dev/null | head -n 1)

# Set JAVA_HOME to Android Studio bundled JDK
export JAVA_HOME ?= $(shell find "$(ANDROID_STUDIO_ROOT)/jbr" "$(ANDROID_STUDIO_ROOT)/jre" "$(ANDROID_STUDIO_ROOT)/Contents/jbr/Contents/Home" "$(ANDROID_STUDIO_ROOT)/Contents/jre/Contents/Home" -maxdepth 1 -type d 2>/dev/null | head -n 1)

ifeq ($(JAVA_HOME),)
    unexport JAVA_HOME
else
    export PATH := $(JAVA_HOME)/bin:$(PATH)
endif

# Set up Go toolchain
GOBIN := $(PWD)/android/build/go/bin
export GOBIN
export PATH := $(GOBIN):$(ANDROID_HOME)/cmdline-tools/latest/bin:$(ANDROID_HOME)/platform-tools:$(PATH)

# Android build directories
ANDROID_DIR := android
ANDROID_LIBS_DIR := $(ANDROID_DIR)/libs
ANDROID_AAR := $(ANDROID_LIBS_DIR)/libolm.aar
ANDROID_AAR_UNSTRIPPED := $(ANDROID_LIBS_DIR)/libolm_unstripped.aar
DEBUG_APK := olm-debug.apk

# Compute absolute path for unstripped AAR
ABS_UNSTRIPPED_AAR := $(shell pwd)/$(ANDROID_AAR_UNSTRIPPED)

$(info Using ANDROID_HOME: $(ANDROID_HOME))
$(info Using NDK_ROOT: $(NDK_ROOT))
$(info Using JAVA_HOME: $(JAVA_HOME))

# ==============================================================================
# Android SDK Setup Targets
# ==============================================================================

# Download and install Android command-line tools
$(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager:
	@echo "Installing Android SDK command-line tools..."
	mkdir -p $(ANDROID_HOME)/tmp
	mkdir -p $(ANDROID_HOME)/cmdline-tools
	(cd $(ANDROID_HOME)/tmp && \
		curl --silent -O -L $(ANDROID_TOOLS_URL) && \
		echo $(ANDROID_TOOLS_SUM) | shasum -c - && \
		unzip -q $(shell basename $(ANDROID_TOOLS_URL)))
	mv $(ANDROID_HOME)/tmp/cmdline-tools $(ANDROID_HOME)/cmdline-tools/latest
	rm -rf $(ANDROID_HOME)/tmp
	@echo "Android SDK command-line tools installed"

.PHONY: androidsdk
androidsdk: $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager ## Install Android SDK packages (NDK, platform-tools, build-tools)
	@echo "Installing Android SDK packages..."
	yes | $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1 || true
	$(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager --update
	$(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager $(ANDROID_SDK_PACKAGES)
	@echo "Android SDK packages installed"
	@echo ""
	@echo "SDK installed at: $(ANDROID_SDK_ROOT)"
	@echo "NDK installed at: $(shell ls -1d $(ANDROID_HOME)/ndk/* 2>/dev/null | sort -V | tail -n 1)"

.PHONY: checkandroidsdk
checkandroidsdk: ## Check that Android SDK is installed
	@if [ ! -f "$(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager" ]; then \
		echo ""; \
		echo "ERROR: Android SDK not found."; \
		echo ""; \
		echo "Run 'make androidsdk' to install it automatically, or"; \
		echo "set ANDROID_SDK_ROOT to your existing Android SDK location."; \
		echo ""; \
		exit 1; \
	fi
	@$(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager --list_installed 2>/dev/null | grep -q 'ndk' || (\
		echo ""; \
		echo "ERROR: Android NDK not installed."; \
		echo ""; \
		echo "Run 'make androidsdk' to install required SDK packages."; \
		echo ""; \
		exit 1)
	@echo "Android SDK check passed ✓"

# ==============================================================================
# Gomobile Setup
# ==============================================================================

$(GOBIN):
	mkdir -p $(GOBIN)

$(GOBIN)/gomobile: $(GOBIN)/gobind go.mod go.sum | $(GOBIN)
	@echo "Installing gomobile..."
	go install golang.org/x/mobile/cmd/gomobile@latest

$(GOBIN)/gobind: go.mod go.sum | $(GOBIN)
	@echo "Installing gobind..."
	go install golang.org/x/mobile/cmd/gobind@latest

.PHONY: gomobile-init
gomobile-init: checkandroidsdk $(GOBIN)/gomobile ## Install and initialize gomobile
	@echo "Initializing gomobile with Android SDK..."
	$(GOBIN)/gomobile init
	@echo "gomobile initialized ✓"

# ==============================================================================
# Android Build Targets
# ==============================================================================

.PHONY: android-lib
android-lib: $(ANDROID_AAR) ## Build the Android AAR library

$(ANDROID_AAR): $(ANDROID_AAR_UNSTRIPPED)
	@echo "Creating stripped AAR..."
	@mkdir -p $(ANDROID_LIBS_DIR)
	@if [ -n "$(STRIP_TOOL)" ] && [ -f "$(STRIP_TOOL)" ]; then \
		echo "Stripping debug symbols..."; \
		rm -rf temp_aar; \
		mkdir temp_aar; \
		unzip -q $(ABS_UNSTRIPPED_AAR) -d temp_aar; \
		if [ -f temp_aar/jni/arm64-v8a/libgojni.so ]; then \
			$(STRIP_TOOL) --strip-debug temp_aar/jni/arm64-v8a/libgojni.so; \
		fi; \
		if [ -f temp_aar/jni/armeabi-v7a/libgojni.so ]; then \
			$(STRIP_TOOL) --strip-debug temp_aar/jni/armeabi-v7a/libgojni.so; \
		fi; \
		(cd temp_aar && zip -qr ../$(ANDROID_AAR) .); \
		rm -rf temp_aar; \
	else \
		echo "Strip tool not found, copying unstripped AAR..."; \
		cp $(ANDROID_AAR_UNSTRIPPED) $(ANDROID_AAR); \
	fi
	@echo "AAR created at: $(ANDROID_AAR) ✓"

$(ANDROID_AAR_UNSTRIPPED): checkandroidsdk $(GOBIN)/gomobile
	@echo "Building Android AAR with gomobile..."
	@mkdir -p $(ANDROID_LIBS_DIR)
	@rm -f $(ABS_UNSTRIPPED_AAR)
	$(GOBIN)/gomobile bind -target android \
		-androidapi 26 \
		-ldflags "-linkmode=external -extldflags=-Wl,-z,max-page-size=16384" \
		-o $(ABS_UNSTRIPPED_AAR) \
		./libolm
	@if [ ! -f $(ABS_UNSTRIPPED_AAR) ]; then \
		echo "Error: AAR was not created"; \
		exit 1; \
	fi
	@echo "Unstripped AAR created at: $(ANDROID_AAR_UNSTRIPPED) ✓"

.PHONY: apk
apk: $(DEBUG_APK) ## Build the debug APK

.PHONY: android-apk
android-apk: $(DEBUG_APK) ## Build the debug APK (alias)

$(DEBUG_APK): android-lib
	@echo "Building Android APK..."
	@if [ ! -d "$(ANDROID_DIR)" ]; then \
		echo "Error: Android project directory not found at $(ANDROID_DIR)"; \
		exit 1; \
	fi
	(cd $(ANDROID_DIR) && ./gradlew assembleDebug)
	@if [ -f android/build/outputs/apk/debug/android-debug.apk ]; then \
		cp android/build/outputs/apk/debug/android-debug.apk $(DEBUG_APK); \
		echo "APK created at: $(DEBUG_APK) ✓"; \
	else \
		echo "Error: APK was not created"; \
		exit 1; \
	fi

.PHONY: install
install: $(DEBUG_APK) ## Install the debug APK on a connected device
	@echo "Installing APK on device..."
	adb install -r $(DEBUG_APK)
	@echo "APK installed ✓"

.PHONY: run
run: install ## Install and run the app on a connected device
	@echo "Starting OLM app..."
	adb shell am start -n net.pangolin.olm/.MainActivity

# ==============================================================================
# Utility Targets
# ==============================================================================

.PHONY: android-clean
android-clean: ## Clean Android build artifacts
	@echo "Cleaning Android build artifacts..."
	@rm -rf $(ANDROID_LIBS_DIR) $(DEBUG_APK)
	@if [ -d "$(ANDROID_DIR)" ]; then \
		(cd $(ANDROID_DIR) && ./gradlew clean 2>/dev/null || true); \
	fi
	@rm -f libgojni.so.* temp_aar
	@echo "Android build artifacts cleaned ✓"

.PHONY: android-env
android-env: ## Print Android environment variables
	@echo "Android Build Environment:"
	@echo "  ANDROID_SDK_ROOT=$(ANDROID_SDK_ROOT)"
	@echo "  ANDROID_HOME=$(ANDROID_HOME)"
	@echo "  NDK_ROOT=$(NDK_ROOT)"
	@echo "  JAVA_HOME=$(JAVA_HOME)"
	@echo "  GOBIN=$(GOBIN)"
	@echo ""
	@echo "To use these in your shell, run:"
	@echo "  export ANDROID_SDK_ROOT=$(ANDROID_SDK_ROOT)"
	@echo "  export ANDROID_HOME=$(ANDROID_HOME)"
	@echo "  export PATH=$(ANDROID_HOME)/cmdline-tools/latest/bin:$(ANDROID_HOME)/platform-tools:\$$PATH"

.PHONY: android-help
android-help: ## Show Android build help
	@echo ""
	@echo "OLM Android Build System"
	@echo "========================"
	@echo ""
	@echo "Quick Start:"
	@echo "  1. make androidsdk      # Install Android SDK/NDK (first time only)"
	@echo "  2. make gomobile-init   # Initialize gomobile (first time only)"
	@echo "  3. make android-lib     # Build the Go AAR library"
	@echo "  4. make apk             # Build the Android APK"
	@echo ""
	@echo "Common targets:"
	@echo "  make androidsdk         Install Android SDK and NDK"
	@echo "  make checkandroidsdk    Verify SDK is installed"
	@echo "  make gomobile-init      Install and initialize gomobile"
	@echo "  make android-lib        Build the libolm.aar library"
	@echo "  make apk                Build the debug APK"
	@echo "  make install            Install APK on connected device"
	@echo "  make run                Install and run the app"
	@echo "  make android-clean      Clean build artifacts"
	@echo "  make android-env        Show environment variables"
	@echo ""
