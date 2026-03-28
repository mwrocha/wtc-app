package br.com.fiap.wtcconnecta.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InAppNotification(
    val title: String,
    val body: String,
    val type: String = "DIRECT",
    val chatId: String = "",
    val chatName: String = "",
    val chatType: String = "1on1"
)

object InAppNotificationState {
    private val _notification = MutableStateFlow<InAppNotification?>(null)
    val notification = _notification.asStateFlow()

    fun show(notification: InAppNotification) {
        _notification.value = notification
    }

    fun dismiss() {
        _notification.value = null
    }
}