package com.skydown.android.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.theme.SpotifyGreenContainer
import com.skydown.android.ui.theme.YouTubeDeepRed
import com.skydown.android.ui.theme.YouTubeRedContainer
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.hasCuratedMerchCategory
import com.skydown.shared.model.merchCategoryTitle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MerchandiseCard(
    item: MerchandiseItem,
    onTap: (MerchandiseItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when {
        item.source == "shopify" || !item.shopifyProductId.isNullOrBlank() -> SpotifyGreen
        item.featured -> YouTubeDeepRed
        else -> MaterialTheme.colorScheme.secondary
    }
    val accentContainer = when {
        item.source == "shopify" || !item.shopifyProductId.isNullOrBlank() -> SpotifyGreenContainer
        item.featured -> YouTubeRedContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val displayImageUrls = remember(item.imageUrls, item.customImageOverride) {
        item.customImageOverride.takeIf { it.isNotBlank() }
            ?.let { listOf(it) + item.imageUrls.filterNot { url -> url == it } }
            ?: item.imageUrls
    }
    val pagerState = rememberPagerState(pageCount = { displayImageUrls.size.coerceAtLeast(1) })

    SkydownCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap(item) },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    AsyncImage(
                        model = displayImageUrls.getOrNull(page),
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
                                    accentColor.copy(alpha = 0.08f),
                                    accentContainer.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.86f),
                                ),
                            ),
                        ),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MerchStatePill(
                        text = if (item.available) "Drop live" else "Sold out",
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
                    if (displayImageUrls.drop(1).isNotEmpty()) {
                        MerchStatePill(
                            text = "${displayImageUrls.size} Bilder",
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

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "EUR ${String.format(Locale.US, "%.2f", item.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.94f),
                    )
                }

                if (displayImageUrls.drop(1).isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(displayImageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 9.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (item.available) "Ansehen" else "Produkt",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                )
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
        accentContainer.copy(alpha = 0.96f)
    } else {
        Color.Black.copy(alpha = 0.52f)
    }
    val contentColor = if (isAccent) {
        accentColor.copy(alpha = 0.98f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
