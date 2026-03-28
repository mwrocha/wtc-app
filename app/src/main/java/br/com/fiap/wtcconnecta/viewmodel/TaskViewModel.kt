package br.com.fiap.wtcconnecta.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Task
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class TaskViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState = _uiState.asStateFlow()

    // Tarefas agrupadas por status para o Kanban
    val pendingTasks   get() = _uiState.value.tasks.filter { it.status == "PENDING" }
    val inProgressTasks get() = _uiState.value.tasks.filter { it.status == "IN_PROGRESS" }
    val doneTasks      get() = _uiState.value.tasks.filter { it.status == "DONE" }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tasks = repository.getTasks()
                _uiState.update { it.copy(isLoading = false, tasks = tasks) }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Erro ao carregar tarefas: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar tarefas.") }
            }
        }
    }

    fun createTask(request: TaskRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.createTask(request)
                _uiState.update { it.copy(successMessage = "Tarefa criada com sucesso!") }
                loadTasks()
                onSuccess()
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Erro ao criar tarefa: ${e.message}")
                _uiState.update { it.copy(error = "Erro ao criar tarefa.") }
            }
        }
    }

    fun updateStatus(taskId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                repository.updateTaskStatus(taskId, newStatus)
                // Atualiza localmente sem recarregar tudo
                _uiState.update { state ->
                    state.copy(
                        tasks = state.tasks.map { task ->
                            if (task.id == taskId) task.copy(status = newStatus) else task
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Erro ao atualizar status: ${e.message}")
                _uiState.update { it.copy(error = "Erro ao atualizar tarefa.") }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _uiState.update { state ->
                    state.copy(tasks = state.tasks.filter { it.id != taskId })
                }
                _uiState.update { it.copy(successMessage = "Tarefa removida.") }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Erro ao deletar tarefa: ${e.message}")
                _uiState.update { it.copy(error = "Erro ao remover tarefa.") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}