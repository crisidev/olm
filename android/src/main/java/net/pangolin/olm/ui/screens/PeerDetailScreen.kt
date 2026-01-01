package net.pangolin.olm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.pangolin.olm.Peer
import net.pangolin.olm.ui.viewmodel.PeerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerDetailScreen(
    siteId: Int,
    viewModel: PeerViewModel,
    onNavigateBack: () -> Unit
) {
    val peer = viewModel.getPeerById(siteId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peer?.name ?: "Peer Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (peer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text("Peer not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic Info Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        DetailRow("Name", peer.name)
                        DetailRow("Site ID", peer.siteId.toString())
                        DetailRow("Status", if (peer.connected) "Connected" else "Disconnected")
                        DetailRow("Endpoint", peer.endpoint)

                        if (peer.publicKey.isNotEmpty()) {
                            DetailRow("Public Key", peer.publicKey.take(16) + "...")
                        }
                    }
                }

                // Connection Info Card
                if (peer.connected) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Connection",
                                style = MaterialTheme.typography.titleMedium
                            )

                            DetailRow("Round-Trip Time", viewModel.formatRTT(peer.rtt))
                            DetailRow("Connection Type", when {
                                peer.isRelay -> "Relay"
                                peer.holepunchConnected -> "Direct (Holepunch)"
                                else -> "Unknown"
                            })
                        }
                    }
                }

                // Remote Subnets Card
                if (peer.remoteSubnets.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Remote Subnets",
                                style = MaterialTheme.typography.titleMedium
                            )

                            peer.remoteSubnets.forEach { subnet ->
                                Text(
                                    text = "• $subnet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Aliases Card
                if (peer.aliases.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DNS Aliases",
                                style = MaterialTheme.typography.titleMedium
                            )

                            peer.aliases.forEach { alias ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = alias.alias,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = alias.ip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
