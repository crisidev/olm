//go:build android

package olm

import (
	"github.com/fosrl/newt/logger"
	"github.com/fosrl/olm/dns"
)

// SetupDNSOverride is a no-op on Android
// DNS configuration is handled through Android's VpnService.Builder API
func SetupDNSOverride(interfaceName string, dnsProxy *dns.DNSProxy) error {
	logger.Debug("DNS override not needed on Android (handled by VpnService)")
	return nil
}

// RestoreDNSOverride is a no-op on Android
func RestoreDNSOverride() error {
	logger.Debug("DNS restore not needed on Android")
	return nil
}
