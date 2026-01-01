package net.pangolin.olm.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.pangolin.olm.ConnectionConfig
import net.pangolin.olm.ConnectionState
import net.pangolin.olm.OLMService
import net.pangolin.olm.ui.viewmodel.MainViewModel
import net.pangolin.olm.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToPeers: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val status by viewModel.status.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, connect
            val config = ConnectionConfig(
                endpoint = settings.endpoint,
                id = settings.id,
                secret = settings.secret,
                userToken = settings.userToken,
                orgId = settings.orgId,
                mtu = settings.mtu,
                dns = settings.dns,
                upstreamDNS = settings.upstreamDNS,
                holepunch = settings.holepunch,
                tunnelDNS = settings.tunnelDNS,
                overrideDNS = settings.overrideDNS,
                pingInterval = settings.pingInterval,
                pingTimeout = settings.pingTimeout
            )
            viewModel.connect(config)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OLM VPN") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Status: ${connectionState.displayName}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    status?.let { currentStatus ->
                        Text("Organization: ${currentStatus.orgId}")
                        Text("Version: ${currentStatus.version}")
                        Text("Connected Peers: ${currentStatus.peers.count { it.connected }}")
                    }

                    if (connectionState is ConnectionState.AuthError) {
                        Text(
                            text = (connectionState as ConnectionState.AuthError).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Connection button
            val isConnected = connectionState.isConnected
            Button(
                onClick = {
                    if (isConnected) {
                        viewModel.disconnect()
                    } else {
                        // Check VPN permission
                        val prepareIntent = OLMService.prepareVpnService(context)
                        if (prepareIntent != null) {
                            vpnPermissionLauncher.launch(prepareIntent)
                        } else {
                            // Permission already granted
                            val config = ConnectionConfig(
                                endpoint = settings.endpoint,
                                id = settings.id,
                                secret = settings.secret,
                                userToken = settings.userToken,
                                orgId = settings.orgId,
                                mtu = settings.mtu,
                                dns = settings.dns,
                                upstreamDNS = settings.upstreamDNS,
                                holepunch = settings.holepunch,
                                tunnelDNS = settings.tunnelDNS,
                                overrideDNS = settings.overrideDNS,
                                pingInterval = settings.pingInterval,
                                pingTimeout = settings.pingTimeout
                            )
                            viewModel.connect(config)
                        }
                    }
                },
                modifier = Modifier
                    .size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isConnected) "Disconnect" else "Connect",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToPeers,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Peers")
                }

                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings")
                }
            }
        }
    }
}
