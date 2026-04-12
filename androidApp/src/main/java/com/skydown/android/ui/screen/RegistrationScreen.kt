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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ApiException
import com.skydown.android.R
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
    val googleMissingTokenMessage = stringResource(R.string.auth_google_register_missing_token)
    val googleRegisterFailedMessage = stringResource(R.string.auth_google_register_failed)
    val googleClient = remember(context) { GoogleSignInManager.client(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val intentData = result.data
        if (intentData == null && result.resultCode != Activity.RESULT_OK) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled(
                readableGoogleFallbackForActivityResult(result.resultCode)
            )
            return@rememberLauncherForActivityResult
        }

        try {
            val account = GoogleSignInManager.accountFromIntent(intentData)
            val idToken = account.idToken

            if (idToken.isNullOrBlank()) {
                googleClient.signOut()
                viewModel.onGoogleSignInCancelled(googleMissingTokenMessage)
            } else {
                viewModel.signInWithGoogle(idToken, onClose)
            }
        } catch (exception: ApiException) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled(exception.toReadableGoogleMessage())
        } catch (_: Exception) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled(googleRegisterFailedMessage)
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
                    text = stringResource(R.string.auth_register_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onClose,
                    enabled = !isAuthBusy,
                ) {
                    Text(stringResource(R.string.auth_close))
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = stringResource(R.string.auth_register_new_account),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.auth_register_subtitle),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LoginInfoPill(text = stringResource(R.string.auth_register_badge_profile))
                    LoginInfoPill(text = stringResource(R.string.auth_register_badge_google))
                    LoginInfoPill(text = stringResource(R.string.auth_register_badge_directstart))
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = stringResource(R.string.auth_register_email_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text(stringResource(R.string.auth_username)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text(stringResource(R.string.auth_email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text(stringResource(R.string.auth_password)) },
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
                    label = { Text(stringResource(R.string.auth_confirm_password)) },
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
                    Text(if (uiState.isLoading) stringResource(R.string.auth_register_loading) else stringResource(R.string.auth_register))
                }
                GoogleAuthButton(
                    text = if (uiState.isGoogleLoading) {
                        stringResource(R.string.auth_google_loading)
                    } else {
                        stringResource(R.string.auth_register_google)
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
                    text = stringResource(R.string.auth_register_google_hint),
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
