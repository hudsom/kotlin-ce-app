package com.hudsom.kotlinceapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ServicoNotificacaoLocal(
    private val contexto: Context,
    params: WorkerParameters
) : Worker(contexto, params) {

    companion object {
        const val CANAL_ID = "canal_local"
        const val CANAL_NOME = "Notificações Locais"
    }

    override fun doWork(): Result {
        exibirNotificacao()
        return Result.success()
    }

    private fun exibirNotificacao() {
        val manager = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val canal = NotificationChannel(CANAL_ID, CANAL_NOME, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(canal)

        val intent = Intent(contexto, TelaPrincipalActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            contexto, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(contexto, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(contexto.getString(R.string.notificacao_titulo))
            .setContentText(contexto.getString(R.string.notificacao_mensagem))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(1001, notificacao)
    }
}
