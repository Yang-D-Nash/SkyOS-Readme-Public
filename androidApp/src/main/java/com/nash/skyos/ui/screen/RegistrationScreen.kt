package com.nash.skyos.ui.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.MembershipAnalyticsTracker
import com.nash.skyos.data.LegalContentSettings
import com.nash.skyos.data.GoogleSignInManager
import com.nash.skyos.ui.component.GoogleAuthButton
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.model.SettingsLegalDocumentType
import com.nash.skyos.ui.model.resolve
import com.nash.skyos.ui.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onClose: () -> Unit,
    growthTracker: MembershipAnalyticsTracker,
    onBusyStateChanged: (Boolean) -> Unit = {},
    viewModel: RegistrationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val legalSettings by AppContainer.legalContentRepository.settings.collectAsStateWithLifecycle()
    val isAuthBusy = uiState.isLoading || uiState.isGoogleLoading
    val context = LocalContext.current
    val activeLegalDocument = remember { mutableStateOf<SettingsLegalDocumentType?>(null) }
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
                viewModel.signInWithGoogle(idToken) {
                    growthTracker.track("signup_complete", surface = "registration_google")
                    onClose()
                }
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

    LaunchedEffect(legalSettings.resolvedLastUpdatedLabel) {
        viewModel.updateLegalVersionLabel(legalSettings.resolvedLastUpdatedLabel)
    }

    LaunchedEffect(Unit) {
        growthTracker.track("signup_start", surface = "registration_sheet")
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
                Text(
                    text = stringResource(
                        R.string.auth_register_legal_version,
                        legalSettings.resolvedLastUpdatedLabel,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                RegistrationConsentRow(
                    text = stringResource(R.string.auth_register_accept_terms),
                    checked = uiState.acceptedTerms,
                    onCheckedChange = viewModel::updateAcceptedTerms,
                    enabled = !isAuthBusy,
                    modifier = Modifier.padding(top = 4.dp),
                )
                RegistrationConsentRow(
                    text = stringResource(R.string.auth_register_accept_privacy),
                    checked = uiState.acceptedPrivacyPolicy,
                    onCheckedChange = viewModel::updateAcceptedPrivacyPolicy,
                    enabled = !isAuthBusy,
                )
                RegistrationConsentRow(
                    text = stringResource(R.string.auth_register_ai_optin),
                    checked = uiState.aiConsentEnabled,
                    onCheckedChange = viewModel::updateAiConsentEnabled,
                    enabled = !isAuthBusy,
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = { activeLegalDocument.value = SettingsLegalDocumentType.TermsAndConditions },
                        enabled = !isAuthBusy,
                    ) {
                        Text(stringResource(R.string.auth_register_open_terms))
                    }
                    TextButton(
                        onClick = { activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy },
                        enabled = !isAuthBusy,
                    ) {
                        Text(stringResource(R.string.auth_register_open_privacy))
                    }
                }
                Button(
                    onClick = {
                        viewModel.register {
                            growthTracker.track("signup_complete", surface = "registration_sheet")
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isAuthBusy && uiState.acceptedTerms && uiState.acceptedPrivacyPolicy,
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
                    enabled = !isAuthBusy && uiState.acceptedTerms && uiState.acceptedPrivacyPolicy,
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

    activeLegalDocument.value?.let { documentType ->
        RegistrationLegalDocumentSheet(
            documentType = documentType,
            legalContent = legalSettings,
            onDismiss = { activeLegalDocument.value = null },
        )
    }
}

@Composable
private fun RegistrationConsentRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationLegalDocumentSheet(
    documentType: SettingsLegalDocumentType,
    legalContent: LegalContentSettings,
    onDismiss: () -> Unit,
) {
    val document = documentType.resolve(legalContent)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 16.dp,
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
                SkydownCard {
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
        }
    }
}
