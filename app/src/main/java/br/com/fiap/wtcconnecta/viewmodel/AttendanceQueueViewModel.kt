package br.com.fiap.wtcconnecta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.remote.PendingConversation
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceQueueUiState(
    val pendingConversations: List<PendingConversation> = emptyList(),
    val pendingCount: Long = 0L,
    val isLoading: Boolean = false,
    val isAssuming: Boolean = false,
    val assumeSuccess: String? = null,      // conversationId assumido
    val assumedClientId: String? = null,    // ← clientId para navegação direta
    val assumedClientName: String? = null,  // ← clientName para exibição
    val error: String? = null
)

class AttendanceQueueViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceQueueUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val conversations = RetrofitClient.instance.getPendingConversations()
                val countResp     = RetrofitClient.instance.getPendingCount()
                _uiState.update {
                    it.copy(
                        isLoading            = false,
                        pendingConversations = conversations,
                        pendingCount         = countResp.count
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar fila.") }
            }
        }
    }

    // Polling a cada 15 segundos para atualizar o badge
    fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                try {
                    val countResp = RetrofitClient.instance.getPendingCount()
                    _uiState.update { it.copy(pendingCount = countResp.count) }
                } catch (_: Exception) {}
            }
        }
    }

    fun assumeConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAssuming = true, error = null) }
            try {
                // ← Busca os dados do cliente ANTES de remover da lista
                val conv = _uiState.value.pendingConversations
                    .find { it.conversationId == conversationId }
                val clientId   = conv?.clientId   ?: ""
                val clientName = conv?.clientName ?: "Cliente"

                val response = RetrofitClient.instance.assumeConversation(conversationId)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(
                            isAssuming           = false,
                            assumeSuccess        = conversationId,
                            assumedClientId      = clientId,    // ← salva antes de remover
                            assumedClientName    = clientName,  // ← salva antes de remover
                            pendingConversations = state.pendingConversations
                                .filter { it.conversationId != conversationId },
                            pendingCount         = (state.pendingCount - 1).coerceAtLeast(0)
                        )
                    }
                } else {
                    val msg = if (response.code() == 400)
                        "Esta conversa já está sendo atendida."
                    else "Erro ao assumir atendimento."
                    _uiState.update { it.copy(isAssuming = false, error = msg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isAssuming = false, error = "Erro: ${e.message}") }
            }
        }
    }

    fun clearAssumeSuccess() = _uiState.update {
        it.copy(assumeSuccess = null, assumedClientId = null, assumedClientName = null)
    }
    fun clearError() = _uiState.update { it.copy(error = null) }
}