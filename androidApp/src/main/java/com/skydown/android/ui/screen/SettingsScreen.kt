package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.theme.AppearanceMode
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
                    if (uiState.email.isNotBlank()) {
                        Text(
                            text = uiState.email,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                    Text(
                        text = "Du kannst dich hier abmelden oder direkt mit einem anderen Konto anmelden.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSigningOut,
                        ) {
                            Text(if (uiState.isSigningOut) "Abmelden..." else "Abmelden")
                        }
                        OutlinedButton(
                            onClick = { viewModel.signOut(onOpenLogin) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSigningOut,
                        ) {
                            Text("Mit anderem Konto anmelden")
                        }
                    }
                    uiState.accountErrorMessage?.let { message ->
                        Text(
                            text = message,
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onOpenLogin,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Anmelden")
                        }
                        OutlinedButton(
                            onClick = onOpenRegistration,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Registrieren")
                        }
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
                Text(
                    text = "Aktuell: ${uiState.colorScheme.label}",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                AppearanceMode.entries.forEach { scheme ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .selectable(
                                selected = uiState.colorScheme == scheme,
                                onClick = { viewModel.updateColorScheme(scheme) },
                                role = Role.RadioButton,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(scheme.label)
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
