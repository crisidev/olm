// Copyright (c) Pangolin Inc & AUTHORS
// SPDX-License-Identifier: Apache-2.0

package libolm

import (
	"fmt"
	"strings"
)

// VPNConfig holds the configuration for establishing the Android VPN.
type VPNConfig struct {
	TunnelIP      string
	MTU           int
	DNS           string
	Routes        []string
	UtilitySubnet string
}

// EstablishVPN configures and establishes the Android VPN using the provided builder.
// It returns the file descriptor that can be used to create the TUN device.
func (b *Backend) EstablishVPN(builder VPNServiceBuilder, config VPNConfig) (int32, error) {
	b.log("OLM", fmt.Sprintf("Establishing VPN with config: %+v", config))

	// Set MTU
	if err := builder.SetMTU(int32(config.MTU)); err != nil {
		return 0, fmt.Errorf("failed to set MTU: %w", err)
	}

	// Add tunnel IP address
	addr, prefixLen := parseIPWithPrefix(config.TunnelIP)
	if err := builder.AddAddress(addr, int32(prefixLen)); err != nil {
		return 0, fmt.Errorf("failed to add address: %w", err)
	}

	// Add utility subnet route (for DNS proxy, etc.)
	if config.UtilitySubnet != "" {
		utilAddr, utilPrefix := parseIPWithPrefix(config.UtilitySubnet)
		if err := builder.AddRoute(utilAddr, int32(utilPrefix)); err != nil {
			b.log("OLM", fmt.Sprintf("Warning: failed to add utility subnet route: %v", err))
		}
	}

	// Add default route (0.0.0.0/0) to route all traffic through VPN
	if err := builder.AddRoute("0.0.0.0", 0); err != nil {
		return 0, fmt.Errorf("failed to add default route: %w", err)
	}

	// Add additional routes if specified
	for _, route := range config.Routes {
		routeAddr, routePrefix := parseIPWithPrefix(route)
		if err := builder.AddRoute(routeAddr, int32(routePrefix)); err != nil {
			b.log("OLM", fmt.Sprintf("Warning: failed to add route %s: %v", route, err))
		}
	}

	// Add DNS server
	if config.DNS != "" {
		if err := builder.AddDNSServer(config.DNS); err != nil {
			b.log("OLM", fmt.Sprintf("Warning: failed to add DNS server: %v", err))
		}
	}

	// Establish the VPN and get the file descriptor
	pfd, err := builder.Establish()
	if err != nil {
		return 0, fmt.Errorf("failed to establish VPN: %w", err)
	}

	// Detach the file descriptor so we can use it
	fd, err := pfd.Detach()
	if err != nil {
		return 0, fmt.Errorf("failed to detach file descriptor: %w", err)
	}

	b.log("OLM", fmt.Sprintf("VPN established successfully, fd=%d", fd))
	return fd, nil
}

// ProtectSocket protects a socket from being routed through the VPN.
// This is necessary for the WebSocket connection to the control server.
func (b *Backend) ProtectSocket(fd int32) bool {
	b.mu.Lock()
	defer b.mu.Unlock()

	if b.olmService == nil {
		b.log("OLM", "Warning: no VPN service available to protect socket")
		return false
	}

	protected := b.olmService.Protect(fd)
	if protected {
		b.log("OLM", fmt.Sprintf("Socket fd=%d protected from VPN", fd))
	} else {
		b.log("OLM", fmt.Sprintf("Failed to protect socket fd=%d", fd))
	}

	return protected
}

// parseIPWithPrefix parses an IP address with CIDR notation (e.g., "10.0.0.1/24")
// and returns the IP address and prefix length separately.
func parseIPWithPrefix(ipWithPrefix string) (string, int) {
	// Default prefix lengths
	defaultIPv4Prefix := 32
	defaultIPv6Prefix := 128

	parts := strings.Split(ipWithPrefix, "/")
	if len(parts) == 1 {
		// No prefix specified, use default based on IP version
		ip := parts[0]
		if strings.Contains(ip, ":") {
			return ip, defaultIPv6Prefix
		}
		return ip, defaultIPv4Prefix
	}

	// Parse prefix length
	ip := parts[0]
	var prefix int
	fmt.Sscanf(parts[1], "%d", &prefix)

	return ip, prefix
}
