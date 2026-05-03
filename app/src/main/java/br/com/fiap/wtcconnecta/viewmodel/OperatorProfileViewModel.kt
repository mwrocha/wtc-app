package br.com.fiap.wtcconnecta.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.remote.UserPublicDto
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class OperatorProfileUiState(
    val operator: UserPublicDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val passwordSuccess: Boolean = false,
    val passwordError: String? = null,
    val emailSuccess: Boolean = false,
    val emailError: String? = null,
    // ── Avatar ────────────────────────────────────────────────────────
    val avatarUrl: String? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null
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
                val email = getEmailFromToken() ?: return@launch
                val user = repository.getUserByEmail(email)
                _uiState.update { it.copy(operator = user, isLoading = false) }
                // Carrega avatar após perfil
                loadAvatar()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar perfil.") }
            }
        }
    }

    private fun loadAvatar() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getMyAvatar()
                if (response.isSuccessful) {
                    val url = response.body()?.get("url") as? String
                    if (!url.isNullOrBlank()) {
                        _uiState.update { it.copy(avatarUrl = url) }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun uploadAvatar(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, avatarError = null) }
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Não foi possível abrir o arquivo.")
                val bytes = inputStream.readBytes()
                inputStream.close()

                val extension = when (mimeType) {
                    "image/png" -> "png"
                    "image/gif" -> "gif"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "file", "avatar.$extension", requestBody
                )

                val response = RetrofitClient.instance.uploadAvatar(part)
                if (response.isSuccessful) {
                    val url = response.body()?.get("url") as? String
                    _uiState.update {
                        it.copy(isUploadingAvatar = false, avatarUrl = url)
                    }
                } else {
                    val msg = when (response.code()) {
                        400 -> "Tipo ou tamanho inválido (máx 5MB, JPEG/PNG/GIF/WEBP)."
                        else -> "Erro ao enviar avatar (${response.code()})."
                    }
                    _uiState.update { it.copy(isUploadingAvatar = false, avatarError = msg) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isUploadingAvatar = false, avatarError = "Erro: ${e.message}")
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { state ->
            state.copy(
                operator = state.operator?.copy(name = name), success = true
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
            _uiState.update {
                it.copy(
                    isLoading = true, passwordError = null, passwordSuccess = false
                )
            }
            try {
                val success = repository.changePassword(email, currentPassword, newPassword)
                if (success) {
                    _uiState.update { it.copy(isLoading = false, passwordSuccess = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false, passwordError = "Senha atual incorreta."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false, passwordError = "Erro ao alterar senha."
                    )
                }
            }
        }
    }

    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailError = null, emailSuccess = false) }
            try {
                repository.changeEmail(newEmail, password).onSuccess {
                    repository.logout()
                    _uiState.update { it.copy(isLoading = false, emailSuccess = true) }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false, emailError = e.message ?: "Erro ao alterar e-mail."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false, emailError = "Erro ao alterar e-mail."
                    )
                }
            }
        }
    }

    private fun getEmailFromToken(): String? {
        val token = RetrofitClient.authToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = android.util.Base64.decode(
                payload.padEnd((payload.length + 3) / 4 * 4, '='), android.util.Base64.URL_SAFE
            )
            val json = String(decoded)
            val start = json.indexOf("\"sub\"") + 7
            val end = json.indexOf("\"", start)
            if (start > 6 && end > start) json.substring(start, end) else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, avatarError = null) }
            try {
                val response = RetrofitClient.instance.deleteAvatar()
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isUploadingAvatar = false, avatarUrl = null) }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false, avatarError = "Erro ao excluir foto."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploadingAvatar = false, avatarError = "Erro ao excluir foto."
                    )
                }
            }
        }
    }

    fun clearAvatarError() {
        _uiState.update { it.copy(avatarError = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(success = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearPasswordState() {
        _uiState.update { it.copy(passwordSuccess = false, passwordError = null) }
    }

    fun clearEmailState() {
        _uiState.update { it.copy(emailSuccess = false, emailError = null) }
    }
}