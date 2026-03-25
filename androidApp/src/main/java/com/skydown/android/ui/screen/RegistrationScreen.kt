package com.skydown.android.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    viewModel: RegistrationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkydownCard {
            Text(
                text = "Neues Konto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("Benutzername") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("E-Mail-Adresse") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Passwort") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = { Text("Passwort bestaetigen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Button(
                onClick = { viewModel.register(onClose) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
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
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
            )
            Text(
                text = "Beim ersten Google-Login wird dein Konto automatisch angelegt.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Wenn du oben einen Benutzernamen eintraegst, wird er fuer dein Profil uebernommen.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(
                onClick = onClose,
                modifier = Modifier.padding(top = 8.dp),
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
            ) {
                Text("Abbrechen")
            }
        }

        ToastHost(
            message = uiState.errorMessage,
            type = ToastType.Error,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
