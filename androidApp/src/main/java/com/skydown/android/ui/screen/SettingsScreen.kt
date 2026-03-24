package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onOpenLogin: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    onOpenOrders: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Einstellungen",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Die Android-Version folgt denselben Bereichen wie auf iOS: Konto, Admin, Allgemein, Anzeige und App-Info.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        item {
            SkydownCard {
                SectionHeader("Konto")
                if (uiState.isLoggedIn) {
                    Text(
                        text = "Angemeldet als ${uiState.username}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "Abmelden und Konto loeschen werden nach der Firebase-Migration verbunden.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                    TextButton(
                        onClick = onOpenOrders,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Bestellungen")
                    }
                } else {
                    Text(
                        text = "Anmelden und Registrieren",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = onOpenLogin) { Text("Anmelden") }
                        TextButton(onClick = onOpenRegistration) { Text("Registrieren") }
                    }
                }
            }
        }

        item {
            SkydownCard {
                SectionHeader("Admin")
                Text(
                    text = if (uiState.isAdmin) "Bestellungen verfügbar" else "Keine Admin-Berechtigung",
                    modifier = Modifier.padding(top = 8.dp),
                    color = if (uiState.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (uiState.isAdmin) {
                    TextButton(
                        onClick = onOpenOrders,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Admin-Bestellungen oeffnen")
                    }
                }
            }
        }

        item {
            SkydownCard {
                SectionHeader("Allgemein")
                Text(
                    text = "Sprache: ${uiState.language}",
                    modifier = Modifier.padding(top = 8.dp),
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Benachrichtigungen")
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::updateNotifications,
                    )
                }
            }
        }

        item {
            SkydownCard {
                SectionHeader("Anzeige")
                listOf("light", "dark", "system").forEach { scheme ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(scheme.replaceFirstChar { it.uppercase() })
                        RadioButton(
                            selected = uiState.colorScheme == scheme,
                            onClick = { viewModel.updateColorScheme(scheme) },
                        )
                    }
                }
            }
        }

        item {
            SkydownCard {
                SectionHeader("App-Info")
                Text(
                    text = "Version ${uiState.appVersion}",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "Datenschutz, Nutzungsbedingungen und Support-Mail werden als naechste plattformspezifische Schritte angebunden.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}
