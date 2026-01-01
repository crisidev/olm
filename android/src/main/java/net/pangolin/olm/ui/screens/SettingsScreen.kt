package net.pangolin.olm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import net.pangolin.olm.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Settings Section
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = settings.endpoint,
                onValueChange = { viewModel.updateEndpoint(it) },
                label = { Text("Endpoint URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.id,
                onValueChange = { viewModel.updateId(it) },
                label = { Text("OLM ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.secret,
                onValueChange = { viewModel.updateSecret(it) },
                label = { Text("Secret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = settings.userToken,
                onValueChange = { viewModel.updateUserToken(it) },
                label = { Text("User Token (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.orgId,
                onValueChange = { viewModel.updateOrgId(it) },
                label = { Text("Organization ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Divider()

            // Network Settings Section
            Text(
                text = "Network",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = settings.mtu.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { mtu ->
                        if (mtu in 1280..1500) {
                            viewModel.updateMtu(mtu)
                        }
                    }
                },
                label = { Text("MTU (1280-1500)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = settings.dns,
                onValueChange = { viewModel.updateDns(it) },
                label = { Text("DNS Server") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.upstreamDNS.joinToString(","),
                onValueChange = {
                    val dnsServers = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                    viewModel.updateUpstreamDNS(dnsServers)
                },
                label = { Text("Upstream DNS (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // Advanced Settings Section
            Text(
                text = "Advanced",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SwitchSetting(
                label = "Enable Holepunch",
                description = "Use NAT traversal for direct peer connections",
                checked = settings.holepunch,
                onCheckedChange = { viewModel.updateHolepunch(it) }
            )

            SwitchSetting(
                label = "Tunnel DNS",
                description = "Route DNS queries through the VPN tunnel",
                checked = settings.tunnelDNS,
                onCheckedChange = { viewModel.updateTunnelDNS(it) }
            )

            SwitchSetting(
                label = "Override DNS",
                description = "Override system DNS settings",
                checked = settings.overrideDNS,
                onCheckedChange = { viewModel.updateOverrideDNS(it) }
            )

            Divider()

            // Log Level Setting
            Text(
                text = "Debugging",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            var expanded by remember { mutableStateOf(false) }
            val logLevels = listOf("DEBUG", "INFO", "WARN", "ERROR", "FATAL")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = settings.logLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Log Level") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    logLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                viewModel.updateLogLevel(level)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Divider()

            // Clear Settings Button
            OutlinedButton(
                onClick = { viewModel.clearSettings() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All Settings")
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
