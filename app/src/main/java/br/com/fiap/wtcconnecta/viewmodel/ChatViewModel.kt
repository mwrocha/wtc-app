package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.MessageStatus
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionError: String? = null,
    val currentClient: Client? = null,
    val senderNames: Map<String, String> = emptyMap()
)

class ChatViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var currentChatId: String = ""
    private var currentChatType: String = ""
    private var currentLoggedId: String = ""
    private var originalChatId: String = ""

    fun loadMessages(chatId: String, chatType: String, loggedInUserId: String) {
        currentChatId   = chatId
        originalChatId  = chatId
        currentChatType = chatType
        currentLoggedId = loggedInUserId

        viewModelScope.launch {
            Log.d("ChatViewModel", "Carregando mensagens chatId=$chatId, tipo=$chatType")
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchMessages(chatId, chatType, loggedInUserId)
        }
    }

    private suspend fun fetchMessages(
        chatId: String,
        chatType: String,
        loggedInUserId: String
    ) {
        try {
            val messages: List<Message> = when (chatType) {
                "group" -> repository.getConversation(chatId)
                else    -> {
                    if (chatId.contains("@") && !chatId.contains("_")) {
                        emptyList()
                    } else if (!chatId.contains("@")) {
                        try {
                            val all = repository.getMyConversations()
                            val filtered = all.filter {
                                it.recipientId == chatId || it.senderId == chatId ||
                                        it.conversationId?.contains(chatId) == true
                            }
                            if (filtered.isNotEmpty()) {
                                val realConversationId = filtered.first().conversationId
                                val loggedEmail = getLoggedEmail()
                                // Só atualiza currentChatId se o conversationId contém
                                // o email do usuário logado — evita contaminação
                                if (realConversationId != null &&
                                    (loggedEmail == null || realConversationId.contains(loggedEmail))) {
                                    currentChatId = realConversationId
                                }
                            }
                            filtered
                        } catch (e: Exception) { emptyList() }
                    } else {
                        val byConversation = try {
                            repository.getConversation(chatId)
                        } catch (e: Exception) { emptyList() }

                        if (byConversation.isEmpty()) {
                            try {
                                repository.getMyConversations()
                                    .filter { it.conversationId == chatId }
                            } catch (e: Exception) { emptyList() }
                        } else {
                            byConversation
                        }
                    }
                }
            }

            val knownIds = _uiState.value.senderNames.keys
            val unknownSenders = messages
                .map { it.senderId }
                .filter { it.isNotBlank() && it !in knownIds }
                .distinct()

            val newNames = mutableMapOf<String, String>()
            for (senderId in unknownSenders) {
                try {
                    val user = repository.getUserByEmail(senderId)
                    if (user != null) newNames[senderId] = user.name
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Não encontrou nome para $senderId")
                }
            }

            _uiState.update {
                it.copy(
                    isLoading   = false,
                    messages    = messages.sortedBy { msg -> msg.createdAt },
                    senderNames = it.senderNames + newNames,
                    error       = null
                )
            }

        } catch (e: HttpException) {
            Log.e("ChatViewModel", "Erro HTTP ${e.code()}: ${e.message()}")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (e.code() == 404) "Nenhuma mensagem encontrada." else "Erro ao carregar mensagens."
                )
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Erro inesperado: ${e.message}")
            _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar mensagens.") }
        }
    }

    fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                if (currentChatId.isNotBlank()) {
                    try {
                        fetchMessages(currentChatId, currentChatType, currentLoggedId)
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Polling erro: ${e.message}")
                    }
                }
            }
        }
    }

    fun markConversationAsRead(conversationId: String) {
        if (conversationId.isBlank()) return
        viewModelScope.launch {
            try {
                RetrofitClient.instance.markConversationAsRead(conversationId)
                Log.d("ChatViewModel", "Conversa marcada como lida: $conversationId")
            } catch (e: Exception) {
                Log.w("ChatViewModel", "Erro ao marcar como lida: ${e.message}")
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newContent)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(contentRaw = newContent, edited = true)
                            else msg
                        })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionError = e.message ?: "Erro ao editar mensagem") }
                }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != messageId })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionError = e.message ?: "Erro ao excluir mensagem") }
                }
        }
    }

    fun clearActionError() { _uiState.update { it.copy(actionError = null) } }

    fun getSenderName(senderId: String): String =
        _uiState.value.senderNames[senderId] ?: senderId

    fun sendMessage(text: String, chatId: String, chatType: String, senderId: String) {
        if (text.isBlank()) return
        viewModelScope.launch {

            val tempId = "temp_${System.currentTimeMillis()}"
            val tempMessage = Message(
                id             = tempId,
                body           = text,
                senderId       = senderId,
                conversationId = currentChatId.ifBlank { chatId },
                createdAt      = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                statusRaw      = MessageStatus.SENDING.name
            )
            _uiState.update { state ->
                state.copy(messages = state.messages + tempMessage)
            }

            try {
                val success = if (chatType == "group") {
                    repository.sendGroupMessage(chatId, text)
                } else {
                    val receiverId = resolveReceiverId(senderId)
                    repository.sendMessage(receiverId = receiverId, content = text)
                }

                if (success) {
                    val idToFetch = if (currentChatId.isNotBlank()) currentChatId else chatId
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != tempId })
                    }
                    fetchMessages(idToFetch, chatType, senderId)
                } else {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == tempId) msg.copy(statusRaw = MessageStatus.FAILED.name)
                            else msg
                        })
                    }
                    _uiState.update { it.copy(error = "Falha ao enviar mensagem.") }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Erro ao enviar: ${e.message}")
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == tempId) msg.copy(statusRaw = MessageStatus.FAILED.name)
                        else msg
                    })
                }
                _uiState.update { it.copy(error = "Erro ao enviar mensagem.") }
            }
        }
    }

    private fun resolveReceiverId(senderEmail: String): String {
        // Caso 1: chatId é diretamente o email do operador (sem histórico ainda)
        if (originalChatId.contains("@") && !originalChatId.contains("_")) {
            return originalChatId
        }

        // Caso 2: chatId não é email — usa como recipientId direto (ID de grupo etc)
        if (!originalChatId.contains("@")) return originalChatId

        // Caso 3: conversationId no formato email_email — extrai o outro email
        val convId = currentChatId.ifBlank { originalChatId }
        if (convId.contains("@") && convId.contains("_")) {
            val firstAt    = convId.indexOf("@")
            val splitPoint = convId.indexOf("_", firstAt)
            if (splitPoint > 0) {
                val emailA = convId.substring(0, splitPoint)
                val emailB = convId.substring(splitPoint + 1)
                return when {
                    emailA.equals(senderEmail, ignoreCase = true) -> emailB
                    emailB.equals(senderEmail, ignoreCase = true) -> emailA
                    else -> {
                        // Nenhum email corresponde ao remetente — conversationId contaminado
                        // Fallback seguro para o chatId original
                        Log.w("ChatViewModel", "resolveReceiverId: convId contaminado " +
                                "convId=$convId sender=$senderEmail → fallback=$originalChatId")
                        originalChatId
                    }
                }
            }
        }
        return originalChatId
    }

    // ── Extrai o email do usuário logado a partir do JWT ──────────────────────
    private fun getLoggedEmail(): String? {
        val token = RetrofitClient.authToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(
                payload.padEnd((payload.length + 3) / 4 * 4, '='),
                android.util.Base64.URL_SAFE
            )
            val json = String(decoded)
            val start = json.indexOf("\"sub\"") + 7
            val end   = json.indexOf("\"", start)
            if (start > 6 && end > start) json.substring(start, end) else null
        } catch (e: Exception) { null }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}