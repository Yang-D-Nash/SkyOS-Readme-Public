package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginInput(
    val email: String,
    val password: String,
)
