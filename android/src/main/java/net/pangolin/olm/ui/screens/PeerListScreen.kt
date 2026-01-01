package net.pangolin.olm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.pangolin.olm.Peer
import net.pangolin.olm.ui.viewmodel.PeerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerListScreen(
    viewModel: PeerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPeer: (Int) -> Unit
) {
    val peers by viewModel.peers.collectAsState()
    val connectedCount by viewModel.connectedCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peers ($connectedCount connected)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (peers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No peers connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(peers) { peer ->
                    PeerCard(
                        peer = peer,
                        viewModel = viewModel,
                        onClick = { onNavigateToPeer(peer.siteId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PeerCard(
    peer: Peer,
    viewModel: PeerViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (peer.connected) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                        shape = CircleShape
                    )
            )

            // Peer info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Site ID: ${peer.siteId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (peer.connected) {
                    Text(
                        text = "RTT: ${viewModel.formatRTT(peer.rtt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (peer.isRelay) {
                        Text(
                            text = "Via Relay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (peer.holepunchConnected) {
                        Text(
                            text = "Direct Connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
