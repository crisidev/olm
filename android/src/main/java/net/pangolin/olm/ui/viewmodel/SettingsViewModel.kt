package net.pangolin.olm.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.pangolin.olm.App
import net.pangolin.olm.Settings

/**
 * ViewModel for managing application settings.
 */
class SettingsViewModel : ViewModel() {

    private val app: App = App.instance
    private val preferenceStore = app.preferenceStore
    private val json = Json { ignoreUnknownKeys = true }

    private val _settings = MutableStateFlow(preferenceStore.getSettings())
    val settings = _settings.asStateFlow()

    fun updateEndpoint(endpoint: String) {
        _settings.update { it.copy(endpoint = endpoint) }
        saveSettings()
    }

    fun updateId(id: String) {
        _settings.update { it.copy(id = id) }
        saveSettings()
    }

    fun updateSecret(secret: String) {
        _settings.update { it.copy(secret = secret) }
        saveSettings()
    }

    fun updateUserToken(userToken: String) {
        _settings.update { it.copy(userToken = userToken) }
        saveSettings()
    }

    fun updateOrgId(orgId: String) {
        _settings.update { it.copy(orgId = orgId) }
        saveSettings()
    }

    fun updateMtu(mtu: Int) {
        _settings.update { it.copy(mtu = mtu) }
        saveSettings()
    }

    fun updateDns(dns: String) {
        _settings.update { it.copy(dns = dns) }
        saveSettings()
    }

    fun updateUpstreamDNS(upstreamDNS: List<String>) {
        _settings.update { it.copy(upstreamDNS = upstreamDNS) }
        saveSettings()
    }

    fun updateHolepunch(enabled: Boolean) {
        _settings.update { it.copy(holepunch = enabled) }
        saveSettings()
    }

    fun updateTunnelDNS(enabled: Boolean) {
        _settings.update { it.copy(tunnelDNS = enabled) }
        saveSettings()
    }

    fun updateOverrideDNS(enabled: Boolean) {
        _settings.update { it.copy(overrideDNS = enabled) }
        saveSettings()
    }

    fun updateLogLevel(logLevel: String) {
        _settings.update { it.copy(logLevel = logLevel) }
        saveSettings()

        // Update runtime log level via Go backend
        viewModelScope.launch {
            try {
                val settingsJson = json.encodeToString(mapOf("logLevel" to logLevel))
                app.olmApp.updateSettings(settingsJson)
                Log.d(TAG, "Log level updated to: $logLevel")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update log level", e)
            }
        }
    }

    private fun saveSettings() {
        try {
            preferenceStore.saveSettings(_settings.value)
            Log.d(TAG, "Settings saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
        }
    }

    fun clearSettings() {
        viewModelScope.launch {
            try {
                preferenceStore.clear()
                _settings.value = Settings()
                Log.d(TAG, "Settings cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear settings", e)
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
