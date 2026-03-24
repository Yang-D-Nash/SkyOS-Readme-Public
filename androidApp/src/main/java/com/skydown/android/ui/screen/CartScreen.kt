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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.viewmodel.CartViewModel
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CartScreen(
    onOpenLogin: () -> Unit = {},
    viewModel: CartViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Warenkorb",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Aufbau wie in der iOS-Form: Cart-Items, Kontaktdaten, Nachricht und Versandbestaetigung.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }

            if (!uiState.isLoggedIn) {
                item {
                    SkydownCard {
                        Text("Du bist nicht angemeldet.", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = onOpenLogin,
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("Anmelden")
                        }
                    }
                }
            } else {
                item {
                    SkydownCard {
                        SectionHeader("Dein Warenkorb")
                        if (uiState.items.isEmpty()) {
                            Text(
                                text = "Dein Warenkorb ist leer.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                }

                items(uiState.items, key = { "${it.item.id}-${it.size}" }) { cartItem ->
                    SkydownCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(cartItem.item.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Groesse: ${cartItem.size}, Anzahl: ${cartItem.quantity}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                            Text(
                                text = "EUR ${String.format(Locale.US, "%.2f", cartItem.item.price * cartItem.quantity)}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Button(
                            onClick = { viewModel.removeItem(cartItem.item.id.orEmpty(), cartItem.size) },
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("Entfernen")
                        }
                    }
                }

                item {
                    SkydownCard {
                        SectionHeader("Deine Kontaktdaten")
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = { Text("Name*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        )
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = viewModel::updateEmail,
                            label = { Text("E-Mail*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        )
                        OutlinedTextField(
                            value = uiState.whatsApp,
                            onValueChange = viewModel::updateWhatsApp,
                            label = { Text("WhatsApp Nummer (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                        )
                    }
                }

                item {
                    SkydownCard {
                        SectionHeader("Nachricht")
                        OutlinedTextField(
                            value = uiState.message,
                            onValueChange = viewModel::updateMessage,
                            label = { Text("Nachricht") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            minLines = 4,
                        )
                    }
                }

                item {
                    SkydownCard {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.submitOrder()
                                }
                            },
                            enabled = viewModel.isFormValid() && !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.isSubmitting) "Sende Bestellung..." else "Bestellung abschicken")
                        }
                        Text(
                            text = "Sie werden in den naechsten Minuten per E-Mail oder WhatsApp kontaktiert.",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}
