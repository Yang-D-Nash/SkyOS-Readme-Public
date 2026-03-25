package com.skydown.android.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
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
import com.skydown.android.data.GoogleSignInManager
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.LoginViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

@Composable
fun LoginScreen(
    onClose: () -> Unit,
    onOpenRegistration: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
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
                viewModel.onGoogleSignInCancelled("Google-Anmeldung hat kein gueltiges Token geliefert.")
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
                text = "Willkommen bei Skydown",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Melden Sie sich an, um exklusive Inhalte zu sehen.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("E-Mail-Adresse") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Passwort") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Button(
                onClick = { viewModel.signIn(onClose) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
            ) {
                Text(if (uiState.isLoading) "Anmelden..." else "Anmelden")
            }
            Button(
                onClick = {
                    viewModel.beginGoogleSignIn()
                    googleClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleClient.signInIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
            ) {
                Text(if (uiState.isGoogleLoading) "Google wird gestartet..." else "Mit Google anmelden")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onOpenRegistration) {
                    Text("Noch kein Konto? Registrieren")
                }
                TextButton(onClick = onClose) {
                    Text("Schliessen")
                }
            }
        }

        ToastHost(
            message = uiState.errorMessage,
            type = ToastType.Error,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun ApiException.toReadableGoogleMessage(): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
        CommonStatusCodes.CANCELED -> "Google-Anmeldung wurde abgebrochen."
        CommonStatusCodes.NETWORK_ERROR -> "Netzwerkfehler bei Google-Anmeldung. Bitte erneut versuchen."
        CommonStatusCodes.DEVELOPER_ERROR -> {
            "Google-Anmeldung ist fuer Android noch nicht korrekt konfiguriert. " +
                "In Firebase fehlt sehr wahrscheinlich die Android-SHA-1/SHA-256 fuer com.skydown.android."
        }
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google-Anmeldung laeuft bereits."
        GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google-Anmeldung ist fehlgeschlagen."
        CommonStatusCodes.INTERNAL_ERROR -> "Interner Google-Fehler. Bitte App neu starten."
        else -> "Google-Anmeldung fehlgeschlagen: ${localizedMessage ?: statusCode}"
    }
}
