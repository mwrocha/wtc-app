package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupManagementUiState(
    val divisions: List<Division> = emptyList(),
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class GroupManagementViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupManagementUiState())
    val uiState = _uiState.asStateFlow()

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val divisions = repository.getDivisions()
                val groups = repository.getGroups()
                _uiState.update {
                    it.copy(
                        divisions = divisions, groups = groups, isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar dados.") }
            }
        }
    }

    // ── Divisões ──────────────────────────────────────────────────────────────

    fun createDivision(name: String) {
        viewModelScope.launch {
            try {
                val created = repository.createDivision(name)
                _uiState.update { state ->
                    state.copy(
                        divisions = state.divisions + created,
                        successMessage = "Divisão \"${created.name}\" criada!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar divisão.") }
            }
        }
    }

    fun updateDivision(id: String, name: String) {
        viewModelScope.launch {
            try {
                val updated = repository.updateDivision(id, name)
                _uiState.update { state ->
                    state.copy(
                        divisions = state.divisions.map { if (it.id == id) updated else it },
                        successMessage = "Divisão atualizada!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao atualizar divisão.") }
            }
        }
    }

    fun deleteDivision(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteDivision(id)
                _uiState.update { state ->
                    state.copy(
                        divisions = state.divisions.filter { it.id != id },
                        groups = state.groups.filter { it.divisionId != id },
                        successMessage = "Divisão removida."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao remover divisão.") }
            }
        }
    }

    // ── Grupos ────────────────────────────────────────────────────────────────

    fun createGroup(name: String, divisionId: String) {
        viewModelScope.launch {
            try {
                val created = repository.createGroup(name, divisionId)
                _uiState.update { state ->
                    state.copy(
                        groups = state.groups + created,
                        successMessage = "Grupo \"${created.name}\" criado!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao criar grupo.") }
            }
        }
    }

    fun updateGroup(id: String, name: String, divisionId: String) {
        viewModelScope.launch {
            try {
                val updated = repository.updateGroup(id, name, divisionId)
                _uiState.update { state ->
                    state.copy(
                        groups = state.groups.map { if (it.id == id) updated else it },
                        successMessage = "Grupo atualizado!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao atualizar grupo.") }
            }
        }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteGroup(id)
                _uiState.update { state ->
                    state.copy(
                        groups = state.groups.filter { it.id != id },
                        successMessage = "Grupo removido."
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao remover grupo.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}