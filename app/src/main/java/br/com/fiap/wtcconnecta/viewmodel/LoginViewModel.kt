package br.com.fiap.wtcconnecta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginResult(
    val userId: String,
    val role: String,
    val name: String,
    val email: String   // adicionado
)

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginResult: LoginResult? = null
)

class LoginViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "E-mail e senha são obrigatórios.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.login(email, password)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginResult = LoginResult(
                                userId = response.id,
                                role = when (response.role) {
                                    "OPERATOR" -> "operador"
                                    "CLIENT"   -> "cliente"
                                    else       -> response.role.lowercase()
                                },
                                name  = response.name,
                                email = response.email   // adicionado
                            )
                        )
                    }
                }
                .onFailure { error ->
                    val message = when {
                        error.message?.contains("401") == true -> "E-mail ou senha inválidos."
                        error.message?.contains("conexão") == true -> "Erro de conexão."
                        else -> error.message ?: "Erro desconhecido."
                    }
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onLoginHandled() {
        _uiState.update { it.copy(loginResult = null) }
    }
}