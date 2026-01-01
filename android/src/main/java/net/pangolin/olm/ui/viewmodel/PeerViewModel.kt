package net.pangolin.olm.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.pangolin.olm.App
import net.pangolin.olm.Peer

/**
 * ViewModel for managing peer information.
 */
class PeerViewModel : ViewModel() {

    private val app: App = App.instance

    // Peers sorted by connection status and RTT
    val peers = app.peers.map { peerList ->
        peerList.sortedWith(
            compareByDescending<Peer> { it.connected }
                .thenBy { it.rtt }
                .thenBy { it.siteId }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Connected peers count
    val connectedCount = peers.map { list ->
        list.count { it.connected }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    init {
        // Periodically refresh peers
        viewModelScope.launch {
            while (true) {
                delay(5000) // Every 5 seconds
                refreshPeers()
            }
        }
    }

    /**
     * Get a specific peer by site ID.
     */
    fun getPeerById(siteId: Int): Peer? {
        return peers.value.find { it.siteId == siteId }
    }

    /**
     * Refresh peers from Go backend.
     */
    fun refreshPeers() {
        try {
            app.updatePeers()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh peers", e)
        }
    }

    /**
     * Format RTT in milliseconds to a human-readable string.
     */
    fun formatRTT(rttMs: Long): String {
        return when {
            rttMs < 1 -> "< 1 ms"
            rttMs < 1000 -> "$rttMs ms"
            else -> "${rttMs / 1000}.${(rttMs % 1000) / 100} s"
        }
    }

    companion object {
        private const val TAG = "PeerViewModel"
    }
}
