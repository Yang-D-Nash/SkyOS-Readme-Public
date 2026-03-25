package com.skydown.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skydown.android.ui.screen.AgentScreen
import com.skydown.android.ui.screen.CartScreen
import com.skydown.android.ui.screen.AiScreen
import com.skydown.android.ui.screen.IntroScreen
import com.skydown.android.ui.screen.LoginScreen
import com.skydown.android.ui.screen.MusicScreen
import com.skydown.android.ui.screen.OrderScreen
import com.skydown.android.ui.screen.RegistrationScreen
import com.skydown.android.ui.screen.SettingsScreen
import com.skydown.android.ui.screen.ShopScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkydownApp() {
    val navController = rememberNavController()
    var showIntro by remember { mutableStateOf(true) }
    var authSheet by remember { mutableStateOf<AuthSheet?>(null) }
    var showOrders by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val authSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ordersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val destinations = listOf(
        BottomDestination("shop", "Shop", { Icon(Icons.Default.ShoppingCart, contentDescription = null) }),
        BottomDestination("music", "Musik", { Icon(Icons.Default.MusicNote, contentDescription = null) }),
        BottomDestination("ai", "AI", { Icon(Icons.Default.AutoAwesome, contentDescription = null) }),
        BottomDestination("agent", "Agent", { Icon(Icons.Default.Bolt, contentDescription = null) }),
        BottomDestination("more", "Mehr", { Icon(Icons.Default.Settings, contentDescription = null) }),
    )
    val moreRoutes = setOf("cart", "settings")

    if (showIntro) {
        IntroScreen(
            onFinished = { showIntro = false },
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = if (destination.route == "more") {
                                currentDestination?.hierarchy?.any { it.route in moreRoutes } == true
                            } else {
                                currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            },
                            onClick = {
                                if (destination.route == "more") {
                                    showMoreMenu = true
                                } else {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = destination.icon,
                            label = {
                                Text(
                                    text = destination.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "shop",
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("shop") {
                    ShopScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                    )
                }
                composable("music") { MusicScreen() }
                composable("ai") { AiScreen() }
                composable("agent") { AgentScreen() }
                composable("cart") {
                    CartScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                        onOpenRegistration = { authSheet = AuthSheet.Registration },
                        onOpenOrders = { showOrders = true },
                    )
                }
            }
        }
    }

    if (showMoreMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            sheetState = moreSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            MoreMenuSheet(
                onOpenCart = {
                    showMoreMenu = false
                    navController.navigate("cart") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenSettings = {
                    showMoreMenu = false
                    navController.navigate("settings") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }

    authSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { authSheet = null },
            sheetState = authSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (sheet) {
                AuthSheet.Login -> LoginScreen(
                    onClose = { authSheet = null },
                    onOpenRegistration = { authSheet = AuthSheet.Registration },
                )
                AuthSheet.Registration -> RegistrationScreen(
                    onClose = { authSheet = null },
                )
            }
        }
    }

    if (showOrders) {
        ModalBottomSheet(
            onDismissRequest = { showOrders = false },
            sheetState = ordersSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            OrderScreen(
                onClose = { showOrders = false },
            )
        }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
private fun MoreMenuSheet(
    onOpenCart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Mehr",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Korb und Einstellungen liegen auf Android hier, damit die Bottom Navigation bei 5 Haupttabs sauber bleibt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        TextButton(
            onClick = onOpenCart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text("Warenkorb")
        }
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text("Einstellungen")
        }
    }
}

private enum class AuthSheet {
    Login,
    Registration,
}
