package net.pangolin.olm

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import libolm.ParcelFileDescriptor as GoParcelFileDescriptor
import libolm.VPNServiceBuilder as GoVPNServiceBuilder

/**
 * Wrapper around Android's VpnService.Builder that implements the Go interface.
 */
class VPNServiceBuilderImpl(
    private val builder: VpnService.Builder
) : GoVPNServiceBuilder {

    override fun setMTU(mtu: Int) {
        builder.setMtu(mtu)
        Log.d(TAG, "MTU set to: $mtu")
    }

    override fun addAddress(addr: String, prefixLen: Int) {
        builder.addAddress(addr, prefixLen)
        Log.d(TAG, "Address added: $addr/$prefixLen")
    }

    override fun addRoute(route: String, prefixLen: Int) {
        builder.addRoute(route, prefixLen)
        Log.d(TAG, "Route added: $route/$prefixLen")
    }

    override fun addDNSServer(dns: String) {
        builder.addDnsServer(dns)
        Log.d(TAG, "DNS server added: $dns")
    }

    override fun establish(): GoParcelFileDescriptor {
        val pfd = builder.establish()
            ?: throw IllegalStateException("Failed to establish VPN")

        Log.d(TAG, "VPN established successfully")
        return ParcelFileDescriptorImpl(pfd)
    }

    companion object {
        private const val TAG = "VPNServiceBuilder"
    }
}

/**
 * Wrapper around Android's ParcelFileDescriptor that implements the Go interface.
 */
class ParcelFileDescriptorImpl(
    private val pfd: ParcelFileDescriptor
) : GoParcelFileDescriptor {

    override fun detach(): Int {
        val fd = pfd.detachFd()
        Log.d(TAG, "ParcelFileDescriptor detached: fd=$fd")
        return fd
    }

    companion object {
        private const val TAG = "ParcelFileDescriptor"
    }
}
