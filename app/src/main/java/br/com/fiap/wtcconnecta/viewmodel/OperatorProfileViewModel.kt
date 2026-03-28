package br.com.fiap.wtcconnecta.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.remote.UserPublicDto
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperatorProfileUiState(
    val operator: UserPublicDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val passwordSuccess: Boolean = false,
    val passwordError: String? = null,
    val emailSuccess: Boolean = false,
    val emailError: String? = null
)

class OperatorProfileViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun loadProfile(operatorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Busca email do token e carrega o perfil
                val email = getEmailFromToken() ?: return@launch
                val user  = repository.getUserByEmail(email)
                _uiState.update { it.copy(operator = user, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar perfil.") }
            }
        }
    }

    fun updateName(name: String) {
        // Para o operador, só atualiza o nome localmente por ora
        // (pode ser expandido para um endpoint PUT /api/users/{id})
        _uiState.update { state ->
            state.copy(
                operator = state.operator?.copy(name = name),
                success  = true
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val email = _uiState.value.operator?.email ?: return
        if (newPassword.length < 6) {
            _uiState.update { it.copy(passwordError = "A nova senha deve ter pelo menos 6 caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, passwordError = null, passwordSuccess = false) }
            try {
                val success = repository.changePassword(email, currentPassword, newPassword)
                if (success) {
                    _uiState.update { it.copy(isLoading = false, passwordSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, passwordError = "Senha atual incorreta.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, passwordError = "Erro ao alterar senha.") }
            }
        }
    }

    private fun getEmailFromToken(): String? {
        val token = RetrofitClient.authToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(
                payload.padEnd((payload.length + 3) / 4 * 4, '='),
                android.util.Base64.URL_SAFE
            )
            val json  = String(decoded)
            val start = json.indexOf("\"sub\"") + 7
            val end   = json.indexOf("\"", start)
            if (start > 6 && end > start) json.substring(start, end) else null
        } catch (e: Exception) { null }
    }

    fun clearSuccess()       { _uiState.update { it.copy(success = false) } }
    fun clearError()         { _uiState.update { it.copy(error = null) } }
    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailError = null, emailSuccess = false) }
            try {
                repository.changeEmail(newEmail, password)
                    .onSuccess {
                        // Logout automático — JWT inválido após troca de email
                        repository.logout()
                        _uiState.update { it.copy(isLoading = false, emailSuccess = true) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, emailError = e.message ?: "Erro ao alterar e-mail.") }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, emailError = "Erro ao alterar e-mail.") }
            }
        }
    }

    fun clearPasswordState() { _uiState.update { it.copy(passwordSuccess = false, passwordError = null) } }
    fun clearEmailState()    { _uiState.update { it.copy(emailSuccess = false, emailError = null) } }
}