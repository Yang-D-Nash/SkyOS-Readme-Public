package com.nash.skyos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nash.skyos.R
import com.skydown.shared.text.OwnerHubPrompts
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.SkydownPremiumIconAction
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownPanelSurface
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.theme.SkydownPanelTitleTextStyle
import com.nash.skyos.ui.theme.SkydownSectionTitleTextStyle
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownText
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHubScreen(
    onBack: () -> Unit,
    hasAiAccess: Boolean,
    onOpenAgentWithPrompt: (String) -> Unit,
) {
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val colorScheme = MaterialTheme.colorScheme
    val scroll = rememberScrollState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = stringResource(R.string.owner_hub_nav_title),
                    )
                },
                navigationIcon = {
                    SkydownPremiumIconAction(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        onClick = onBack,
                        modifier = Modifier.padding(start = 4.dp),
                        accent = MaterialTheme.colorScheme.primary,
                        size = 40.dp,
                        iconSize = 19.dp,
                    )
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("owner.hub.root")
                .background(
                    skydownScreenBrush(
                        secondaryColor = colorScheme.skydownAccentMystic(),
                        primaryAlpha = 0.028f,
                        secondaryAlpha = 0.018f,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(skydownContentPadding(innerPadding))
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            ) {
                Text(
                    text = stringResource(R.string.owner_hub_title),
                    style = SkydownSectionTitleTextStyle,
                    color = colorScheme.skydownText(),
                )
                Text(
                    text = stringResource(R.string.owner_hub_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.skydownSecondaryText().copy(alpha = 0.78f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                OwnerHubMetricCard(
                    title = stringResource(R.string.owner_hub_card_health_title),
                    subtitle = stringResource(R.string.owner_hub_card_health_subtitle),
                    accent = colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                OwnerHubMetricCard(
                    title = stringResource(R.string.owner_hub_card_growth_title),
                    subtitle = stringResource(R.string.owner_hub_card_growth_subtitle),
                    accent = SpotifyGreen,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                OwnerHubMetricCard(
                    title = stringResource(R.string.owner_hub_card_quality_title),
                    subtitle = stringResource(R.string.owner_hub_card_quality_subtitle),
                    accent = colorScheme.skydownAccentHighlight(),
                    modifier = Modifier.weight(1f),
                )
                OwnerHubMetricCard(
                    title = stringResource(R.string.owner_hub_card_release_title),
                    subtitle = stringResource(R.string.owner_hub_card_release_subtitle),
                    accent = colorScheme.skydownAccentMystic(),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .skydownPanelSurface(
                        accent = colorScheme.skydownAccentMystic(),
                        cornerRadius = SkydownUiTokens.cardCornerRadius,
                        shadowRadius = 8.dp,
                        shadowYOffset = 4.dp,
                    )
                    .padding(SkydownUiTokens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
            ) {
                Text(
                    text = stringResource(R.string.owner_hub_briefing_title),
                    style = SkydownPanelTitleTextStyle,
                    color = colorScheme.onSurface,
                    modifier = Modifier.testTag("owner.hub.briefing.title"),
                )
                Text(
                    text = stringResource(R.string.owner_hub_briefing_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface.copy(alpha = 0.72f),
                )
                BrandActionButton(
                    text = stringResource(R.string.owner_hub_briefing_cta),
                    onClick = { onOpenAgentWithPrompt(OwnerHubPrompts.dailyBriefing) },
                    accent = colorScheme.skydownAccentMystic(),
                    enabled = hasAiAccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner.hub.briefing.cta"),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            ) {
                Text(
                    text = stringResource(R.string.owner_hub_roadmap_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = colorScheme.onBackground.copy(alpha = 0.55f),
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Text(
                        text = stringResource(R.string.owner_hub_roadmap_action_queue),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onBackground.copy(alpha = 0.68f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                    Text(
                        text = stringResource(R.string.owner_hub_roadmap_weekly_pdf),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onBackground.copy(alpha = 0.68f),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun OwnerHubMetricCard(
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = 120.dp)
            .skydownPanelSurface(
                accent = accent,
                cornerRadius = 18.dp,
                shadowRadius = 7.dp,
                shadowYOffset = 3.dp,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.owner_hub_metric_placeholder),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
        )
    }
}
