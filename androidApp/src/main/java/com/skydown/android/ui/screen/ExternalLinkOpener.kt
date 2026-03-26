package com.skydown.android.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openExternalLink(
    context: Context,
    url: String,
    browserMissingMessage: String = "Kein Browser gefunden.",
) {
    val normalizedUrl = url.trim()
    if (normalizedUrl.isBlank()) {
        Toast.makeText(context, browserMissingMessage, Toast.LENGTH_SHORT).show()
        return
    }

    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (normalizedUrl.contains("instagram.com", ignoreCase = true)) {
        val instagramIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
            setPackage("com.instagram.android")
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (tryStartActivity(context, instagramIntent)) {
            return
        }
    }

    if (!tryStartActivity(context, webIntent)) {
        Toast.makeText(context, browserMissingMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun tryStartActivity(
    context: Context,
    intent: Intent,
): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
