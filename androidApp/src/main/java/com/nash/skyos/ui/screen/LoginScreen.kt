package com.nash.skyos.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.R
import com.nash.skyos.data.GoogleSignInManager
import com.nash.skyos.ui.component.GoogleAuthButton
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.auth.AuthEntryContext
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownEditorialCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownPanelTitleTextStyle
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownText
import com.nash.skyos.ui.viewmodel.LoginViewModel
import com.google.android.gms.common.api.ApiException

@Composable
private fun authEntrySubtitleRes(context: AuthEntryContext): Int = when (context) {
    AuthEntryContext.DEFAULT -> R.string.auth_login_subtitle
    AuthEntryContext.AI -> R.string.auth_entry_subtitle_ai
    AuthEntryContext.MERCH_SHOP -> R.string.auth_entry_subtitle_merch
    AuthEntryContext.CART -> R.string.auth_entry_subtitle_cart
    AuthEntryContext.SETTINGS -> R.string.auth_entry_subtitle_settings
    AuthEntryContext.MUSIC -> R.string.auth_entry_subtitle_music
}

@Composable
fun LoginScreen(
    onClose: () -> Unit,
    onOpenRegistration: () -> Unit,
    onBusyStateChanged: (Boolean) -> Unit = {},
    entryContext: AuthEntryContext = AuthEntryContext.DEFAULT,
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

    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .skydownAtmosphereBackground(
                primaryAlpha = 0.038f,
                secondaryAlpha = 0.024f,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SkydownUiTokens.screenHorizontalPadding, vertical = SkydownUiTokens.screenTopPadding),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.screenSectionSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_login_title),
                    style = SkydownPanelTitleTextStyle,
                    color = colorScheme.skydownText(),
                )
                TextButton(
                    onClick = onClose,
                    enabled = !isAuthBusy,
                ) {
                    Text(
                        stringResource(R.string.auth_close),
                        style = SkydownBodyCaptionTextStyle,
                        color = colorScheme.skydownSecondaryText().copy(alpha = 0.88f),
                    )
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(SkydownUiTokens.panelPadding)) {
                Text(
                    text = stringResource(R.string.auth_login_welcome),
                    style = SkydownPanelTitleTextStyle,
                    color = colorScheme.skydownText(),
                )
                Text(
                    text = stringResource(authEntrySubtitleRes(entryContext)),
                    style = SkydownBodyCaptionTextStyle,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = stringResource(R.string.feature_status_live_title),
                    style = SkydownEditorialCaptionTextStyle,
                    color = colorScheme.tertiary.copy(alpha = 0.92f),
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = stringResource(R.string.feature_status_live_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.skydownText().copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = stringResource(R.string.feature_status_next_title),
                    style = SkydownEditorialCaptionTextStyle,
                    color = colorScheme.primary.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = stringResource(R.string.feature_status_next_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.76f),
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    LoginInfoPill(text = stringResource(R.string.auth_login_badge_account))
                    LoginInfoPill(text = stringResource(R.string.auth_login_badge_google))
                    LoginInfoPill(text = stringResource(R.string.auth_login_badge_secure))
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(SkydownUiTokens.panelPadding)) {
                Text(
                    text = stringResource(R.string.auth_login_email_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.skydownText(),
                )
                Text(
                    text = stringResource(R.string.auth_login_email_section_subtitle),
                    style = SkydownBodyCaptionTextStyle,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.76f),
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
                    shape = MaterialTheme.shapes.large,
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
                    shape = MaterialTheme.shapes.large,
                )
                Button(
                    onClick = { viewModel.signIn(onClose) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isAuthBusy,
                    shape = MaterialTheme.shapes.large,
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
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.68f),
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
                shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
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
