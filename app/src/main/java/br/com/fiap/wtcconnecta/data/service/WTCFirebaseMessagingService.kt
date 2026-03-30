package br.com.fiap.wtcconnecta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import br.com.fiap.wtcconnecta.R
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WTCFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "wtc_messages"
        const val CHANNEL_NAME = "Mensagens WTC"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"] ?: "WTC Connecta"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"] ?: ""
        val type           = remoteMessage.data["type"]           ?: "DIRECT"
        val conversationId = remoteMessage.data["conversationId"] ?: ""
        val groupId        = remoteMessage.data["groupId"]        ?: ""
        val senderId       = remoteMessage.data["senderId"]       ?: ""

        // Ignorar notificação se o remetente for o próprio usuário logado
        val loggedUserEmail = getEmailFromToken(RetrofitClient.authToken)
        if (senderId.isNotBlank() && loggedUserEmail != null && senderId == loggedUserEmail) {
            Log.d("FCM", "Push ignorado — remetente é o próprio usuário logado")
            return
        }

        // GROUP_REQUEST — solicitação de troca de grupo (operador)
        if (type == "GROUP_REQUEST") {
            Log.d("FCM", "Push de solicitação de grupo recebido")
            InAppNotificationState.show(
                InAppNotification(
                    title    = title,
                    body     = body,
                    type     = "GROUP_REQUEST",
                    chatId   = "group_requests",
                    chatName = "Solicitações de Grupo",
                    chatType = "group_request"
                )
            )
            showSystemNotification(title, body)
            return
        }

        val chatId   = if (type == "GROUP") groupId else conversationId
        val chatName = if (type == "GROUP") "Grupo" else "Atendimento WTC"
        val chatType = if (type == "GROUP") "group" else "1on1"

        Log.d("FCM", "Push recebido: type=$type chatId=$chatId")

        // Banner in-app clicável com os dados do chat
        InAppNotificationState.show(
            InAppNotification(
                title    = title,
                body     = body,
                type     = type,
                chatId   = chatId,
                chatName = chatName,
                chatType = chatType
            )
        )

        // Notificação simples do sistema (sem deep link por agora)
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        createNotificationChannel()
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_login)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificações de mensagens do WTC Connecta" }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getEmailFromToken(token: String?): String? {
        if (token == null) return null
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(
                payload.padEnd((payload.length + 3) / 4 * 4, '='),
                android.util.Base64.URL_SAFE
            )
            val json = String(decoded)
            // Extrai o campo sub do JWT payload
            val start = json.indexOf("\"sub\"") + 7
            val end = json.indexOf("\"", start)
            if (start > 6 && end > start) json.substring(start, end) else null
        } catch (e: Exception) { null }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Novo token: $token")
        RetrofitClient.authToken ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { RetrofitClient.instance.updateFcmToken(mapOf("token" to token)) }
            catch (e: Exception) { Log.e("FCM", "Erro token: ${e.message}") }
        }
    }
}