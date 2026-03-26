package com.skydown.android.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.model.SettingsUiState
import com.skydown.android.ui.theme.AppearanceMode
import com.skydown.android.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenLogin: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},
    onOpenOrders: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Einstellungen",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Konto, Look und App-Bereiche klarer sortiert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SettingsOverviewCard(uiState = uiState)
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Konto")
                        if (uiState.isLoggedIn) {
                            Text(
                                text = "Angemeldet als ${uiState.username}",
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.SemiBold,
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
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text(if (uiState.isSigningOut) "Abmelden..." else "Abmelden")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.signOut(onOpenLogin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut,
                                    shape = RoundedCornerShape(18.dp),
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
                                fontWeight = FontWeight.SemiBold,
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
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Anmelden")
                                }
                                OutlinedButton(
                                    onClick = onOpenRegistration,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Registrieren")
                                }
                            }
                        }
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Admin")
                        Text(
                            text = if (uiState.isAdmin) "Bestellungen verfügbar" else "Keine Admin-Berechtigung",
                            modifier = Modifier.padding(top = 8.dp),
                            color = if (uiState.isAdmin) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                        )
                        if (uiState.isAdmin) {
                            OutlinedButton(
                                onClick = onOpenOrders,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Text("Admin-Bestellungen oeffnen")
                            }
                        }
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Allgemein")
                        Text(
                            text = "Sprache: ${uiState.language}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        SettingsToggleRow(
                            title = "Benachrichtigungen",
                            body = "Push-Hinweise fuer Updates und wichtige App-Aktionen.",
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = viewModel::updateNotifications,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("Anzeige")
                        Text(
                            text = "Aktuell: ${uiState.colorScheme.label}",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        AppearanceMode.entries.forEach { scheme ->
                            AppearanceChoiceRow(
                                title = scheme.label,
                                selected = uiState.colorScheme == scheme,
                                onClick = { viewModel.updateColorScheme(scheme) },
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }

                item {
                    SkydownCard(contentPadding = PaddingValues(18.dp)) {
                        SectionHeader("App-Info")
                        Text(
                            text = "Version ${uiState.appVersion}",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = "Support",
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "skydownent@gmail.com",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        OutlinedButton(
                            onClick = { openSupportEmail(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Support kontaktieren")
                        }
                        Text(
                            text = "Weitere rechtliche Infos folgen direkt in einem der naechsten Updates.",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsOverviewCard(
    uiState: SettingsUiState,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (uiState.isLoggedIn) uiState.username else "Skydown Einstellungen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Android folgt hier jetzt derselben klaren Struktur wie der Rest der App: Konto, Admin, Anzeige und Support sauber getrennt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsBadge(
                text = if (uiState.isLoggedIn) "Konto aktiv" else "Gast",
                icon = Icons.Default.Person,
                isActive = uiState.isLoggedIn,
            )
            SettingsBadge(
                text = if (uiState.notificationsEnabled) "Hinweise an" else "Hinweise aus",
                icon = Icons.Default.Notifications,
                isActive = uiState.notificationsEnabled,
            )
            SettingsBadge(
                text = uiState.colorScheme.label,
                icon = Icons.Default.Palette,
                isActive = true,
            )
        }

        if (uiState.isAdmin) {
            SettingsBadge(
                text = "Admin aktiv",
                icon = Icons.Default.CheckCircle,
                isActive = true,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AppearanceChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun SettingsBadge(
    text: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:skydownent@gmail.com"))
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
