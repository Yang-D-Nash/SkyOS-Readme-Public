package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class ContactRequest(
    val name: String,
    val email: String,
    val whatsAppNumber: String = "",
    val selectedSize: String = "M",
    val selectedQuantity: Int = 1,
    val message: String,
)
