package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.ActionButton
import br.com.fiap.wtcconnecta.data.model.Campaign
import br.com.fiap.wtcconnecta.data.model.CampaignRequest
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CampaignUiState(
    val isLoading: Boolean = false,
    val campaigns: List<Campaign> = emptyList(),
    val groups: List<Group> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class CampaignViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampaignUiState())
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val campaigns = safeCall { repository.getCampaigns() } ?: emptyList()
                val groups    = safeCall { repository.getGroups() } ?: emptyList()
                val divisions = safeCall { repository.getDivisions() } ?: emptyList()
                val clients   = safeCall { repository.getClients() } ?: emptyList()
                val tags      = clients.flatMap { it.tags.orEmpty() }.distinct().sorted()

                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        campaigns     = campaigns,
                        groups        = groups,
                        divisions     = divisions,
                        availableTags = tags
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar dados.") }
            }
        }
    }

    fun createAndDispatch(
        title: String,
        body: String,
        url: String?,
        targetGroupId: String?,
        targetDivisionId: String?,
        targetTags: List<String>,
        actions: List<ActionButton>,
        actionUrls: Map<String, String>
    ) {
        if (title.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(error = "Título e mensagem são obrigatórios.") }
            return
        }
        if (targetGroupId == null && targetDivisionId == null && targetTags.isEmpty()) {
            _uiState.update { it.copy(error = "Selecione pelo menos um grupo, divisão ou tag.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = CampaignRequest(
                    title            = title,
                    body             = body,
                    url              = url.takeIf { !it.isNullOrBlank() },
                    actions          = actions.takeIf { it.isNotEmpty() },
                    actionUrls       = actionUrls.takeIf { it.isNotEmpty() },
                    targetGroupId    = targetGroupId,
                    targetDivisionId = targetDivisionId,
                    targetTags       = targetTags.takeIf { it.isNotEmpty() }
                )

                val created = safeCall { repository.createCampaign(request) }
                if (created == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Falha ao criar campanha.") }
                    return@launch
                }

                val dispatched = safeCall { repository.dispatchCampaign(created.id) }
                if (dispatched != null) {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Campanha enviada com sucesso!") }
                    loadData()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Campanha criada mas falha ao disparar.") }
                }
            } catch (e: Exception) {
                Log.e("CampaignVM", "Erro: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Erro ao criar campanha.") }
            }
        }
    }

    fun dispatchExisting(campaignId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                safeCall { repository.dispatchCampaign(campaignId) }
                _uiState.update { it.copy(isLoading = false, successMessage = "Campanha disparada!") }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao disparar campanha.") }
            }
        }
    }

    fun updateCampaign(id: String, request: CampaignRequest) {
        viewModelScope.launch {
            try {
                repository.updateCampaign(id, request)
                _uiState.update { it.copy(successMessage = "Campanha atualizada!") }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao atualizar campanha.") }
            }
        }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }

    private suspend fun <T> safeCall(block: suspend () -> T): T? = try {
        block()
    } catch (e: Exception) {
        Log.e("CampaignVM", "safeCall: ${e.message}")
        null
    }
}