package net.pangolin.olm.ui.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.pangolin.olm.App
import net.pangolin.olm.ConnectionConfig
import net.pangolin.olm.ConnectionState
import net.pangolin.olm.OLMService
import net.pangolin.olm.Status

/**
 * ViewModel for the main connection screen.
 */
class MainViewModel : ViewModel() {

    private val app: App = App.instance
    private val json = Json { ignoreUnknownKeys = true }

    val connectionState = app.connectionState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConnectionState.Disconnected
    )

    val status = app.status.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        // Periodically refresh status when connected
        viewModelScope.launch {
            while (true) {
                delay(5000) // Every 5 seconds
                if (connectionState.value.isConnected) {
                    refreshStatus()
                }
            }
        }
    }

    /**
     * Connect to VPN with the given configuration.
     */
    fun connect(config: ConnectionConfig) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Connecting with config: endpoint=${config.endpoint}, orgId=${config.orgId}")

                // Start VPN service
                val intent = Intent(app, OLMService::class.java).apply {
                    action = OLMService.ACTION_CONNECT
                }
                app.startService(intent)

                // Send config to Go backend
                val configJson = json.encodeToString(config)
                app.olmApp.connect(configJson)

                Log.d(TAG, "Connect request sent to Go backend")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
            }
        }
    }

    /**
     * Disconnect from VPN.
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Disconnecting VPN")

                // Disconnect via Go backend
                app.olmApp.disconnect()

                // Stop VPN service
                val intent = Intent(app, OLMService::class.java)
                app.stopService(intent)

                Log.d(TAG, "Disconnect request sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
            }
        }
    }

    /**
     * Switch to a different organization.
     */
    fun switchOrganization(orgId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Switching to organization: $orgId")
                app.olmApp.switchOrg(orgId)
                Log.d(TAG, "Organization switch requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch organization", e)
            }
        }
    }

    /**
     * Refresh status from Go backend.
     */
    private fun refreshStatus() {
        try {
            app.updateStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh status", e)
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
