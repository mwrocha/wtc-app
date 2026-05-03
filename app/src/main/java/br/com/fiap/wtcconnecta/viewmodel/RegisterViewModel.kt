package br.com.fiap.wtcconnecta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false, val error: String? = null, val registerSuccess: Boolean = false
)

class RegisterViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        cpf: String? = null,
        phone: String? = null,
        company: String? = null
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Todos os campos obrigatórios devem ser preenchidos.") }
            return
        }

        if (role == "CLIENT") {
            if (cpf.isNullOrBlank()) {
                _uiState.update { it.copy(error = "CPF é obrigatório para clientes.") }
                return
            }
            if (phone.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Telefone é obrigatório para clientes.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.register(name, email, password, role, cpf, phone, company).onSuccess {
                _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
            }.onFailure { error ->
                val message = when {
                    error.message?.contains("409") == true -> "E-mail já cadastrado."
                    error.message?.contains("conexão") == true -> "Falha na conexão. Verifique a internet."
                    else -> error.message ?: "Erro ao cadastrar. Tente novamente."
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onRegisterHandled() {
        _uiState.update { it.copy(registerSuccess = false, error = null) }
    }
}