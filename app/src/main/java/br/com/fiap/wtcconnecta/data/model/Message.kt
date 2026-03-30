package br.com.fiap.wtcconnecta.data.model

import com.google.gson.annotations.SerializedName

// ── Enum de status de entrega ─────────────────────────────────────────────────
enum class MessageStatus {
    SENDING,    // Criando localmente no app (antes do servidor confirmar)
    SENT,       // Salvo no servidor
    DELIVERED,  // Destinatário buscou a mensagem
    READ,       // Destinatário abriu e leu
    FAILED;     // Falha no envio

    companion object {
        fun fromString(value: String?): MessageStatus = when (value?.uppercase()) {
            "SENDING"   -> SENDING
            "SENT"      -> SENT
            "DELIVERED" -> DELIVERED
            "READ"      -> READ
            "FAILED"    -> FAILED
            else        -> SENT // fallback para mensagens antigas sem status
        }
    }
}

data class Message(
    val id: String = "",
    val body: String? = null,
    val text: String? = null,
    val title: String? = null,
    val url: String? = null,
    val actions: List<ActionButton>? = null,
    val actionUrls: Map<String, String>? = null,
    val createdAt: String? = null,
    val senderId: String = "",
    val recipientId: String? = null,
    val conversationId: String? = null,
    val groupId: String? = null,
    val type: String? = null,
    val read: Boolean = false,
    val edited: Boolean = false,

    // ── Status de entrega ─────────────────────────────────────────────────────
    @SerializedName("status")
    val statusRaw: String? = null,

    @SerializedName("content")
    val contentRaw: String? = null
) {
    val content: String get() = contentRaw ?: ""
    val displayContent: String get() = contentRaw?.ifBlank { null } ?: body ?: text ?: ""

    // Resolve o status: usa statusRaw do JSON ou infere pelo campo read
    val status: MessageStatus get() = when {
        statusRaw != null -> MessageStatus.fromString(statusRaw)
        read              -> MessageStatus.READ
        else              -> MessageStatus.SENT
    }
}