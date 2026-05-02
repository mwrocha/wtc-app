package br.com.fiap.wtcconnecta.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class ProfileUiState(
    val client: Client? = null,
    val groups: List<Group> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val passwordSuccess: Boolean = false,
    val passwordError: String? = null,
    val emailSuccess: Boolean = false,
    val emailError: String? = null,
    val avatarUrl: String? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null,
    // Telefone
    val phoneSuccess: Boolean = false,
    val phoneError: String? = null,
    // CPF
    val cpfSuccess: Boolean = false,
    val cpfError: String? = null,
    // Empresa (solicitação)
    val companyRequestSuccess: Boolean = false,
    val companyRequestError: String? = null
)

class ProfileViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack = _navigateBack

    fun loadProfile(clientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val client    = repository.getClientById(clientId)
                val groups    = repository.getGroups()
                val divisions = repository.getDivisions()
                _uiState.update {
                    it.copy(client = client, groups = groups,
                        divisions = divisions, isLoading = false)
                }
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
                    if (!url.isNullOrBlank()) _uiState.update { it.copy(avatarUrl = url) }
                }
            } catch (_: Exception) {}
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
                    "image/png"  -> "png"
                    "image/gif"  -> "gif"
                    "image/webp" -> "webp"
                    else         -> "jpg"
                }

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "avatar.$extension", requestBody)

                val response = RetrofitClient.instance.uploadAvatar(part)
                if (response.isSuccessful) {
                    val url = response.body()?.get("url") as? String
                    _uiState.update { it.copy(isUploadingAvatar = false, avatarUrl = url) }
                } else {
                    val msg = when (response.code()) {
                        400  -> "Tipo ou tamanho inválido (máx 5MB, JPEG/PNG/GIF/WEBP)."
                        else -> "Erro ao enviar avatar (${response.code()})."
                    }
                    _uiState.update { it.copy(isUploadingAvatar = false, avatarError = msg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadingAvatar = false, avatarError = "Erro: ${e.message}") }
            }
        }
    }

    fun updateClientProfile(
        name: String,
        selectedGroupId: String,
        phone: String? = null,
        cpf: String? = null,
        company: String? = null
    ) {
        val client = _uiState.value.client ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                // Atualiza nome via endpoint dedicado
                val nameResponse = RetrofitClient.instance.updateMyName(mapOf("name" to name))
                val nameOk = nameResponse.isSuccessful

                // Atualiza demais campos via PUT /api/clients/{id}
                val fieldsOk = try {
                    repository.updateClientProfile(
                        clientId  = client.id,
                        name      = name,
                        groupId   = selectedGroupId,
                        phone     = phone,
                        cpf       = cpf,
                        company   = company
                    )
                } catch (e: Exception) {
                    android.util.Log.w("ProfileViewModel", "updateClientProfile fields: ${e.message}")
                    true // ignora erro de parse do body
                }

                if (nameOk || fieldsOk) {
                    _uiState.update {
                        it.copy(
                            client    = client.copy(
                                name    = name,
                                groupId = selectedGroupId,
                                phone   = phone ?: client.phone,
                                tags  = client.tags ?: emptyList(),
                                cpf     = cpf ?: client.cpf,
                                company = company ?: client.company
                            ),
                            isLoading = false,
                            success   = true
                        )
                    }
                    _navigateBack.tryEmit(Unit)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Falha ao atualizar perfil.") }
                }
            } catch (e: Exception) {
                android.util.Log.w("ProfileViewModel", "updateClientProfile exception: ${e.message}")
                _uiState.update {
                    it.copy(
                        client    = client.copy(
                            name    = name,
                            groupId = selectedGroupId,
                            phone   = phone ?: client.phone,
                            tags  = client.tags ?: emptyList(),
                            cpf     = cpf ?: client.cpf,
                            company = company ?: client.company
                        ),
                        isLoading = false,
                        success   = true
                    )
                }
                _navigateBack.tryEmit(Unit)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val client = _uiState.value.client ?: return
        if (newPassword.length < 6) {
            _uiState.update { it.copy(passwordError = "A nova senha deve ter pelo menos 6 caracteres.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, passwordError = null, passwordSuccess = false) }
            try {
                val success = repository.changePassword(client.email, currentPassword, newPassword)
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

    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, emailError = null, emailSuccess = false) }
            try {
                repository.changeEmail(newEmail, password)
                    .onSuccess {
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

    fun requestGroupChange(newGroupId: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.requestGroupChange(newGroupId, reason)
                _uiState.update { it.copy(success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao enviar solicitação.") }
            }
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
                    _uiState.update { it.copy(isUploadingAvatar = false, avatarError = "Erro ao excluir foto.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadingAvatar = false, avatarError = "Erro ao excluir foto.") }
            }
        }
    }

    fun changePhone(newPhone: String) {
        val client = _uiState.value.client ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, phoneError = null, phoneSuccess = false) }
            repository.changePhone(client.id, newPhone)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            phoneSuccess = true,
                            client       = it.client?.copy(
                                phone = newPhone,
                                tags  = it.client.tags ?: emptyList()  // ← fix
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, phoneError = e.message ?: "Erro ao alterar telefone.") }
                }
        }
    }

    fun changeCpf(newCpf: String) {
        val client = _uiState.value.client ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, cpfError = null, cpfSuccess = false) }
            repository.changeCpf(client.id, newCpf)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading  = false,
                            cpfSuccess = true,
                            client     = it.client?.copy(
                                cpf  = newCpf,
                                tags = it.client.tags ?: emptyList()  // ← fix
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, cpfError = e.message ?: "Erro ao alterar CPF.") }
                }
        }
    }

    fun changeCompany(newCompany: String) {
        val client = _uiState.value.client ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, companyRequestError = null, companyRequestSuccess = false) }
            repository.changeCompany(client.id, newCompany)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading             = false,
                            companyRequestSuccess = true,
                            client                = it.client?.copy(
                                company = newCompany,
                                tags    = it.client.tags ?: emptyList()  // ← fix
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, companyRequestError = e.message ?: "Erro ao alterar empresa.") }
                }
        }
    }


    fun clearPhoneState()          { _uiState.update { it.copy(phoneSuccess = false, phoneError = null) } }
    fun clearCpfState()            { _uiState.update { it.copy(cpfSuccess = false, cpfError = null) } }
    fun clearCompanyRequestState() { _uiState.update { it.copy(companyRequestSuccess = false, companyRequestError = null) } }

    fun clearAvatarError()   { _uiState.update { it.copy(avatarError = null) } }
    fun clearEmailState()    { _uiState.update { it.copy(emailSuccess = false, emailError = null) } }
    fun clearSuccess()       { _uiState.update { it.copy(success = false) } }
    fun clearError()         { _uiState.update { it.copy(error = null) } }
    fun clearPasswordState() { _uiState.update { it.copy(passwordSuccess = false, passwordError = null) } }
}