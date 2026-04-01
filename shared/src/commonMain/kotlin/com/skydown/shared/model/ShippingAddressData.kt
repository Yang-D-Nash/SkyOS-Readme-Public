package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class ShippingAddressData(
    val address1: String,
    val address2: String = "",
    val city: String,
    val zip: String,
    val countryCode: String,
    val countryName: String,
)
