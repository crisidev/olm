// Copyright (c) Pangolin Inc & AUTHORS
// SPDX-License-Identifier: Apache-2.0

package libolm

import (
	_ "golang.org/x/mobile/bind"
)

// AppContext provides a context within which the Application is running.
// This interface is implemented by the Android Application class and provides
// a hook into Android-specific functionality.
type AppContext interface {
	// Log logs the given tag and message to Android logcat
	Log(tag, message string)

	// GetDeviceModel returns the Android device model
	GetDeviceModel() string

	// GetOSVersion returns the Android OS version
	GetOSVersion() string

	// EncryptToPref stores the given value to an encrypted preference at the given key
	EncryptToPref(key, value string) error

	// DecryptFromPref retrieves the given value from an encrypted preference
	// at the given key, or returns empty string if unset
	DecryptFromPref(key string) (string, error)
}

// StatusCallback is the interface for receiving OLM status updates.
// The Android App class implements this to receive events from the Go backend.
type StatusCallback interface {
	// OnRegistered is called when the client successfully registers with the server
	OnRegistered()

	// OnConnected is called when the VPN connection is established
	OnConnected()

	// OnTerminated is called when the connection is terminated by the server
	OnTerminated()

	// OnAuthError is called when authentication fails
	OnAuthError(statusCode int32, message string)

	// OnPeerUpdate is called when peer information changes (as JSON)
	OnPeerUpdate(peerJSON string)
}

// Application encapsulates the running OLM Application.
// There is only a single instance of Application per Android application.
type Application interface {
	// Connect starts the VPN connection with the given configuration (JSON)
	Connect(configJSON string) error

	// Disconnect stops the VPN connection
	Disconnect() error

	// SwitchOrg switches to a different organization
	SwitchOrg(orgID string) error

	// GetStatus returns the current status as JSON
	GetStatus() string

	// GetPeers returns the current peer list as JSON
	GetPeers() string

	// UpdateSettings updates runtime settings (JSON)
	UpdateSettings(settingsJSON string) error
}

// OLMService corresponds to the Android OLMService (VpnService).
// The Android VPN service implements this interface.
type OLMService interface {
	// ID returns the unique ID of this instance of the OLMService.
	// Every time we start a new VPN service, it should have a new ID.
	ID() string

	// Protect protects the socket identified by the given file descriptor from
	// being captured by the VPN. Returns whether the socket was successfully protected.
	Protect(fd int32) bool

	// NewBuilder creates a new VPNServiceBuilder in preparation for starting
	// the Android VPN.
	NewBuilder() VPNServiceBuilder

	// Close closes the service
	Close()
}

// VPNServiceBuilder corresponds to Android's VpnService.Builder.
// This interface wraps the Android VPN configuration builder.
type VPNServiceBuilder interface {
	// SetMTU sets the MTU for the VPN interface
	SetMTU(mtu int32) error

	// AddAddress adds an IP address to the VPN interface
	AddAddress(addr string, prefixLen int32) error

	// AddRoute adds a route to the VPN
	AddRoute(route string, prefixLen int32) error

	// AddDNSServer adds a DNS server to the VPN
	AddDNSServer(dns string) error

	// Establish establishes the VPN and returns the file descriptor
	Establish() (ParcelFileDescriptor, error)
}

// ParcelFileDescriptor corresponds to Android's ParcelFileDescriptor.
// This wraps a file descriptor that can be passed from Java to Go.
type ParcelFileDescriptor interface {
	// Detach detaches the file descriptor and returns the raw fd
	Detach() (int32, error)
}

// Start initializes the OLM backend.
// This should be called once when the Android application starts.
// dataDir is the directory for storing application data.
// callback receives status updates from the Go backend.
// appCtx provides access to Android-specific functionality.
func Start(dataDir string, callback StatusCallback, appCtx AppContext) Application {
	return start(dataDir, callback, appCtx)
}

// RequestVPN is called by Android when the VPN service is ready to start.
// The service parameter is the Android VpnService instance.
func RequestVPN(service OLMService) {
	onVPNRequested <- service
}

// ServiceDisconnect is called when the VPN service is being destroyed.
func ServiceDisconnect(service OLMService) {
	onDisconnect <- service
}

// Channels for communication between Android and Go
var (
	onVPNRequested = make(chan OLMService, 1)
	onDisconnect   = make(chan OLMService, 1)
)
