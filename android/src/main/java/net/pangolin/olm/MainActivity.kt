package net.pangolin.olm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.pangolin.olm.ui.screens.MainScreen
import net.pangolin.olm.ui.screens.PeerDetailScreen
import net.pangolin.olm.ui.screens.PeerListScreen
import net.pangolin.olm.ui.screens.SettingsScreen
import net.pangolin.olm.ui.theme.OLMTheme
import net.pangolin.olm.ui.viewmodel.MainViewModel
import net.pangolin.olm.ui.viewmodel.PeerViewModel
import net.pangolin.olm.ui.viewmodel.SettingsViewModel

/**
 * Main Activity that hosts the Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val peerViewModel: PeerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OLMApp(
                mainViewModel = mainViewModel,
                settingsViewModel = settingsViewModel,
                peerViewModel = peerViewModel
            )
        }
    }
}

@Composable
fun OLMApp(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    peerViewModel: PeerViewModel
) {
    OLMTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            // Main screen with connect/disconnect
            composable("main") {
                MainScreen(
                    viewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToPeers = { navController.navigate("peers") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            // Peer list screen
            composable("peers") {
                PeerListScreen(
                    viewModel = peerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPeer = { siteId ->
                        navController.navigate("peer/$siteId")
                    }
                )
            }

            // Peer detail screen
            composable(
                route = "peer/{siteId}",
                arguments = listOf(navArgument("siteId") { type = NavType.IntType })
            ) { backStackEntry ->
                val siteId = backStackEntry.arguments?.getInt("siteId") ?: return@composable
                PeerDetailScreen(
                    siteId = siteId,
                    viewModel = peerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings screen
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
