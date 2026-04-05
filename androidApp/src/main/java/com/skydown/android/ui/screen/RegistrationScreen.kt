package com.skydown.android.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ApiException
import com.skydown.android.data.GoogleSignInManager
import com.skydown.android.ui.component.GoogleAuthButton
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onClose: () -> Unit,
    onBusyStateChanged: (Boolean) -> Unit = {},
    viewModel: RegistrationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthBusy = uiState.isLoading || uiState.isGoogleLoading
    val context = LocalContext.current
    val googleClient = remember(context) { GoogleSignInManager.client(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled()
            return@rememberLauncherForActivityResult
        }

        try {
            val account = GoogleSignInManager.accountFromIntent(result.data)
            val idToken = account.idToken

            if (idToken.isNullOrBlank()) {
                googleClient.signOut()
                viewModel.onGoogleSignInCancelled("Google-Registrierung hat kein gueltiges Token geliefert.")
            } else {
                viewModel.signInWithGoogle(idToken, onClose)
            }
        } catch (exception: ApiException) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled(exception.toReadableGoogleMessage())
        } catch (_: Exception) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled("Google-Registrierung konnte nicht abgeschlossen werden.")
        }
    }

    LaunchedEffect(isAuthBusy) {
        onBusyStateChanged(isAuthBusy)
    }

    DisposableEffect(Unit) {
        onDispose {
            onBusyStateChanged(false)
        }
    }

    Box(
        modifier = Modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Registrieren",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onClose,
                    enabled = !isAuthBusy,
                ) {
                    Text("Schliessen")
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = "Neues Konto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Der Onboarding-Flow ist jetzt klarer auf kleine Android-Displays abgestimmt und fuehrt schneller in die App.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LoginInfoPill(text = "Profil")
                    LoginInfoPill(text = "Google")
                    LoginInfoPill(text = "Direktstart")
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = "Mit E-Mail registrieren",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("Benutzername") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text("E-Mail-Adresse") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Passwort") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::updateConfirmPassword,
                    label = { Text("Passwort bestaetigen") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(18.dp),
                )
                Button(
                    onClick = { viewModel.register(onClose) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isAuthBusy,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (uiState.isLoading) "Registrieren..." else "Registrieren")
                }
                GoogleAuthButton(
                    text = if (uiState.isGoogleLoading) {
                        "Google wird gestartet..."
                    } else {
                        "Mit Google registrieren"
                    },
                    isLoading = uiState.isGoogleLoading,
                    onClick = {
                        viewModel.beginGoogleSignIn()
                        googleClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleClient.signInIntent)
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    enabled = !isAuthBusy,
                )
                Text(
                    text = "Beim ersten Google-Login wird dein Konto automatisch angelegt. Ein eingetragener Benutzername wird direkt uebernommen.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            ToastHost(
                message = uiState.errorMessage,
                type = ToastType.Error,
            )
        }
    }
}
