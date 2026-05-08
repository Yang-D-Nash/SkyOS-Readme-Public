package com.nash.skyos.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownCardTitleTextStyle
import com.nash.skyos.ui.theme.SkydownEditorialCaptionTextStyle
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownCinematicShadow
import com.nash.skyos.ui.theme.skydownError
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownLuminanceLift
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownSheetScrim
import com.nash.skyos.ui.theme.skydownSheetSurface
import com.nash.skyos.ui.theme.skydownStateIconSurface
import com.nash.skyos.ui.theme.skydownSuccess
import com.nash.skyos.ui.theme.skydownText

/**
 * Central form field for the premium brand system.
 *
 * Screens should prefer this over raw text fields so focus, contrast,
 * shape, helper copy, and dense vertical rhythm stay consistent.
 */
@Composable
fun SkydownPremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    labelContent: (@Composable () -> Unit)? = null,
    placeholderContent: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val activeAccent = if (isError) colorScheme.skydownError() else colorScheme.primary
    val labelColor = when {
        !enabled -> colorScheme.skydownSecondaryText().copy(alpha = 0.42f)
        isError -> colorScheme.skydownError()
        isFocused -> activeAccent
        else -> colorScheme.skydownSecondaryText().copy(alpha = if (isDarkPalette) 0.82f else 0.78f)
    }
    val textColor = if (enabled) colorScheme.skydownText() else colorScheme.skydownSecondaryText().copy(alpha = 0.54f)
    val borderAlpha = when {
        !enabled -> 0.10f
        isError -> 0.62f
        isFocused -> if (isDarkPalette) 0.46f else 0.38f
        else -> if (isDarkPalette) 0.18f else 0.15f
    }
    val hasLabel = label.isNotBlank() || labelContent != null
    val fieldShape = RoundedCornerShape(SkydownUiTokens.buttonStandardCornerRadius)
    val rowAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    val fieldVerticalPadding = if (singleLine) 0.dp else 12.dp
    val inputTopPadding = if (singleLine) 0.dp else 2.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkydownUiTokens.inputMinHeight),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hasLabel) {
            Box(
                modifier = Modifier.padding(horizontal = 2.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides labelColor) {
                    labelContent?.invoke() ?: Text(
                        text = label,
                        style = SkydownEditorialCaptionTextStyle,
                        color = labelColor,
                        maxLines = 1,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SkydownUiTokens.inputMinHeight)
                .skydownPanelSurface(
                    accent = activeAccent,
                    cornerRadius = SkydownUiTokens.buttonStandardCornerRadius,
                    shadowRadius = if (isFocused) SkydownUiTokens.elevationPanel else SkydownUiTokens.elevationRaised,
                    shadowYOffset = SkydownUiTokens.panelShadowYOffset,
                )
                .border(
                    width = SkydownUiTokens.elevationHairline,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.18f else 0.36f),
                            activeAccent.copy(alpha = borderAlpha),
                            colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) 0.05f else 0.035f),
                        ),
                        start = Offset.Zero,
                        end = Offset(320f, 320f),
                    ),
                    shape = fieldShape,
                )
                .padding(horizontal = 14.dp, vertical = fieldVerticalPadding),
            verticalAlignment = rowAlignment,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = activeAccent.copy(alpha = if (enabled) 0.82f else 0.34f),
                    modifier = Modifier
                        .padding(top = inputTopPadding)
                        .size(20.dp),
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (leadingIcon != null) SkydownUiTokens.buttonIconLabelSpacing else 0.dp,
                        end = if (trailingIcon != null) SkydownUiTokens.buttonIconLabelSpacing else 0.dp,
                        top = inputTopPadding,
                    ),
                enabled = enabled,
                readOnly = readOnly,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(activeAccent),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty() && placeholderContent != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = SkydownUiTokens.buttonIconLabelSpacing),
                            ) {
                                CompositionLocalProvider(
                                    LocalContentColor provides colorScheme.skydownSecondaryText().copy(alpha = 0.58f),
                                ) {
                                    placeholderContent()
                                }
                            }
                        }
                        innerTextField()
                    }
                },
            )

            trailingIcon?.invoke()
        }

        supportingContent?.invoke()
            ?: supportingText?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) colorScheme.skydownError() else colorScheme.skydownSecondaryText(),
                )
            }
    }
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
            SkydownPremiumIconSurface(
                accent = accent,
                loading = loading,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(SkydownUiTokens.stateIconContentSize),
                )
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
fun SkydownPremiumIconSurface(
    accent: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.size(SkydownUiTokens.stateIconSurfaceSize),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.skydownStateIconSurface(accent),
        contentColor = accent,
        border = BorderStroke(SkydownUiTokens.elevationHairline, accent.copy(alpha = 0.18f)),
        shadowElevation = SkydownUiTokens.elevationStateIcon,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SkydownUiTokens.stateIconProgressSize),
                    strokeWidth = SkydownUiTokens.progressStrokeWidth,
                    color = accent,
                )
            } else {
                content()
            }
        }
    }
}

