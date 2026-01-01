package net.pangolin.olm

import android.app.Application
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import libolm.AppContext
import libolm.Application as GoApplication
import libolm.Libolm
import libolm.StatusCallback
import net.pangolin.olm.util.PreferenceStore

/**
 * Main Application class that implements Go interfaces for bridging Android and Go.
 */
class App : Application(), AppContext, StatusCallback {

    lateinit var olmApp: GoApplication
        private set

    lateinit var preferenceStore: PreferenceStore
        private set

    private val json = Json { ignoreUnknownKeys = true }

    // Connection state exposed to UI
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Peers exposed to UI
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers = _peers.asStateFlow()

    // Status exposed to UI
    private val _status = MutableStateFlow<Status?>(null)
    val status = _status.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "Initializing OLM Android app")

        // Initialize preference store
        preferenceStore = PreferenceStore(this)

        // Initialize Go backend
        try {
            olmApp = Libolm.start(
                filesDir.absolutePath,
                this,  // StatusCallback
                this   // AppContext
            )
            Log.d(TAG, "OLM backend initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OLM backend", e)
        }
    }

    // AppContext interface implementation (called by Go)

    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun getDeviceModel(): String {
        return Build.MODEL
    }

    override fun getOSVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }

    override fun encryptToPref(key: String, value: String) {
        preferenceStore.putString(key, value)
    }

    override fun decryptFromPref(key: String): String {
        return try {
            preferenceStore.getString(key, "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt preference: $key", e)
            ""
        }
    }

    // StatusCallback interface implementation (called by Go)

    override fun onRegistered() {
        Log.d(TAG, "StatusCallback: onRegistered")
        _connectionState.value = ConnectionState.Registered
    }

    override fun onConnected() {
        Log.d(TAG, "StatusCallback: onConnected")
        _connectionState.value = ConnectionState.Connected
        updateStatus()
    }

    override fun onTerminated() {
        Log.d(TAG, "StatusCallback: onTerminated")
        _connectionState.value = ConnectionState.Terminated
        _peers.value = emptyList()
        _status.value = null
    }

    override fun onAuthError(statusCode: Int, message: String) {
        Log.e(TAG, "StatusCallback: onAuthError - code=$statusCode, message=$message")
        _connectionState.value = ConnectionState.AuthError(statusCode, message)
    }

    override fun onPeerUpdate(peerJSON: String) {
        Log.d(TAG, "StatusCallback: onPeerUpdate")
        try {
            val peers = json.decodeFromString<List<Peer>>(peerJSON)
            _peers.value = peers
            Log.d(TAG, "Updated ${peers.size} peers")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse peer update JSON", e)
        }
    }

    // Helper methods

    /**
     * Update the status from the Go backend.
     */
    fun updateStatus() {
        try {
            val statusJson = olmApp.getStatus()
            val status = json.decodeFromString<Status>(statusJson)
            _status.value = status
            Log.d(TAG, "Status updated: connected=${status.connected}, peers=${status.peers.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status", e)
        }
    }

    /**
     * Update peers from the Go backend.
     */
    fun updatePeers() {
        try {
            val peersJson = olmApp.getPeers()
            val peers = json.decodeFromString<List<Peer>>(peersJson)
            _peers.value = peers
            Log.d(TAG, "Peers updated: ${peers.size} peers")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update peers", e)
        }
    }

    companion object {
        private const val TAG = "OLMApp"

        @Volatile
        lateinit var instance: App
            private set
    }
}
