package com.nash.skyos.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationPermissionCoordinator {
    private const val prefsName = "skydown.permissions"
    private const val notificationPromptedKey = "notifications.prompted.once"

    fun shouldRequestOnLaunch(context: Context): Boolean {
        if (!requiresRuntimePermission()) {
            return false
        }
        if (hasRuntimePermission(context)) {
            return false
        }
        return !wasPromptedBefore(context)
    }

    fun requiresRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasRuntimePermission(context: Context): Boolean {
        if (!requiresRuntimePermission()) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled() && hasRuntimePermission(context)
    }

    fun markPrompted(context: Context) {
        context.applicationContext
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(notificationPromptedKey, true)
            .apply()
    }

    fun openNotificationSettings(context: Context) {
        val appContext = context.applicationContext
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            putExtra("android.provider.extra.APP_PACKAGE", appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            appContext.startActivity(intent)
        }.onFailure {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(fallbackIntent)
        }
    }

    private fun wasPromptedBefore(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getBoolean(notificationPromptedKey, false)
    }
}
