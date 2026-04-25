package com.nash.skyos.ui.theme

enum class AppearanceMode(
    val storageValue: String,
    val label: String,
) {
    Light("light", "Light"),
    Dark("dark", "Dark"),
    System("system", "System"),
    ;

    fun shouldUseDarkTheme(systemIsDark: Boolean): Boolean {
        return when (this) {
            Light -> false
            Dark -> true
            System -> systemIsDark
        }
    }

    companion object {
        fun fromStorageValue(value: String?): AppearanceMode {
            return entries.firstOrNull { it.storageValue == value } ?: System
        }
    }
}
