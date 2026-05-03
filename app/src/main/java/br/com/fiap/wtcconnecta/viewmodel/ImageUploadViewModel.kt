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
    val isPdf: Boolean = false,
    val fileName: String? = null,
    val error: String? = null
)

class ImageUploadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUploadUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Faz upload de imagem ou PDF.
     * Detecta o tipo automaticamente pelo mimeType da URI.
     */
    fun uploadFile(uri: Uri, context: Context, onSuccess: (url: String, key: String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }

            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Não foi possível abrir o arquivo.")
                val bytes = inputStream.readBytes()
                inputStream.close()

                val isPdf = mimeType == "application/pdf"

                val extension = when (mimeType) {
                    "application/pdf" -> "pdf"
                    "image/png" -> "png"
                    "image/gif" -> "gif"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }

                // Tenta obter o nome do arquivo original
                val fileName = try {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        val nameIndex =
                            it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        it.moveToFirst()
                        if (nameIndex >= 0) it.getString(nameIndex) else "arquivo.$extension"
                    } ?: "arquivo.$extension"
                } catch (_: Exception) {
                    "arquivo.$extension"
                }

                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                val response = RetrofitClient.instance.uploadImage(part)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadedUrl = body.url,
                            uploadedKey = body.objectKey,
                            isPdf = isPdf,
                            fileName = fileName
                        )
                    }
                    onSuccess(body.url, body.objectKey)
                } else {
                    val msg = when (response.code()) {
                        400 -> "Tipo ou tamanho de arquivo inválido."
                        413 -> "Arquivo muito grande (máx 10 MB)."
                        else -> "Erro ao fazer upload (${response.code()})."
                    }
                    _uiState.update { it.copy(isUploading = false, error = msg) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = "Erro: ${e.message}") }
            }
        }
    }

    // Mantido para compatibilidade com código existente
    fun uploadImage(uri: Uri, context: Context, onSuccess: (url: String, key: String) -> Unit) {
        uploadFile(uri, context, onSuccess)
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun reset() = _uiState.update { ImageUploadUiState() }
}