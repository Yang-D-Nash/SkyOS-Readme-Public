package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrderScreen(
    onClose: () -> Unit,
    viewModel: OrderViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ToastHost(
            message = uiState.errorMessage ?: uiState.successMessage,
            type = if (uiState.errorMessage != null) ToastType.Error else ToastType.Success,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bestellungen",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Admin-Ansicht analog zur iOS-Liste mit Statusumschaltung und Loeschen.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                        )
                    }
                    TextButton(onClick = onClose) {
                        Text("Schliessen")
                    }
                }
            }

            items(uiState.orders, key = { it.id.orEmpty() }) { order ->
                SkydownCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = order.userEmail,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.toggleCompleted(order.id.orEmpty()) }) {
                            Icon(
                                imageVector = if (order.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (order.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        order.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                    item.size?.takeIf { it.isNotBlank() }?.let { size ->
                                        Text(
                                            text = "Groesse: $size",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        )
                                    }
                                }
                                Text("x${item.quantity}")
                            }
                        }
                    }

                    Text(
                        text = formatOrderDate(order.timestampEpochMillis),
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    TextButton(
                        onClick = { viewModel.deleteOrder(order.id.orEmpty()) },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("Loeschen")
                    }
                }
            }
        }
    }
}

private fun formatOrderDate(timestampEpochMillis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(timestampEpochMillis))
}
