package br.com.fiap.wtcconnecta.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class ImageUploadUiState(
    val isUploading: Boolean = false,
    val uploadedUrl: String? = null,
    val uploadedKey: String? = null,
    val error: String? = null
)

class ImageUploadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUploadUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Faz upload de uma imagem selecionada pelo usuário.
     * [uri]     — URI retornada pelo picker de imagens do Android
     * [context] — necessário para abrir o InputStream da URI
     * [onSuccess] — callback com a URL pré-assinada após upload bem-sucedido
     */
    fun uploadImage(uri: Uri, context: Context, onSuccess: (url: String, key: String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }

            try {
                // Lê o arquivo a partir da URI
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Não foi possível abrir o arquivo.")
                val bytes = inputStream.readBytes()
                inputStream.close()

                // Monta o multipart
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val extension = when (mimeType) {
                    "image/png"  -> "png"
                    "image/gif"  -> "gif"
                    "image/webp" -> "webp"
                    else         -> "jpg"
                }
                val part = MultipartBody.Part.createFormData(
                    "file", "upload.$extension", requestBody
                )

                // Chama o endpoint
                val response = RetrofitClient.instance.uploadImage(part)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadedUrl = body.url,
                            uploadedKey = body.objectKey
                        )
                    }
                    onSuccess(body.url, body.objectKey)
                } else {
                    val msg = when (response.code()) {
                        400  -> "Tipo ou tamanho de arquivo inválido."
                        413  -> "Arquivo muito grande (máx 5 MB)."
                        else -> "Erro ao fazer upload (${response.code()})."
                    }
                    _uiState.update { it.copy(isUploading = false, error = msg) }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isUploading = false, error = "Erro: ${e.message}")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun reset()      = _uiState.update { ImageUploadUiState() }
}