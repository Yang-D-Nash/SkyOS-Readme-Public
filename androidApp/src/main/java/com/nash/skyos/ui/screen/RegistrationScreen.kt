package com.nash.skyos.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ApiException
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.MembershipAnalyticsTracker
import com.nash.skyos.data.LegalContentSettings
import com.nash.skyos.data.GoogleSignInManager
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.GoogleAuthButton
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownPremiumIconAction
import com.nash.skyos.ui.component.SkydownPremiumMicrocopy
import com.nash.skyos.ui.component.SkydownPremiumSheetDragHandle
import com.nash.skyos.ui.component.SkydownPremiumTextField
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownPremiumSheetContainerColor
import com.nash.skyos.ui.component.skydownPremiumSheetContentColor
import com.nash.skyos.ui.component.skydownPremiumSheetScrimColor
import com.nash.skyos.ui.component.skydownPremiumSheetShape
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.model.SettingsLegalDocumentType
import com.nash.skyos.ui.model.resolve
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownPanelTitleTextStyle
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownText
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

    val colorScheme = MaterialTheme.colorScheme
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val usernameValidationMessage = when {
        !attemptedSubmit -> null
        uiState.username.isBlank() -> stringResource(R.string.auth_validation_username_required)
        else -> null
    }
    val emailValidationMessage = authEmailValidationMessage(
        email = uiState.email,
        attemptedSubmit = attemptedSubmit,
    )
    val passwordValidationMessage = when {
        !attemptedSubmit -> null
        uiState.password.isBlank() -> stringResource(R.string.auth_validation_password_required)
        uiState.password.length < 6 -> stringResource(R.string.auth_validation_password_min_length)
        else -> null
    }
    val confirmPasswordValidationMessage = when {
        !attemptedSubmit -> null
        uiState.confirmPassword.isBlank() -> stringResource(R.string.auth_validation_password_required)
        uiState.password != uiState.confirmPassword -> stringResource(R.string.auth_validation_password_mismatch)
        else -> null
    }
    val consentValidationMessage = when {
        !attemptedSubmit -> null
        !uiState.acceptedTerms || !uiState.acceptedPrivacyPolicy -> stringResource(R.string.auth_validation_consent_required)
        else -> null
    }
    val canSubmitRegistration = usernameValidationMessage == null &&
        emailValidationMessage == null &&
        passwordValidationMessage == null &&
        confirmPasswordValidationMessage == null &&
        consentValidationMessage == null &&
        uiState.username.isNotBlank() &&
        uiState.email.trim().looksLikeEmailAddress() &&
        uiState.password.length >= 6 &&
        uiState.password == uiState.confirmPassword &&
        uiState.acceptedTerms &&
        uiState.acceptedPrivacyPolicy
    val submitRegistration: () -> Unit = {
        attemptedSubmit = true
        if (canSubmitRegistration && !isAuthBusy) {
            focusManager.clearFocus()
            viewModel.register {
                growthTracker.track("signup_complete", surface = "registration_sheet")
                onClose()
            }
        }
    }
    val submitGoogleRegistration: () -> Unit = {
        attemptedSubmit = true
        if (consentValidationMessage == null && !isAuthBusy) {
            viewModel.beginGoogleSignIn()
            googleClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleClient.signInIntent)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .skydownAtmosphereBackground(
                primaryAlpha = 0.038f,
                secondaryAlpha = 0.026f,
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
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_register_title),
                    style = SkydownPanelTitleTextStyle,
                    color = colorScheme.skydownText(),
                )
                BrandActionButton(
                    text = stringResource(R.string.auth_close),
                    onClick = onClose,
                    accent = colorScheme.primary,
                    filled = false,
                    compact = true,
                    enabled = !isAuthBusy,
                )
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(SkydownUiTokens.panelPadding)) {
                Text(
                    text = stringResource(R.string.auth_register_new_account),
                    style = SkydownPanelTitleTextStyle,
                    color = colorScheme.skydownText(),
                )
                Text(
                    text = stringResource(R.string.auth_register_subtitle),
                    style = SkydownBodyCaptionTextStyle,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 10.dp),
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                ) {
                    LoginInfoPill(text = stringResource(R.string.auth_register_badge_profile))
                    LoginInfoPill(text = stringResource(R.string.auth_register_badge_google))
                    LoginInfoPill(text = stringResource(R.string.auth_register_badge_directstart))
                }
            }

            SkydownCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(SkydownUiTokens.panelPadding)) {
                Text(
                    text = stringResource(R.string.auth_register_email_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.skydownText(),
                )
                SkydownPremiumTextField(
                    value = uiState.username,
                    onValueChange = viewModel::updateUsername,
                    label = stringResource(R.string.auth_username),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                    enabled = !isAuthBusy,
                    isError = usernameValidationMessage != null,
                    supportingText = usernameValidationMessage,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                SkydownPremiumTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = stringResource(R.string.auth_email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    enabled = !isAuthBusy,
                    isError = emailValidationMessage != null,
                    supportingText = emailValidationMessage,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                SkydownPremiumTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = stringResource(R.string.auth_password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    enabled = !isAuthBusy,
                    isError = passwordValidationMessage != null,
                    supportingText = passwordValidationMessage,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        SkydownPremiumIconAction(
                            icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.auth_password_hide else R.string.auth_password_show,
                            ),
                            onClick = { passwordVisible = !passwordVisible },
                            accent = colorScheme.skydownSecondaryText().copy(alpha = 0.84f),
                            size = 36.dp,
                            iconSize = 18.dp,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                )
                SkydownPremiumTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::updateConfirmPassword,
                    label = stringResource(R.string.auth_confirm_password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    enabled = !isAuthBusy,
                    isError = confirmPasswordValidationMessage != null,
                    supportingText = confirmPasswordValidationMessage,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        SkydownPremiumIconAction(
                            icon = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (confirmPasswordVisible) R.string.auth_password_hide else R.string.auth_password_show,
                            ),
                            onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                            accent = colorScheme.skydownSecondaryText().copy(alpha = 0.84f),
                            size = 36.dp,
                            iconSize = 18.dp,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            submitRegistration()
                        },
                    ),
                )
                Text(
                    text = stringResource(
                        R.string.auth_register_legal_version,
                        legalSettings.resolvedLastUpdatedLabel,
                    ),
                    style = SkydownBodyCaptionTextStyle,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.76f),
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
                consentValidationMessage?.let { message ->
                    SkydownPremiumMicrocopy(
                        text = message,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                ) {
                    BrandActionButton(
                        text = stringResource(R.string.auth_register_open_terms),
                        onClick = { activeLegalDocument.value = SettingsLegalDocumentType.TermsAndConditions },
                        accent = colorScheme.tertiary,
                        filled = false,
                        compact = true,
                        enabled = !isAuthBusy,
                    )
                    BrandActionButton(
                        text = stringResource(R.string.auth_register_open_privacy),
                        onClick = { activeLegalDocument.value = SettingsLegalDocumentType.PrivacyPolicy },
                        accent = colorScheme.tertiary,
                        filled = false,
                        compact = true,
                        enabled = !isAuthBusy,
                    )
                }
                BrandActionButton(
                    text = if (uiState.isLoading) stringResource(R.string.auth_register_loading) else stringResource(R.string.auth_register),
                    onClick = submitRegistration,
                    accent = colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isAuthBusy,
                    isLoading = uiState.isLoading,
                )
                GoogleAuthButton(
                    text = if (uiState.isGoogleLoading) {
                        stringResource(R.string.auth_google_loading)
                    } else {
                        stringResource(R.string.auth_register_google)
                    },
                    isLoading = uiState.isGoogleLoading,
                    onClick = submitGoogleRegistration,
                    modifier = Modifier.padding(top = 12.dp),
                    enabled = !isAuthBusy,
                )
                SkydownPremiumMicrocopy(
                    text = stringResource(R.string.auth_register_google_hint),
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
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = SkydownUiTokens.stackSpacingHairline),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
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
        shape = skydownPremiumSheetShape(),
        containerColor = skydownPremiumSheetContainerColor(),
        contentColor = skydownPremiumSheetContentColor(),
        scrimColor = skydownPremiumSheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { SkydownPremiumSheetDragHandle() },
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = SkydownUiTokens.screenHorizontalPadding,
                top = SkydownUiTokens.screenTopPadding,
                end = SkydownUiTokens.screenHorizontalPadding,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.screenSectionSpacing),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                    Text(
                        text = document.title,
                        style = SkydownPanelTitleTextStyle,
                        color = MaterialTheme.colorScheme.skydownText(),
                    )
                    Text(
                        text = stringResource(R.string.legal_ui_last_updated, document.updatedAt),
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
