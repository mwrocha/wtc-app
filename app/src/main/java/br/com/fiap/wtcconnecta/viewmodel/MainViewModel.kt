package br.com.fiap.wtcconnecta.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserSession(
    val id: String,
    val role: String,
    val name: String,
    val email: String   // adicionado para identificar o senderId nas mensagens
)

class MainViewModel : ViewModel() {
    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession = _userSession.asStateFlow()

    fun onLoginSuccess(result: LoginResult) {
        _userSession.update {
            UserSession(
                id    = result.userId,
                role  = result.role,
                name  = result.name,
                email = result.email   // precisa existir no LoginResult
            )
        }
    }

    fun logout() {
        _userSession.update { null }
    }
}