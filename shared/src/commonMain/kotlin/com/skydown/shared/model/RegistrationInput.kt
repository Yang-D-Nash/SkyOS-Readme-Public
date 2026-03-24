package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationInput(
    val username: String,
    val email: String,
    val whatsApp: String,
    val password: String,
    val confirmPassword: String,
)
