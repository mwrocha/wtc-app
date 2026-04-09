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

    private val _allClients       = MutableStateFlow<List<Client>>(emptyList())
    private val _searchQuery      = MutableStateFlow("")
    private val _selectedTags     = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedDivision = MutableStateFlow<Division?>(null)
    private val _selectedGroup    = MutableStateFlow<Group?>(null)

    val searchQuery      = _searchQuery.asStateFlow()
    val selectedTags     = _selectedTags.asStateFlow()
    val selectedDivision = _selectedDivision.asStateFlow()
    val selectedGroup    = _selectedGroup.asStateFlow()

    private val _uiState = MutableStateFlow(HomeOperatorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchAllData()

        viewModelScope.launch {
            combine(
                _allClients,
                _searchQuery,
                _selectedTags,
                _selectedDivision,
                _selectedGroup
            ) { clients, query, tags, division, group ->

                var filtered = clients

                // Filtro por nome
                if (query.isNotBlank())
                    filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }

                // Filtro por divisão
                if (division != null)
                    filtered = filtered.filter { it.divisionId == division.id }

                // Filtro por grupo (só aplica se divisão também estiver selecionada)
                if (group != null)
                    filtered = filtered.filter { it.groupId == group.id }

                // Filtro por tags (todas as tags selecionadas devem estar presentes)
                if (tags.isNotEmpty())
                    filtered = filtered.filter { client ->
                        tags.all { tag -> client.tags.orEmpty().contains(tag) }
                    }

                filtered
            }.collect { filteredClients ->
                _uiState.update { it.copy(clients = filteredClients) }
            }
        }
    }

    // ── Filtros ───────────────────────────────────────────────────────────────

    fun onTagSelected(tag: String) {
        _selectedTags.update { current ->
            if (current.contains(tag)) current - tag else current + tag
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDivisionSelected(division: Division?) {
        _selectedDivision.value = division
        _selectedGroup.value = null  // reset grupo ao trocar divisão
    }

    fun onGroupSelected(group: Group?) {
        _selectedGroup.value = group
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedTags.value = emptySet()
        _selectedDivision.value = null
        _selectedGroup.value = null
    }

    val hasActiveFilters: Boolean
        get() = _searchQuery.value.isNotBlank()
                || _selectedTags.value.isNotEmpty()
                || _selectedDivision.value != null
                || _selectedGroup.value != null

    // ── Dados ─────────────────────────────────────────────────────────────────

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
                        isLoading     = false,
                        availableTags = allTags,
                        groups        = groups,
                        divisions     = divisions
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

    // ── Mensagens de grupo ────────────────────────────────────────────────────

    fun sendGroupMessage(text: String, groupId: String, senderId: String) {
        viewModelScope.launch {
            try {
                val success = repository.sendGroupMessage(groupId = groupId, content = text)
                if (!success)
                    _uiState.update { it.copy(error = "Falha ao enviar mensagem para o grupo.") }
            } catch (e: Exception) {
                Log.e("HomeOperatorViewModel", "Falha ao enviar mensagem de grupo: ${e.message}")
                _uiState.update { it.copy(error = "Falha ao enviar mensagem.") }
            }
        }
    }

    fun sendDivisionMessage(text: String, divisionId: String, senderId: String) {
        viewModelScope.launch {
            val groupsInDivision = _uiState.value.groups.filter { it.divisionId == divisionId }
            if (groupsInDivision.isEmpty()) {
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

            if (failures > 0)
                _uiState.update { it.copy(error = "Mensagem enviada com $failures falha(s).") }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}