package net.pangolin.olm

import kotlinx.serialization.Serializable

/**
 * Connection configuration sent to the Go backend.
 */
@Serializable
data class ConnectionConfig(
    val endpoint: String,
    val id: String,
    val secret: String,
    val userToken: String = "",
    val orgId: String,
    val mtu: Int = 1420,
    val dns: String = "9.9.9.9",
    val upstreamDNS: List<String> = listOf("8.8.8.8:53"),
    val holepunch: Boolean = true,
    val tunnelDNS: Boolean = false,
    val overrideDNS: Boolean = false,
    val pingInterval: String = "3s",
    val pingTimeout: String = "5s"
)

/**
 * VPN connection status from the Go backend.
 */
@Serializable
data class Status(
    val connected: Boolean = false,
    val registered: Boolean = false,
    val terminated: Boolean = false,
    val version: String = "",
    val agent: String = "",
    val orgId: String = "",
    val peers: List<PeerStatus> = emptyList()
)

/**
 * Peer status information from the API.
 */
@Serializable
data class PeerStatus(
    val siteId: Int,
    val name: String,
    val connected: Boolean,
    val rtt: Long,  // Round-trip time in nanoseconds
    val endpoint: String,
    val isRelay: Boolean
)

/**
 * Detailed peer information.
 */
@Serializable
data class Peer(
    val siteId: Int,
    val name: String,
    val connected: Boolean,
    val rtt: Long,  // Round-trip time in milliseconds
    val endpoint: String,
    val publicKey: String = "",
    val isRelay: Boolean,
    val holepunchConnected: Boolean,
    val remoteSubnets: List<String> = emptyList(),
    val aliases: List<Alias> = emptyList()
)

/**
 * DNS alias for a peer.
 */
@Serializable
data class Alias(
    val alias: String,
    val ip: String
)

/**
 * Connection state of the VPN.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Registered : ConnectionState()
    data object Connected : ConnectionState()
    data object Terminated : ConnectionState()
    data class AuthError(val code: Int, val message: String) : ConnectionState()

    val displayName: String
        get() = when (this) {
            is Disconnected -> "Disconnected"
            is Registered -> "Registered"
            is Connected -> "Connected"
            is Terminated -> "Terminated"
            is AuthError -> "Auth Error: $message"
        }

    val isConnected: Boolean
        get() = this is Connected
}

/**
 * Application settings.
 */
@Serializable
data class Settings(
    val endpoint: String = "",
    val id: String = "",
    val secret: String = "",
    val userToken: String = "",
    val orgId: String = "",
    val mtu: Int = 1420,
    val dns: String = "9.9.9.9",
    val upstreamDNS: List<String> = listOf("8.8.8.8:53"),
    val holepunch: Boolean = true,
    val tunnelDNS: Boolean = false,
    val overrideDNS: Boolean = false,
    val pingInterval: String = "3s",
    val pingTimeout: String = "5s",
    val logLevel: String = "INFO"
)
