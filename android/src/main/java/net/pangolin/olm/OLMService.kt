package net.pangolin.olm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import libolm.Libolm
import libolm.OLMService as GoOLMService
import libolm.VPNServiceBuilder as GoVPNServiceBuilder
import java.util.UUID

/**
 * Android VPN service that implements the Go OLMService interface.
 * This service manages the lifecycle of the VPN connection.
 */
class OLMService : VpnService(), GoOLMService {

    private val serviceId = UUID.randomUUID().toString()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OLMService created with ID: $serviceId")

        // Create notification channel for foreground service
        createNotificationChannel()

        // Start as foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Notify Go backend that the VPN service is ready
        try {
            Libolm.requestVPN(this)
            Log.d(TAG, "VPN service ready notification sent to Go")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify Go backend", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OLMService onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_CONNECT -> {
                Log.d(TAG, "Connect action received")
                // Connection is initiated from Go backend via RequestVPN
            }
            ACTION_DISCONNECT -> {
                Log.d(TAG, "Disconnect action received")
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "OLMService destroying")

        // Notify Go backend that the service is disconnecting
        try {
            Libolm.serviceDisconnect(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify Go backend of disconnect", e)
        }

        super.onDestroy()
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by user")
        stopSelf()
        super.onRevoke()
    }

    // GoOLMService interface implementation

    override fun id(): String {
        return serviceId
    }

    override fun protect(fd: Int): Boolean {
        val protected = protect(fd)
        if (protected) {
            Log.d(TAG, "Socket protected: fd=$fd")
        } else {
            Log.w(TAG, "Failed to protect socket: fd=$fd")
        }
        return protected
    }

    override fun newBuilder(): GoVPNServiceBuilder {
        Log.d(TAG, "Creating new VPN builder")
        val builder = Builder()
            .setSession("OLM VPN")
            .setConfigureIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

        return VPNServiceBuilderImpl(builder)
    }

    override fun close() {
        Log.d(TAG, "OLMService close requested")
        stopSelf()
    }

    // Private helper methods

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OLM VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for OLM VPN connection status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "OLMService"
        private const val CHANNEL_ID = "olm_vpn_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_CONNECT = "net.pangolin.olm.CONNECT"
        const val ACTION_DISCONNECT = "net.pangolin.olm.DISCONNECT"

        /**
         * Check if VPN permission is granted.
         */
        fun isVpnPermissionGranted(context: android.content.Context): Boolean {
            val intent = prepare(context)
            return intent == null
        }

        /**
         * Request VPN permission if not already granted.
         */
        fun prepareVpnService(context: android.content.Context): Intent? {
            return prepare(context)
        }
    }
}
