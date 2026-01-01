package net.pangolin.olm.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.pangolin.olm.Settings

/**
 * Secure storage for application settings using encrypted preferences.
 */
class PreferenceStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun getSettings(): Settings {
        val settingsJson = encryptedPrefs.getString(KEY_SETTINGS, null)
        return if (settingsJson != null) {
            try {
                json.decodeFromString<Settings>(settingsJson)
            } catch (e: Exception) {
                Settings() // Return default if parsing fails
            }
        } else {
            Settings()
        }
    }

    fun saveSettings(settings: Settings) {
        val settingsJson = json.encodeToString(settings)
        encryptedPrefs.edit()
            .putString(KEY_SETTINGS, settingsJson)
            .apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return encryptedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putString(key: String, value: String) {
        encryptedPrefs.edit()
            .putString(key, value)
            .apply()
    }

    fun clear() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "olm_secure_prefs"
        private const val KEY_SETTINGS = "settings"
    }
}
