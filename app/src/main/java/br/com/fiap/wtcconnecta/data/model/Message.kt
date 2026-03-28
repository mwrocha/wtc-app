package br.com.fiap.wtcconnecta.data.model

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
    val content: String = ""
) {
    // Resolve o texto da mensagem em ordem de prioridade
    val displayContent: String get() = content.ifBlank { body ?: text ?: "" }
}