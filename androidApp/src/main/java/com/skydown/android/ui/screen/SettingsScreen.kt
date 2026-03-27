package com.skydown.android.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.model.SettingsLegalDocumentType
import com.skydown.android.ui.model.SettingsUiState
import com.skydown.android.ui.model.resolve
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
    val activeLegalDocument = rememberSaveable {
        mutableStateOf<SettingsLegalDocumentType?>(null)
    }
    val showDeleteAccountDialog = rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Einstellungen",
                        subtitle = "Konto, Rechtliches, Anzeige und Support sauber sortiert.",
                    )
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
                    skydownScreenBrush(
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.06f,
                        secondaryAlpha = 0.05f,
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
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
                                text = "Du kannst dich hier abmelden, mit einem anderen Konto neu anmelden oder dein Konto loeschen.",
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
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text(if (uiState.isSigningOut) "Abmelden..." else "Abmelden")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.signOut(onOpenLogin) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Text("Mit anderem Konto anmelden")
                                }
                                OutlinedButton(
                                    onClick = { showDeleteAccountDialog.value = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSigningOut && !uiState.isDeletingAccount,
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(
                                        if (uiState.isDeletingAccount) {
                                            "Konto wird geloescht..."
                                        } else {
                                            "Konto loeschen"
                                        },
                                    )
                                }
                            }
                            uiState.accountErrorMessage?.let { message ->
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(top = 10.dp),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            Text(
                                text = "Melde dich an oder registriere dich, um Bestellungen und persoenliche Bereiche freizuschalten.",
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
                            text = if (uiState.isAdmin) "Bestellungen verfuegbar" else "Keine Admin-Berechtigung",
                            modifier = Modifier.padding(top = 8.dp),
                            color = if (uiState.isAdmin) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                        )
                        Text(
                            text = "Admin-Bereiche bleiben auf Android sichtbar, aber nur mit passender Berechtigung aktiv.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        OutlinedButton(
                            onClick = onOpenOrders,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            enabled = uiState.isAdmin,
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Bestellungen oeffnen")
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
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Datenschutzbestimmungen")
                        }
                        OutlinedButton(
                            onClick = {
                                activeLegalDocument.value = SettingsLegalDocumentType.TermsOfService
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Nutzungsbedingungen")
                        }
                        Text(
                            text = "Support",
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "skydownent@gmail.com",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Button(
                            onClick = {
                                openSupportEmail(
                                    context = context,
                                    userEmail = uiState.email,
                                    userName = uiState.username,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Support-Anfrage senden")
                        }
                        Text(
                            text = "Support und rechtliche Hinweise sind hier direkt aus der App erreichbar.",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }

    activeLegalDocument.value?.let { documentType ->
        SettingsLegalDocumentSheet(
            documentType = documentType,
            onDismiss = { activeLegalDocument.value = null },
        )
    }

    if (showDeleteAccountDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog.value = false },
            title = {
                Text("Konto loeschen")
            },
            text = {
                Text("Moechtest du dein Konto unwiderruflich loeschen?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog.value = false
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isDeletingAccount,
                ) {
                    Text("Konto loeschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog.value = false },
                ) {
                    Text("Abbrechen")
                }
            },
        )
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
                    text = "Android zieht hier mit iOS gleich: Konto, Rechtliches, Anzeige und Support bleiben in einem klaren Flow gebuendelt.",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLegalDocumentSheet(
    documentType: SettingsLegalDocumentType,
    onDismiss: () -> Unit,
) {
    val document = documentType.resolve()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                end = 20.dp,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Zuletzt aktualisiert: ${document.updatedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    Text(
                        text = document.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }

            items(document.sections) { section ->
                SkydownCard(contentPadding = PaddingValues(18.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = section.body,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }

            item {
                SkydownCard(contentPadding = PaddingValues(18.dp)) {
                    Text(
                        text = "Kontakt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "skydownent@gmail.com",
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
            }
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
                },
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

private fun openSupportEmail(
    context: Context,
    userEmail: String,
    userName: String,
) {
    val subject = if (userEmail.isNotBlank()) {
        "Support-Anfrage - $userEmail"
    } else {
        "Support-Anfrage"
    }
    val body = """
        Hallo Skydown-Team,

        ich habe folgende Anfrage:

        Eingeloggter Account: ${userName.ifBlank { "Nicht verfuegbar" }}
        Account-E-Mail: ${userEmail.ifBlank { "Nicht verfuegbar" }}

        Nachricht:
    """.trimIndent()
    openEmailDraft(
        context = context,
        recipients = listOf("skydownent@gmail.com"),
        subject = subject,
        body = body,
    )
}
