package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val groups: List<Group> = emptyList(),
    val lastMessages: Map<String, Message> = emptyMap(),
    val lastMessageIds: Map<String, String> = emptyMap(),
    val conversationIds: Map<String, String> = emptyMap(),
    val operatorEmail: String? = null,
    val error: String? = null
)

class ConversationListViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState = _uiState.asStateFlow()

    private val _newMessageEvent = MutableSharedFlow<String>()
    val newMessageEvent = _newMessageEvent.asSharedFlow()

    fun loadClientData(clientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchData(clientId, isInitialLoad = true)
            _uiState.update { it.copy(isLoading = false) }

            while (true) {
                delay(15000)
                Log.d("ConversationListVM", "Verificando novas mensagens...")
                fetchData(clientId, isInitialLoad = false)
            }
        }
    }

    private suspend fun fetchData(clientId: String, isInitialLoad: Boolean) {
        try {
            if (isInitialLoad && _uiState.value.client == null) {
                val client = repository.getClientById(clientId)
                val allGroups = repository.getGroups()
                val clientGroups = allGroups.filter { it.id == client.groupId }
                _uiState.update { it.copy(client = client, groups = clientGroups) }
            }

            val client = _uiState.value.client ?: return
            val loggedEmail = getLoggedEmail()

            val allMessages = repository.getMyConversations()

            // Filtra apenas mensagens diretas do usuário logado — sem ID MongoDB
            val directMessages = allMessages.filter { msg ->
                msg.type == "CHAT" && msg.groupId == null && !msg.conversationId.orEmpty()
                    .startsWith("campaign_") && (msg.senderId == loggedEmail || msg.recipientId == loggedEmail)
            }

            // conversationId válido: contém @ E contém o email do usuário logado
            val real1on1ConversationId = directMessages.firstOrNull {
                    it.conversationId?.contains("@") == true && loggedEmail != null && it.conversationId.orEmpty()
                        .contains(loggedEmail)
                }?.conversationId ?: run {
                val operatorEmail =
                    directMessages.map { if (it.senderId == loggedEmail) it.recipientId else it.senderId }
                        .firstOrNull { it?.contains("@") == true }
                if (operatorEmail != null && loggedEmail != null) {
                    val sorted = listOf(loggedEmail, operatorEmail).sorted()
                    "${sorted[0]}_${sorted[1]}"
                } else null
            }

            Log.d("ConversationListVM", "conversationId 1:1 resolvido: $real1on1ConversationId")

            if (real1on1ConversationId != null) {
                _uiState.update { state ->
                    state.copy(
                        conversationIds = state.conversationIds + (client.id to real1on1ConversationId)
                    )
                }

                val conv1on1 = directMessages.filter {
                    it.conversationId == real1on1ConversationId
                }

                // ── Operador: extrai do conversationId, não do senderId das mensagens ──
                // Evita capturar email de outro cliente como se fosse operador
                if (loggedEmail != null && real1on1ConversationId.contains(loggedEmail)) {
                    val parts = real1on1ConversationId.split("_")
                    // conversationId = emailA_emailB — pega o que não é o loggedEmail
                    val firstAt = real1on1ConversationId.indexOf("@")
                    val splitPoint = real1on1ConversationId.indexOf("_", firstAt)
                    if (splitPoint > 0) {
                        val emailA = real1on1ConversationId.substring(0, splitPoint)
                        val emailB = real1on1ConversationId.substring(splitPoint + 1)
                        val resolvedOperator = if (emailA == loggedEmail) emailB else emailA
                        Log.d(
                            "ConversationListVM",
                            "operatorEmail resolvido do conversationId: $resolvedOperator"
                        )
                        _uiState.update { it.copy(operatorEmail = resolvedOperator) }
                    }
                }

                checkNewMessages(conv1on1, real1on1ConversationId, "Atendimento WTC", loggedEmail)
            } else {
                // Sem histórico 1:1 — tenta descobrir operador pelo grupo
                val groupMessages = _uiState.value.groups.flatMap { group ->
                    try {
                        repository.getConversation(group.id)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val operatorEmail =
                    groupMessages.firstOrNull { it.senderId != loggedEmail && it.senderId.contains("@") }?.senderId
                if (operatorEmail != null) {
                    _uiState.update { it.copy(operatorEmail = operatorEmail) }
                }
            }

            _uiState.value.groups.forEach { group ->
                val groupMessages = repository.getConversation(group.id)
                checkNewMessages(groupMessages, group.id, group.name, loggedEmail)
            }

        } catch (e: Exception) {
            Log.e("ConversationListVM", "Falha: ${e.message}")
            if (isInitialLoad) {
                _uiState.update { it.copy(isLoading = false, error = "Falha ao carregar dados.") }
            }
        }
    }

    fun getConversationId(clientId: String): String {
        return _uiState.value.conversationIds[clientId] ?: clientId
    }

    fun getOperatorEmail(): String? = _uiState.value.operatorEmail

    private fun getLoggedEmail(): String? {
        val token = RetrofitClient.authToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(
                payload.padEnd((payload.length + 3) / 4 * 4, '='), android.util.Base64.URL_SAFE
            )
            val json = String(decoded)
            val start = json.indexOf("\"sub\"") + 7
            val end = json.indexOf("\"", start)
            if (start > 6 && end > start) json.substring(start, end) else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun checkNewMessages(
        messages: List<Message>, chatId: String, chatName: String, loggedEmail: String?
    ) {
        val lastMessage = messages.lastOrNull() ?: return
        val lastKnownId = _uiState.value.lastMessageIds[chatId]

        _uiState.update { state ->
            state.copy(lastMessages = state.lastMessages + (chatId to lastMessage))
        }

        if (lastMessage.id != lastKnownId) {
            _uiState.update { state ->
                state.copy(lastMessageIds = state.lastMessageIds + (chatId to lastMessage.id))
            }
            if (lastKnownId != null && lastMessage.senderId != loggedEmail) {
                _newMessageEvent.emit("Nova mensagem em $chatName")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}