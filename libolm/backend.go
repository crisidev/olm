// Copyright (c) Pangolin Inc & AUTHORS
// SPDX-License-Identifier: Apache-2.0

package libolm

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/fosrl/newt/logger"
	"github.com/fosrl/newt/util"
	"github.com/fosrl/olm/api"
	"github.com/fosrl/olm/olm"
)

// Backend implements the Application interface and wraps the OLM core.
type Backend struct {
	mu           sync.Mutex
	dataDir      string
	callback     StatusCallback
	appCtx       AppContext
	olmService   OLMService
	globalConfig olm.GlobalConfig
	tunnelConfig olm.TunnelConfig
	ctx          context.Context
	cancel       context.CancelFunc
}

// start creates and initializes a new Backend instance.
// This is called by the Start function in interfaces.go.
func start(dataDir string, callback StatusCallback, appCtx AppContext) Application {
	ctx, cancel := context.WithCancel(context.Background())

	b := &Backend{
		dataDir:  dataDir,
		callback: callback,
		appCtx:   appCtx,
		ctx:      ctx,
		cancel:   cancel,
	}

	// Set up global config with callbacks
	b.globalConfig = olm.GlobalConfig{
		LogLevel:  "INFO",
		EnableAPI: false, // We control via Go API, not HTTP
		Version:   "1.0.0-android",
		Agent:     "olm-android",

		OnRegistered: func() {
			b.log("OLM", "Registered with server")
			if b.callback != nil {
				b.callback.OnRegistered()
			}
		},

		OnConnected: func() {
			b.log("OLM", "VPN connected")
			if b.callback != nil {
				b.callback.OnConnected()
			}
		},

		OnTerminated: func() {
			b.log("OLM", "Connection terminated by server")
			if b.callback != nil {
				b.callback.OnTerminated()
			}
		},

		OnAuthError: func(statusCode int, message string) {
			b.log("OLM", fmt.Sprintf("Auth error: %d - %s", statusCode, message))
			if b.callback != nil {
				b.callback.OnAuthError(int32(statusCode), message)
			}
		},
	}

	// Initialize OLM
	olm.Init(b.ctx, b.globalConfig)

	// Start VPN request handler
	go b.handleVPNRequests()

	b.log("OLM", "Backend initialized")

	return b
}

// handleVPNRequests listens for VPN service requests from Android.
func (b *Backend) handleVPNRequests() {
	for {
		select {
		case service := <-onVPNRequested:
			b.mu.Lock()
			b.olmService = service
			b.mu.Unlock()
			b.log("OLM", fmt.Sprintf("VPN service ready: %s", service.ID()))

		case service := <-onDisconnect:
			b.log("OLM", fmt.Sprintf("VPN service disconnecting: %s", service.ID()))
			b.mu.Lock()
			if b.olmService != nil && b.olmService.ID() == service.ID() {
				b.olmService = nil
			}
			b.mu.Unlock()

		case <-b.ctx.Done():
			return
		}
	}
}

// Connect starts the VPN connection with the given configuration.
// configJSON is a JSON-encoded ConnectionConfig from Android.
func (b *Backend) Connect(configJSON string) error {
	b.mu.Lock()
	defer b.mu.Unlock()

	b.log("OLM", "Connect request received")

	// Parse configuration
	var config struct {
		Endpoint     string   `json:"endpoint"`
		ID           string   `json:"id"`
		Secret       string   `json:"secret"`
		UserToken    string   `json:"userToken"`
		OrgID        string   `json:"orgId"`
		MTU          int      `json:"mtu"`
		DNS          string   `json:"dns"`
		UpstreamDNS  []string `json:"upstreamDNS"`
		Holepunch    bool     `json:"holepunch"`
		TunnelDNS    bool     `json:"tunnelDNS"`
		OverrideDNS  bool     `json:"overrideDNS"`
		PingInterval string   `json:"pingInterval"`
		PingTimeout  string   `json:"pingTimeout"`
	}

	if err := json.Unmarshal([]byte(configJSON), &config); err != nil {
		return fmt.Errorf("failed to parse config JSON: %w", err)
	}

	// Validate required fields
	if config.Endpoint == "" || config.ID == "" || config.Secret == "" {
		return fmt.Errorf("endpoint, id, and secret are required")
	}

	// Set defaults
	if config.MTU == 0 {
		config.MTU = 1420
	}
	if config.DNS == "" {
		config.DNS = "9.9.9.9"
	}
	if len(config.UpstreamDNS) == 0 {
		config.UpstreamDNS = []string{"8.8.8.8:53"}
	}
	if config.PingInterval == "" {
		config.PingInterval = "3s"
	}
	if config.PingTimeout == "" {
		config.PingTimeout = "5s"
	}

	// Parse durations
	pingInterval, err := parseDuration(config.PingInterval)
	if err != nil {
		b.log("OLM", fmt.Sprintf("Invalid ping interval: %v, using default", err))
		pingInterval, _ = parseDuration("3s")
	}

	pingTimeout, err := parseDuration(config.PingTimeout)
	if err != nil {
		b.log("OLM", fmt.Sprintf("Invalid ping timeout: %v, using default", err))
		pingTimeout, _ = parseDuration("5s")
	}

	// Build tunnel config
	b.tunnelConfig = olm.TunnelConfig{
		Endpoint:             config.Endpoint,
		ID:                   config.ID,
		Secret:               config.Secret,
		UserToken:            config.UserToken,
		OrgID:                config.OrgID,
		MTU:                  config.MTU,
		DNS:                  config.DNS,
		UpstreamDNS:          config.UpstreamDNS,
		InterfaceName:        "olm0",
		Holepunch:            config.Holepunch,
		OverrideDNS:          config.OverrideDNS,
		TunnelDNS:            config.TunnelDNS,
		PingIntervalDuration: time.Duration(pingInterval),
		PingTimeoutDuration:  time.Duration(pingTimeout),
		DisableRelay:         false,
		EnableUAPI:           false,
		// FileDescriptorTun will be set when VPN is established
	}

	// Start tunnel in background
	go olm.StartTunnel(b.tunnelConfig)

	b.log("OLM", "VPN connection initiated")
	return nil
}

