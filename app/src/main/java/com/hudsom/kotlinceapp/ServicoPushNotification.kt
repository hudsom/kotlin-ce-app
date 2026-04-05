package com.hudsom.kotlinceapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ServicoPushNotification : FirebaseMessagingService() {

    companion object {
        const val CANAL_ID = "canal_push"
        const val CANAL_NOME = "Notificações Push"
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Novo token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: "Kotlin CE App"
        val corpo = message.notification?.body ?: ""
        exibirNotificacao(titulo, corpo)
    }

    private fun exibirNotificacao(titulo: String, corpo: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val canal = NotificationChannel(CANAL_ID, CANAL_NOME, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(canal)

        val intent = Intent(this, TelaPrincipalActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notificacao)
    }
}
