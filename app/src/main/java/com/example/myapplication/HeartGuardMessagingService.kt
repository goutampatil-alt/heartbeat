package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HeartGuardMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HGMessaging"
        private const val CHANNEL_ID = "heartguard_alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("fcmToken", token)
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not available for token update")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "HeartGuard Alert"
        val body = message.notification?.body ?: "Check the app for details"
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "HeartGuard Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Critical health alerts from HeartGuard"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
