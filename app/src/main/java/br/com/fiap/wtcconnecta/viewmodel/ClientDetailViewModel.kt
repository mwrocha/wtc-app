package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.Note
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientDetailUiState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val clientAvatarUrl: String? = null,   // ← presigned URL resolvida
    val notes: List<Note> = emptyList(),
    val messages: List<Message> = emptyList(),
    val campaigns: List<Message> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val groups: List<Group> = emptyList(),
    val error: String? = null,
    val senderNames: Map<String, String> = emptyMap(),
    val attendanceClosed: Boolean = false
)

class ClientDetailViewModel(private val repository: AuthRepository = AuthRepository()) :
    ViewModel() {

    private val slashCommands = mapOf(
        "/agradecer" to "O WTC Connecta agradece seu contato! Estamos à disposição.",
        "/promo" to "Temos uma promoção especial para você! 10% de desconto em todos os serviços esta semana.",
        "/boleto" to "Claro! Estou gerando a segunda via do seu boleto e enviarei em instantes."
    )

    private val _uiState = MutableStateFlow(ClientDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var currentClientId: String? = null

    private fun buildConversationId(id1: String, id2: String): String =
        if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"

    private fun getOperatorEmail(): String? {
        val token = RetrofitClient.authToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(
                payload.padEnd((payload.length + 3) / 4 * 4, '='), android.util.Base64.URL_SAFE
            )
            val json = String(decoded)
            val subRegex = Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"")
            subRegex.find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e("ClientDetailVM", "Erro ao decodificar token: ${e.message}")
            null
        }
    }

    fun loadAllDetails(clientId: String) {
        currentClientId = clientId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val operatorEmail = getOperatorEmail()
                val client = safeApiCall { repository.getClientById(clientId) }
                val clientEmail = client?.email ?: clientId
                val conversationId =
                    if (operatorEmail != null) buildConversationId(clientEmail, operatorEmail)
                    else clientId

                val conversationDeferred =
                    async { safeApiCall { repository.getConversation(conversationId) } }
                val notesDeferred = async { safeApiCall { repository.getNotesForClient(clientId) } }
                val divisionsDeferred = async { safeApiCall { repository.getDivisions() } }
                val groupsDeferred = async { safeApiCall { repository.getGroups() } }
                val campaignsDeferred =
                    async { safeApiCall { repository.getCampaignsForClient(clientId) } }

                val conversation = conversationDeferred.await() ?: emptyList()
                val notes = notesDeferred.await() ?: emptyList()
                val divisions = divisionsDeferred.await() ?: emptyList()
                val groups = groupsDeferred.await() ?: emptyList()
                val campaigns = campaignsDeferred.await() ?: emptyList()

                Log.d("MSG_DEBUG", "Total mensagens: ${conversation.size}")
                conversation.take(3).forEach { msg ->
                    Log.d(
                        "MSG_DEBUG",
                        "id=${msg.id} body=${msg.body} contentRaw=${msg.contentRaw} display=${msg.displayContent}"
                    )
                }

                if (client == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, error = "Cliente não encontrado."
                        )
                    }
                    return@launch
                }

                val newNames = resolveSenderNames(conversation, client)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        client = client,
                        notes = notes,
                        messages = conversation,
                        campaigns = campaigns,
                        divisions = divisions,
                        groups = groups,
                        senderNames = it.senderNames + newNames
                    )
                }

                // ── Busca presigned URL do avatar do cliente ──────────────────
                // Feito após atualizar o estado principal para não atrasar o carregamento
                loadClientAvatar(client.avatarKey)

            } catch (e: Exception) {
                Log.e("ClientDetailVM", "Erro ao buscar detalhes: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false, error = "Erro ao carregar detalhes do cliente."
                    )
                }
            }
        }
    }

    private fun loadClientAvatar(avatarKey: String?) {
        if (avatarKey.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getPresignedUrl(avatarKey)
                if (response.isSuccessful) {
                    val url = response.body()?.url  // ← .url em vez de .get("url")
                    if (!url.isNullOrBlank()) {
                        _uiState.update { it.copy(clientAvatarUrl = url) }
                    }
                }
            } catch (e: Exception) {
                Log.w("ClientDetailVM", "Falha ao carregar avatar do cliente: ${e.message}")
            }
        }
    }

    fun startPolling(clientId: String) {
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                refreshMessages(clientId)
            }
        }
    }

    private suspend fun refreshMessages(clientId: String) {
        try {
            val operatorEmail = getOperatorEmail()
            val client = _uiState.value.client ?: return
            val conversationId =
                if (operatorEmail != null) buildConversationId(client.email, operatorEmail)
                else clientId

            val updated = safeApiCall { repository.getConversation(conversationId) } ?: return
            val knownIds = _uiState.value.senderNames.keys
            val newSenders = updated.map { it.senderId }.filter { it !in knownIds }.distinct()
            val newNames = mutableMapOf<String, String>()
            for (senderId in newSenders) {
                val user = safeApiCall { repository.getUserByEmail(senderId) }
                if (user != null) newNames[senderId] = user.name
            }
            newNames[client.email] = client.name

            _uiState.update { it.copy(messages = updated, senderNames = it.senderNames + newNames) }
        } catch (e: Exception) {
            Log.e("ClientDetailVM", "Erro no polling: ${e.message}")
        }
    }

    private suspend fun resolveSenderNames(
        conversation: List<Message>, client: Client
    ): Map<String, String> {
        val names = mutableMapOf<String, String>()
        val senders = conversation.map { it.senderId }.filter { it.isNotBlank() }.distinct()
        for (senderId in senders) {
            try {
                val user = repository.getUserByEmail(senderId)
                if (user != null) names[senderId] = user.name
            } catch (e: Exception) {
                Log.w("ClientDetailVM", "Não encontrou nome para $senderId")
            }
        }
        names[client.email] = client.name
        return names
    }

    fun updateClientProfile(divisionId: String, groupId: String, tags: List<String> = emptyList()) {
        val client = _uiState.value.client ?: return
        viewModelScope.launch {
            try {
                val updated = client.copy(
                    divisionId = divisionId,
                    groupId = groupId,
                    tags = tags,
                    noteIds = client.noteIds.orEmpty()
                )
                repository.updateClient(client.id, updated)
                _uiState.update { it.copy(client = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao atualizar perfil do cliente.") }
            }
        }
    }

    fun addNote(text: String, clientId: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val success = repository.createNote(clientId = clientId, text = text)
                if (success) loadAllDetails(clientId)
                else _uiState.update { it.copy(error = "Falha ao adicionar anotação.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao adicionar anotação.") }
            }
        }
    }

    fun updateNote(note: Note, newText: String, clientId: String) {
        viewModelScope.launch {
            try {
                val response =
                    RetrofitClient.instance.updateNote(note.id, mapOf("content" to newText))
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(notes = state.notes.map {
                            if (it.id == note.id) it.copy(text = newText) else it
                        })
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(notes = state.notes.map {
                        if (it.id == note.id) it.copy(text = newText) else it
                    })
                }
            }
        }
    }

    fun deleteNote(noteId: String, clientId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteNote(noteId)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(notes = state.notes.filter { it.id != noteId })
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(notes = state.notes.filter { it.id != noteId })
                }
            }
        }
    }

    fun sendMessage(text: String, clientId: String, senderId: String) {
        if (text.isBlank()) return
        val content = slashCommands[text.trim()] ?: text
        viewModelScope.launch {
            try {
                val success = repository.sendMessage(receiverId = clientId, content = content)
                if (success) {
                    val operatorEmail = getOperatorEmail()
                    val clientEmail = _uiState.value.client?.email ?: clientId
                    val conversationId =
                        if (operatorEmail != null) buildConversationId(clientEmail, operatorEmail)
                        else clientId
                    val updated =
                        safeApiCall { repository.getConversation(conversationId) } ?: emptyList()
                    _uiState.update { it.copy(messages = updated) }
                } else {
                    _uiState.update { it.copy(error = "Falha ao enviar mensagem.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao enviar mensagem.") }
            }
        }
    }

    fun closeAttendance(clientId: String) {
        viewModelScope.launch {
            try {
                val operatorEmail = getOperatorEmail() ?: return@launch
                val clientEmail = _uiState.value.client?.email ?: clientId
                val conversationId = buildConversationId(clientEmail, operatorEmail)

                RetrofitClient.instance.closeConversation(conversationId)

                val farewell =
                    "✅ Atendimento encerrado. Obrigado pelo contato! " + "Caso precise de mais ajuda, estamos à disposição."
                repository.sendMessage(receiverId = clientEmail, content = farewell)

                val updated =
                    safeApiCall { repository.getConversation(conversationId) } ?: emptyList()
                _uiState.update { it.copy(messages = updated, attendanceClosed = true) }

                Log.d("ClientDetailVM", "Atendimento encerrado: $conversationId")
            } catch (e: Exception) {
                Log.e("ClientDetailVM", "Erro ao encerrar atendimento: ${e.message}")
                _uiState.update { it.copy(error = "Erro ao encerrar atendimento.") }
            }
        }
    }

    fun clearAttendanceClosed() {
        _uiState.update { it.copy(attendanceClosed = false) }
    }

    fun getSenderName(senderId: String): String = _uiState.value.senderNames[senderId] ?: senderId

    fun isFromOperator(senderId: String): Boolean = senderId == getOperatorEmail()

    fun getCommandSuggestions(query: String): List<String> {
        if (!query.startsWith("/")) return emptyList()
        return slashCommands.keys.filter { it.startsWith(query, ignoreCase = true) }
    }

    fun deleteMessage(messageId: String, clientId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.deleteMessage(messageId)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != messageId })
                    }
                }
            } catch (e: Exception) {
                Log.e("ClientDetailVM", "Erro ao excluir mensagem: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): T? {
        return try {
            apiCall()
        } catch (e: Exception) {
            Log.e("ClientDetailVM", "safeApiCall erro: ${e.message}")
            null
        }
    }
}