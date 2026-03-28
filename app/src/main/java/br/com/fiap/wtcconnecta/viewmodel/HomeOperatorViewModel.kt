package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeOperatorUiState(
    val isLoading: Boolean = true,
    val clients: List<Client> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val groups: List<Group> = emptyList(),
    val error: String? = null
)

class HomeOperatorViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _allClients = MutableStateFlow<List<Client>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())

    val searchQuery = _searchQuery.asStateFlow()
    val selectedTags = _selectedTags.asStateFlow()

    private val _uiState = MutableStateFlow(HomeOperatorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchAllData()

        viewModelScope.launch {
            combine(_allClients, _searchQuery, _selectedTags) { clients, query, tags ->
                val nameFiltered = if (query.isBlank()) clients
                else clients.filter { it.name.contains(query, ignoreCase = true) }

                if (tags.isEmpty()) nameFiltered
                else nameFiltered.filter { client ->
                    tags.all { selectedTag -> client.tags.orEmpty().contains(selectedTag) }
                }
            }.collect { filteredClients ->
                _uiState.update { it.copy(clients = filteredClients) }
            }
        }
    }

    fun onTagSelected(tag: String) {
        _selectedTags.update { current ->
            if (current.contains(tag)) current - tag else current + tag
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun fetchAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val clientsDeferred   = async { repository.getClients() }
                val groupsDeferred    = async { repository.getGroups() }
                val divisionsDeferred = async { repository.getDivisions() }

                val clients   = clientsDeferred.await()
                val groups    = groupsDeferred.await()
                val divisions = divisionsDeferred.await()

                _allClients.value = clients

                val allTags = clients.flatMap { it.tags.orEmpty() }.distinct().sorted()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        availableTags = allTags,
                        groups = groups,
                        divisions = divisions
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeOperatorViewModel", "Falha ao buscar dados: ${e.message}")
                _uiState.update {
                    it.copy(isLoading = false, error = "Falha ao carregar dados. Tente novamente.")
                }
            }
        }
    }

    fun retryFetch() { fetchAllData() }

    // Envia mensagem para um grupo via POST /api/messages/group
    fun sendGroupMessage(text: String, groupId: String, senderId: String) {
        viewModelScope.launch {
            try {
                val success = repository.sendGroupMessage(groupId = groupId, content = text)
                if (!success) {
                    _uiState.update { it.copy(error = "Falha ao enviar mensagem para o grupo.") }
                } else {
                    Log.d("HomeOperatorViewModel", "Mensagem enviada para grupo $groupId")
                }
            } catch (e: Exception) {
                Log.e("HomeOperatorViewModel", "Falha ao enviar mensagem de grupo: ${e.message}")
                _uiState.update { it.copy(error = "Falha ao enviar mensagem.") }
            }
        }
    }

    // Envia mensagem para todos os grupos de uma divisão via POST /api/messages/group
    fun sendDivisionMessage(text: String, divisionId: String, senderId: String) {
        viewModelScope.launch {
            val groupsInDivision = _uiState.value.groups.filter { it.divisionId == divisionId }
            if (groupsInDivision.isEmpty()) {
                Log.w("HomeOperatorViewModel", "Nenhum grupo encontrado para a divisão $divisionId")
                _uiState.update { it.copy(error = "Nenhum grupo encontrado para essa área.") }
                return@launch
            }

            var failures = 0
            groupsInDivision.forEach { group ->
                try {
                    val success = repository.sendGroupMessage(groupId = group.id, content = text)
                    if (!success) failures++
                } catch (e: Exception) {
                    failures++
                    Log.e("HomeOperatorViewModel", "Falha ao enviar para grupo ${group.id}: ${e.message}")
                }
            }

            if (failures > 0) {
                _uiState.update { it.copy(error = "Mensagem enviada com $failures falha(s).") }
            } else {
                Log.d("HomeOperatorViewModel", "Mensagem enviada para ${groupsInDivision.size} grupos.")
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}