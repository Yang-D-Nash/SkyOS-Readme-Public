package com.nash.skyos.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownCardTitleTextStyle
import com.nash.skyos.ui.theme.SkydownEditorialCaptionTextStyle
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownError
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownSuccess
import com.nash.skyos.ui.theme.skydownText

/**
 * Central form field for the premium brand system.
 *
 * Screens should prefer this over raw OutlinedTextField so focus, contrast,
 * shape, helper copy, and dense vertical rhythm stay consistent.
 */
@Composable
fun SkydownPremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val borderIdle = colorScheme.outline.copy(alpha = if (isDarkPalette) 0.62f else 0.74f)
    val borderFocused = colorScheme.primary.copy(alpha = if (isDarkPalette) 0.92f else 0.86f)
    val container = colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.76f else 0.82f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary.copy(alpha = if (enabled) 0.82f else 0.38f),
                )
            }
        },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.takeIf { it.isNotBlank() }?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) colorScheme.skydownError() else colorScheme.skydownSecondaryText(),
                )
            }
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colorScheme.skydownText(),
            unfocusedTextColor = colorScheme.skydownText(),
            disabledTextColor = colorScheme.skydownSecondaryText().copy(alpha = 0.48f),
            cursorColor = colorScheme.primary,
            focusedBorderColor = borderFocused,
            unfocusedBorderColor = borderIdle,
            disabledBorderColor = borderIdle.copy(alpha = 0.34f),
            errorBorderColor = colorScheme.skydownError(),
            focusedContainerColor = container,
            unfocusedContainerColor = container.copy(alpha = 0.76f),
            disabledContainerColor = colorScheme.skydownSecondaryBackground().copy(alpha = 0.46f),
            errorContainerColor = colorScheme.errorContainer.copy(alpha = if (isDarkPalette) 0.28f else 0.34f),
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.skydownSecondaryText().copy(alpha = 0.80f),
            disabledLabelColor = colorScheme.skydownSecondaryText().copy(alpha = 0.42f),
            errorLabelColor = colorScheme.skydownError(),
        ),
    )
}

@Composable
fun SkydownPremiumStatePanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.AutoAwesome,
    accent: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    loading: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    SkydownCard(
        modifier = modifier,
        contentPadding = PaddingValues(SkydownUiTokens.panelPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = accent.copy(alpha = if (colorScheme.skydownIsDarkPalette()) 0.18f else 0.12f),
                contentColor = accent,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
                shadowElevation = 3.dp,
                tonalElevation = 0.dp,
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent,
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            ) {
                Text(
                    text = title,
                    style = SkydownCardTitleTextStyle,
                    color = colorScheme.skydownText(),
                )
                Text(
                    text = body,
                    style = SkydownBodyCaptionTextStyle,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.86f),
                )
                if (actionLabel != null && onAction != null) {
                    BrandActionButton(
                        text = actionLabel,
                        onClick = onAction,
                        accent = accent,
                        filled = false,
                        compact = true,
                        modifier = Modifier.padding(top = SkydownUiTokens.stackSpacingNano),
                    )
                }
            }
        }
    }
}

@Composable
fun SkydownPremiumMicrocopy(
    text: String,
    modifier: Modifier = Modifier,
    positive: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = text,
        modifier = modifier,
        style = SkydownEditorialCaptionTextStyle,
        color = if (positive) {
            colorScheme.skydownSuccess()
        } else {
            colorScheme.skydownSecondaryText().copy(alpha = 0.74f)
        },
    )
}