// Disconnect stops the VPN connection.
func (b *Backend) Disconnect() error {
	b.mu.Lock()
	defer b.mu.Unlock()

	b.log("OLM", "Disconnect request received")

	if err := olm.StopTunnel(); err != nil {
		return fmt.Errorf("failed to stop tunnel: %w", err)
	}

	b.log("OLM", "VPN disconnected")
	return nil
}

// SwitchOrg switches to a different organization without reconnecting.
func (b *Backend) SwitchOrg(orgID string) error {
	b.mu.Lock()
	defer b.mu.Unlock()

	b.log("OLM", fmt.Sprintf("Switching to organization: %s", orgID))

	if err := olm.SwitchOrg(orgID); err != nil {
		return fmt.Errorf("failed to switch org: %w", err)
	}

	b.log("OLM", "Organization switched successfully")
	return nil
}

// GetStatus returns the current VPN status as JSON.
func (b *Backend) GetStatus() string {
	status := olm.GetStatus()

	jsonData, err := json.Marshal(status)
	if err != nil {
		b.log("OLM", fmt.Sprintf("Failed to marshal status: %v", err))
		return "{}"
	}

	return string(jsonData)
}

// GetPeers returns the current peer list as JSON.
func (b *Backend) GetPeers() string {
	status := olm.GetStatus()

	// Extract peer information from status
	peers := b.extractPeers(status)

	jsonData, err := json.Marshal(peers)
	if err != nil {
		b.log("OLM", fmt.Sprintf("Failed to marshal peers: %v", err))
		return "[]"
	}

	return string(jsonData)
}

// extractPeers converts the API peer status to a format suitable for Android.
func (b *Backend) extractPeers(status api.StatusResponse) []map[string]interface{} {
	peers := make([]map[string]interface{}, 0, len(status.PeerStatuses))

	for _, peer := range status.PeerStatuses {
		peerMap := map[string]interface{}{
			"siteId":              peer.SiteID,
			"name":                peer.Name,
			"connected":           peer.Connected,
			"rtt":                 peer.RTT,
			"endpoint":            peer.Endpoint,
			"publicKey":           "",                  // Not exposed in API status
			"isRelay":             peer.IsRelay,
			"holepunchConnected":  !peer.IsRelay,       // Inverse of relay status
			"remoteSubnets":       []string{},          // TODO: Add when available
			"aliases":             []map[string]string{}, // TODO: Add when available
		}
		peers = append(peers, peerMap)
	}

	return peers
}

// UpdateSettings updates runtime settings.
func (b *Backend) UpdateSettings(settingsJSON string) error {
	b.mu.Lock()
	defer b.mu.Unlock()

	b.log("OLM", "Update settings request received")

	// Parse settings
	var settings map[string]interface{}
	if err := json.Unmarshal([]byte(settingsJSON), &settings); err != nil {
		return fmt.Errorf("failed to parse settings JSON: %w", err)
	}

	// Update logger level if present
	if logLevel, ok := settings["logLevel"].(string); ok {
		logger.GetLogger().SetLevel(util.ParseLogLevel(logLevel))
		b.log("OLM", fmt.Sprintf("Log level updated to: %s", logLevel))
	}

	return nil
}

// log logs a message to Android logcat via the AppContext.
func (b *Backend) log(tag, message string) {
	if b.appCtx != nil {
		b.appCtx.Log(tag, message)
	}
}

// Helper functions

func parseDuration(s string) (duration, error) {
	// Simple duration parser for Android (supports "3s", "5s", etc.)
	// Using time.Duration would be better, but keeping it simple for gomobile
	var d duration
	switch s {
	case "1s":
		d = duration(1000000000) // 1 second in nanoseconds
	case "3s":
		d = duration(3000000000)
	case "5s":
		d = duration(5000000000)
	case "10s":
		d = duration(10000000000)
	default:
		return 0, fmt.Errorf("unsupported duration: %s", s)
	}
	return d, nil
}

// duration is a simple type alias to avoid time.Duration issues with gomobile
type duration int64
