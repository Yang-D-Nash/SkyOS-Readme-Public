package com.nash.skyos.ui.screen

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

fun openExternalUri(
    context: Context,
    uri: Uri,
    missingMessage: String = "Kein passender Handler gefunden.",
) {
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (!tryStartActivity(context, intent)) {
        Toast.makeText(context, missingMessage, Toast.LENGTH_SHORT).show()
    }
}

fun openExternalIntent(
    context: Context,
    intent: Intent,
    missingMessage: String = "Aktion konnte nicht geoeffnet werden.",
) {
    val safeIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (!tryStartActivity(context, safeIntent)) {
        Toast.makeText(context, missingMessage, Toast.LENGTH_SHORT).show()
    }
}

fun openEmailDraft(
    context: Context,
    recipients: List<String>,
    subject: String,
    body: String,
    chooserTitle: String = "Mail-App waehlen",
    emailAppMissingMessage: String = "Keine Mail-App gefunden.",
) {
    val cleanedRecipients = recipients
        .map(String::trim)
        .filter(String::isNotBlank)

    if (cleanedRecipients.isEmpty()) {
        Toast.makeText(context, emailAppMissingMessage, Toast.LENGTH_SHORT).show()
        return
    }

    val baseIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, cleanedRecipients.toTypedArray())
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooserIntent = Intent.createChooser(baseIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (tryStartActivity(context, chooserIntent)) {
        return
    }

    val fallbackIntent = Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("mailto:${cleanedRecipients.joinToString(",")}").buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build(),
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (!tryStartActivity(context, fallbackIntent)) {
        Toast.makeText(context, emailAppMissingMessage, Toast.LENGTH_SHORT).show()
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
    } catch (_: SecurityException) {
        false
    } catch (_: RuntimeException) {
        false
    }
}
