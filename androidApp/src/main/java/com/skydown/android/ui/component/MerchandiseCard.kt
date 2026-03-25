package com.skydown.android.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.skydown.shared.model.MerchandiseItem
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MerchandiseCard(
    item: MerchandiseItem,
    onTap: (MerchandiseItem) -> Unit,
    onEdit: ((MerchandiseItem) -> Unit)? = null,
    onDelete: ((MerchandiseItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { item.imageUrls.size.coerceAtLeast(1) })
    val hasAdminActions = onEdit != null || onDelete != null

    SkydownCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap(item) },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(18.dp)),
            ) { page ->
                AsyncImage(
                    model = item.imageUrls.getOrNull(page),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )
            }

            if (item.imageUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(item.imageUrls.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                    },
                                ),
                        )
                    }
                }
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "EUR ${String.format(Locale.US, "%.2f", item.price)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (hasAdminActions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onEdit?.let { editAction ->
                    TextButton(
                        onClick = { editAction(item) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Bearbeiten")
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))

                onDelete?.let { deleteAction ->
                    Button(
                        onClick = { deleteAction(item) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Loeschen")
                    }
                }
            }
        }
    }
}
