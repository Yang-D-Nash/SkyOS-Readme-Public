package com.nash.skyos.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.hasCuratedMerchCategory
import com.skydown.shared.model.merchCategorySubtitle
import com.skydown.shared.model.merchCategoryTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MerchandiseCard(
    item: MerchandiseItem,
    onTap: (MerchandiseItem) -> Unit,
    modifier: Modifier = Modifier,
    /** First rows in the shelf read slightly larger for scan rhythm (not a separate flow). */
    shelfHighlight: Boolean = false,
    /** Denser, calmer treatment for later rows (curated rhythm, not a new flow). */
    shelfSettled: Boolean = false,
) {
    // SkyOS retail accents only — never third-party brand colors for catalog source.
    val accentColor = if (item.featured) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val accentContainer = if (item.featured) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val displayImageUrls = remember(item.imageUrls, item.customImageOverride) {
        item.customImageOverride.takeIf { it.isNotBlank() }
            ?.let { listOf(it) + item.imageUrls.filterNot { url -> url == it } }
            ?: item.imageUrls
    }
    val safeImageUrls = displayImageUrls.ifEmpty { listOf("") }
    val pagerState = rememberPagerState(pageCount = { safeImageUrls.size })
    val (imageW, imageH, padH) = when {
        shelfHighlight -> Triple(132.dp, 168.dp, 16.dp)
        shelfSettled -> Triple(108.dp, 128.dp, 8.dp)
        else -> Triple(116.dp, 136.dp, 12.dp)
    }
    val imageCorner = when {
        shelfHighlight -> 22.dp
        shelfSettled -> 16.dp
        else -> 18.dp
    }

    SkydownCard(
        modifier = modifier
            .testTag("shop.merch.row")
            .then(
                if (shelfHighlight) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.32f),
                        shape = RoundedCornerShape(26.dp),
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (shelfSettled) {
                    Modifier.alpha(0.95f)
                } else {
                    Modifier
                },
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = padH,
            vertical = padH,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap(item) },
            horizontalArrangement = Arrangement.spacedBy(if (shelfSettled) 8.dp else 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(imageW)
                    .height(imageH)
                    .clip(RoundedCornerShape(imageCorner)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    AsyncImage(
                        model = safeImageUrls.getOrNull(page),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.34f),
                                ),
                            ),
                        ),
                )

                if (safeImageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        repeat(safeImageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 9.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.40f)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MerchStatePill(
                        text = if (item.available) {
                            stringResource(R.string.merch_badge_drop_live)
                        } else {
                            stringResource(R.string.merch_badge_sold_out)
                        },
                        isAccent = item.available,
                        accentColor = accentColor,
                        accentContainer = accentContainer,
                    )
                    if (item.hasCuratedMerchCategory) {
                        MerchStatePill(
                            text = item.merchCategoryTitle,
                            isAccent = false,
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                        )
                    }
                    if (safeImageUrls.size > 1) {
                        MerchStatePill(
                            text = stringResource(R.string.merch_badge_images_count, safeImageUrls.size),
                            isAccent = false,
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                        )
                    }
                    if (item.customBadge.isNotBlank()) {
                        MerchStatePill(
                            text = item.customBadge,
                            isAccent = false,
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                        )
                    }
                }

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.shop_currency_price,
                        item.currency,
                        String.format(java.util.Locale.US, "%.2f", item.price),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (item.hasCuratedMerchCategory) {
                            item.merchCategorySubtitle
                        } else {
                            stringResource(R.string.merch_house_line)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (item.available) {
                            stringResource(R.string.merch_action_details)
                        } else {
                            stringResource(R.string.merch_action_product)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchStatePill(
    text: String,
    isAccent: Boolean,
    accentColor: Color,
    accentContainer: Color,
) {
    val backgroundColor = if (isAccent) {
        accentContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)
    }
    val contentColor = if (isAccent) {
        accentColor.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}
