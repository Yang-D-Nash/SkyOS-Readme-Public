package com.skydown.android.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.R
import com.skydown.android.data.GoogleSignInManager
import com.skydown.android.ui.component.GoogleAuthButton
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.LoginViewModel
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onClose: () -> Unit,
    onOpenRegistration: () -> Unit,
    onBusyStateChanged: (Boolean) -> Unit = {},
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthBusy = uiState.isLoading || uiState.isGoogleLoading
    val context = LocalContext.current
    val googleMissingTokenMessage = stringResource(R.string.auth_google_signin_missing_token)
    val googleSignInFailedMessage = stringResource(R.string.auth_google_signin_failed)
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
                viewModel.onGoogleSignInCancelled(googleMissingTokenMessage)
            } else {
                viewModel.signInWithGoogle(idToken, onClose)
            }
        } catch (exception: ApiException) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled(exception.toReadableGoogleMessage())
        } catch (_: Exception) {
            googleClient.signOut()
            viewModel.onGoogleSignInCancelled(googleSignInFailedMessage)
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
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_login_title),
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
                    text = stringResource(R.string.auth_login_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.auth_login_subtitle),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LoginInfoPill(text = stringResource(R.string.auth_login_badge_account))
                    LoginInfoPill(text = stringResource(R.string.auth_login_badge_google))
                    LoginInfoPill(text = stringResource(R.string.auth_login_badge_secure))
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = stringResource(R.string.auth_login_email_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.auth_login_email_section_subtitle),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text(stringResource(R.string.auth_email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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
                Button(
                    onClick = { viewModel.signIn(onClose) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isAuthBusy,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (uiState.isLoading) stringResource(R.string.auth_login_loading) else stringResource(R.string.auth_sign_in))
                }
                GoogleAuthButton(
                    text = if (uiState.isGoogleLoading) stringResource(R.string.auth_google_loading) else stringResource(R.string.auth_sign_in_google),
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
                    text = stringResource(R.string.auth_login_google_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = onOpenRegistration,
                    modifier = Modifier.padding(top = 10.dp),
                    enabled = !isAuthBusy,
                ) {
                    Text(stringResource(R.string.auth_no_account_register))
                }
            }

            ToastHost(
                message = uiState.errorMessage,
                type = ToastType.Error,
            )
        }
    }
}

@Composable
internal fun LoginInfoPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