@Composable
fun SkydownPremiumIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    size: Dp = SkydownUiTokens.iconActionSurfaceSize,
    iconSize: Dp = SkydownUiTokens.iconActionContentSize,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    Surface(
        onClick = {
            if (enabled) {
                view.performSkydownHaptic(SkydownHapticKind.Selection)
                onClick()
            }
        },
        enabled = enabled,
        modifier = modifier
            .size(size)
            .skydownPressable(interactionSource = interactionSource, pressedScale = 0.982f),
        shape = shape,
        color = MaterialTheme.colorScheme.skydownCardBackground().copy(alpha = if (enabled) 0.88f else 0.58f),
        contentColor = if (enabled) accent else MaterialTheme.colorScheme.skydownSecondaryText().copy(alpha = 0.52f),
        border = BorderStroke(SkydownUiTokens.elevationHairline, accent.copy(alpha = if (enabled) 0.18f else 0.08f)),
        shadowElevation = if (enabled) SkydownUiTokens.elevationStateIcon else 0.dp,
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun SkydownPremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    val trackColor = if (checked) {
        accent.copy(alpha = if (enabled) 0.92f else 0.42f)
    } else {
        MaterialTheme.colorScheme.skydownSecondaryBackground().copy(alpha = if (enabled) 0.82f else 0.46f)
    }
    val knobColor = if (checked) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.skydownText().copy(alpha = if (enabled) 0.92f else 0.44f)
    }
    val borderColor = if (checked) {
        accent.copy(alpha = if (enabled) 0.36f else 0.14f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.18f else 0.08f)
    }

    Surface(
        onClick = {
            if (enabled) {
                view.performSkydownHaptic(SkydownHapticKind.Selection)
                onCheckedChange(!checked)
            }
        },
        enabled = enabled,
        modifier = modifier
            .width(SkydownUiTokens.switchTrackWidth)
            .height(SkydownUiTokens.switchTrackHeight)
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) "On" else "Off"
            }
            .skydownPressable(interactionSource = interactionSource, pressedScale = 0.982f),
        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
        color = trackColor,
        border = BorderStroke(SkydownUiTokens.elevationHairline, borderColor),
        shadowElevation = if (enabled && checked) SkydownUiTokens.elevationStateIcon else 0.dp,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(SkydownUiTokens.switchTrackPadding),
            horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SkydownUiTokens.switchKnobSize)
                    .clip(CircleShape)
                    .background(knobColor),
            )
        }
    }
}

@Composable
fun SkydownPremiumLinkSurface(
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
    borderAlpha: Float = 0.30f,
    baseColor: Color = MaterialTheme.colorScheme.skydownSecondaryBackground().copy(alpha = 0.76f),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        accent.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.10f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .skydownPressable(interactionSource = interactionSource, pressedScale = 0.986f),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(SkydownUiTokens.elevationHairline, accent.copy(alpha = borderAlpha)),
        interactionSource = interactionSource,
    ) {
        content()
    }
}

@Composable
fun SkydownPremiumInlineSurface(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(SkydownUiTokens.pillSoftRadius),
    borderAlpha: Float = 0.14f,
    containerAlpha: Float = 0.58f,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val surfaceModifier = modifier
        .skydownPressable(interactionSource = interactionSource, pressedScale = if (onClick == null) 1f else 0.986f)
    val resolvedOnClick = onClick?.takeIf { enabled }
    if (resolvedOnClick == null) {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = MaterialTheme.colorScheme.skydownSecondaryBackground().copy(alpha = containerAlpha),
            border = BorderStroke(SkydownUiTokens.elevationHairline, accent.copy(alpha = borderAlpha)),
            content = content,
        )
    } else {
        Surface(
            onClick = resolvedOnClick,
            modifier = surfaceModifier,
            shape = shape,
            color = MaterialTheme.colorScheme.skydownSecondaryBackground().copy(alpha = containerAlpha),
            border = BorderStroke(SkydownUiTokens.elevationHairline, accent.copy(alpha = borderAlpha)),
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
fun skydownPremiumSheetShape(): Shape = RoundedCornerShape(
    topStart = SkydownUiTokens.sheetHeroRadius,
    topEnd = SkydownUiTokens.sheetHeroRadius,
)

@Composable
fun skydownPremiumSheetContainerColor(): Color = MaterialTheme.colorScheme.skydownSheetSurface()

@Composable
fun skydownPremiumSheetContentColor(): Color = MaterialTheme.colorScheme.skydownText()

@Composable
fun skydownPremiumSheetScrimColor(): Color = MaterialTheme.colorScheme.skydownSheetScrim()

@Composable
fun SkydownPremiumSheetDragHandle() {
    Surface(
        modifier = Modifier
            .padding(top = SkydownUiTokens.stackSpacingToast, bottom = SkydownUiTokens.stackSpacingMicro)
            .width(SkydownUiTokens.sheetDragHandleWidth)
            .height(SkydownUiTokens.sheetDragHandleHeight),
        shape = RoundedCornerShape(SkydownUiTokens.sheetDragHandleRadius),
        color = MaterialTheme.colorScheme.skydownSecondaryText().copy(
            alpha = if (MaterialTheme.colorScheme.skydownIsDarkPalette()) 0.32f else 0.24f,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = {},
    )
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

@Composable
fun SkydownPremiumLinearProgress(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    progress: (() -> Float)? = null,
) {
    val trackColor = accent.copy(alpha = if (MaterialTheme.colorScheme.skydownIsDarkPalette()) 0.18f else 0.14f)
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth(),
            color = accent,
            trackColor = trackColor,
        )
    } else {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier.fillMaxWidth(),
            color = accent,
            trackColor = trackColor,
        )
    }
}

@Composable
fun SkydownPremiumCircularProgress(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: androidx.compose.ui.unit.Dp = SkydownUiTokens.progressStrokeWidth,
) {
    CircularProgressIndicator(
        modifier = modifier,
        strokeWidth = strokeWidth,
        color = accent,
        trackColor = accent.copy(alpha = if (MaterialTheme.colorScheme.skydownIsDarkPalette()) 0.18f else 0.14f),
    )
}
