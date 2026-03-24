package com.skydown.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String? = null,
    val email: String,
    val username: String,
    val whatsApp: String? = null,
    val registrationDateEpochMillis: Long,
    val isAdmin: Boolean = false,
)

fun sampleUser(): User = User(
    id = "demo-user",
    email = "demo@skydown.app",
    username = "Yang D. Nash",
    whatsApp = "+49 170 0000000",
    registrationDateEpochMillis = 1_725_000_000_000,
    isAdmin = true,
)
