package com.skydown.android.ui.component

import androidx.compose.runtime.compositionLocalOf
import com.skydown.shared.model.User

val LocalSessionUser = compositionLocalOf<User?> { null }
