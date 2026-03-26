package com.skydown.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.skydown.android.ui.screen.AiHubScreen
import com.skydown.android.ui.screen.CartScreen
import com.skydown.android.ui.screen.HomeScreen
import com.skydown.android.ui.screen.IntroScreen
import com.skydown.android.ui.screen.LoginScreen
import com.skydown.android.ui.screen.MusicScreen
import com.skydown.android.ui.screen.OrderScreen
import com.skydown.android.ui.screen.RegistrationScreen
import com.skydown.android.ui.screen.SettingsScreen
import com.skydown.android.ui.screen.ShopScreen
import com.skydown.android.ui.screen.VideoHubScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkydownApp() {
    val navController = rememberNavController()
    var showIntro by remember { mutableStateOf(true) }
    var authSheet by remember { mutableStateOf<AuthSheet?>(null) }
    var showOrders by remember { mutableStateOf(false) }
    val authSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ordersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openSettings = remember(navController) {
        {
            navController.navigate("settings") {
                launchSingleTop = true
            }
        }
    }
    val openCart = remember(navController) {
        {
            navController.navigate("cart") {
                launchSingleTop = true
            }
        }
    }
    val destinations = listOf(
        BottomDestination("shop", "Shop", { Icon(Icons.Default.ShoppingBag, contentDescription = null) }),
        BottomDestination("music", "Musik", { Icon(Icons.Default.MusicNote, contentDescription = null) }),
        BottomDestination("home", "Home", { Icon(Icons.Default.Home, contentDescription = null) }),
        BottomDestination("video", "Video", { Icon(Icons.Default.Movie, contentDescription = null) }),
        BottomDestination("ai", "KI", { Icon(Icons.Default.AutoAwesome, contentDescription = null) }),
    )

    if (showIntro) {
        IntroScreen(
            onFinished = { showIntro = false },
        )
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                    ),
                ) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        tonalElevation = 0.dp,
                    ) {
                        destinations.forEach { destination ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == destination.route } == true

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("home") {
                    HomeScreen(
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
                    )
                }
                composable("shop") {
                    ShopScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
                    )
                }
                composable("music") {
                    MusicScreen(
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
                    )
                }
                composable("video") {
                    VideoHubScreen(
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
                    )
                }
                composable("cart") {
                    CartScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                        onOpenSettings = openSettings,
                    )
                }
                composable("ai") {
                    AiHubScreen(
                        onOpenLogin = { authSheet = AuthSheet.Login },
                        onOpenCart = openCart,
                        onOpenSettings = openSettings,
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

private enum class AuthSheet {
    Login,
    Registration,
}
