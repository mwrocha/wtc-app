package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
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

    private var currentChatId: String = ""      // conversationId resolvido
    private var currentChatType: String = ""
    private var currentLoggedId: String = ""
    private var originalChatId: String = ""    // ID original passado pelo NavGraph

    fun loadMessages(chatId: String, chatType: String, loggedInUserId: String) {
        currentChatId   = chatId
        originalChatId  = chatId   // guarda o ID original
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
                    // Se chatId é um MongoDB ID (sem "@"), resolve o conversationId real
                    // buscando em getMyConversations e filtrando pela conversa com esse recipientId
                    if (!chatId.contains("@")) {
                        try {
                            val all = repository.getMyConversations()
                            // Filtra mensagens onde recipientId ou senderId é esse ID
                            val filtered = all.filter {
                                it.recipientId == chatId || it.senderId == chatId ||
                                        it.conversationId?.contains(chatId) == true
                            }
                            // Se achou, atualiza o currentChatId para o conversationId correto
                            if (filtered.isNotEmpty()) {
                                val realConversationId = filtered.first().conversationId
                                if (realConversationId != null) {
                                    currentChatId = realConversationId
                                }
                            }
                            filtered
                        } catch (e: Exception) { emptyList() }
                    } else {
                        // chatId já é um conversationId (email_email) — busca direto
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

            // Resolve nomes dos remetentes
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

    // Polling a cada 5s
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

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newContent)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(content = newContent, edited = true)
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
            try {
                val success = if (chatType == "group") {
                    repository.sendGroupMessage(chatId, text)
                } else {
                    // Para 1on1: usa o originalChatId (pode ser MongoDB ID do destinatário)
                    // ou extrai o email do destinatário a partir do conversationId
                    val receiverId = resolveReceiverId(senderId)
                    repository.sendMessage(receiverId = receiverId, content = text)
                }
                if (success) {
                    val idToFetch = if (currentChatId.isNotBlank()) currentChatId else chatId
                    fetchMessages(idToFetch, chatType, senderId)
                } else {
                    _uiState.update { it.copy(error = "Falha ao enviar mensagem.") }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Erro ao enviar: ${e.message}")
                _uiState.update { it.copy(error = "Erro ao enviar mensagem.") }
            }
        }
    }

    // Resolve o recipientId correto para envio de mensagem
    // Se o conversationId é "emailA_emailB", retorna o email do outro participante
    private fun resolveReceiverId(senderEmail: String): String {
        val convId = currentChatId
        // Se o chatId original não tem "@", é um MongoDB ID — usa diretamente
        if (!originalChatId.contains("@")) return originalChatId
        // Se o conversationId tem o formato "email_email", extrai o email do outro
        if (convId.contains("@") && convId.contains("_")) {
            val parts = convId.split("_")
            // Reconstrói emails (podem conter "_" no domínio, mas emails geralmente não)
            // Tenta encontrar o email que não é o sender
            val emailA = parts.take(parts.size / 2 + 1).joinToString("_")
            val emailB = parts.drop(parts.size / 2 + 1).joinToString("_")
            return when {
                emailA == senderEmail -> emailB
                emailB == senderEmail -> emailA
                else -> {
                    // Abordagem mais robusta: divide no "_" entre os dois emails
                    val atPositions = convId.indices.filter { convId[it] == '@' }
                    if (atPositions.size >= 2) {
                        val splitPoint = convId.indexOf("_", atPositions[0])
                        val emailFirst  = convId.substring(0, splitPoint)
                        val emailSecond = convId.substring(splitPoint + 1)
                        if (emailFirst == senderEmail) emailSecond else emailFirst
                    } else originalChatId
                }
            }
        }
        return originalChatId
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}