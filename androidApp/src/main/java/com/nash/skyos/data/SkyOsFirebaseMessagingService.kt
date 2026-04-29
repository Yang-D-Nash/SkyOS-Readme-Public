package com.nash.skyos.data

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nash.skyos.productivity.ProductivityReminderNotificationCenter

class SkyOsFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenRegistry.cacheToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val reminderId = message.data["reminderId"].orEmpty().ifBlank { message.messageId.orEmpty() }
        if (message.data["type"] != "reminder" || reminderId.isBlank()) {
            return
        }
        val title = message.notification?.body
            ?: message.notification?.title
            ?: message.data["title"]
            ?: message.data["body"]
            ?: return
        ProductivityReminderNotificationCenter.showNotification(
            context = this,
            reminderId = reminderId,
            title = title,
        )
    }
}
