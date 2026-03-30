package br.com.fiap.wtcconnecta.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.viewmodel.ImageUploadViewModel
import coil.compose.AsyncImage

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBluePale = Color(0xFFEEF6FB)
private val TextMuted   = Color(0xFF6E90A0)

/**
 * Botão de anexar imagem para a barra de input do ChatScreen.
 *
 * Uso:
 *   ImagePickerButton(
 *       onImageReady = { url, key ->
 *           // url  = URL pré-assinada para exibir no chat
 *           // key  = objectKey para salvar no corpo da mensagem
 *       }
 *   )
 */
@Composable
fun ImagePickerButton(
    onImageReady: (url: String, key: String) -> Unit,
    uploadViewModel: ImageUploadViewModel = viewModel()
) {
    val context  = LocalContext.current
    val uiState  by uploadViewModel.uiState.collectAsState()
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher do seletor de imagens
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedUri = it
            uploadViewModel.uploadImage(it, context) { url, key ->
                onImageReady(url, key)
            }
        }
    }

    // Ícone de anexar na barra de input
    IconButton(
        onClick = { launcher.launch("image/*") },
        enabled = !uiState.isUploading
    ) {
        if (uiState.isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = WtcBlue
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Enviar imagem",
                tint = if (uiState.isUploading) TextMuted else WtcBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Snackbar de erro (se houver)
    uiState.error?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { uploadViewModel.clearError() },
            title = { Text("Erro no upload", fontWeight = FontWeight.Bold) },
            text  = { Text(errorMsg, color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { uploadViewModel.clearError() }) {
                    Text("OK", color = WtcBlue)
                }
            }
        )
    }
}

/**
 * Bolha de mensagem de imagem — exibe a imagem com AsyncImage (Coil).
 * Usado dentro do ChatScreen para renderizar mensagens do tipo imagem.
 *
 * [imageUrl]        — URL pré-assinada para carregamento
 * [isFromCurrentUser] — define alinhamento e cor da bolha
 */
@Composable
fun ImageMessageBubble(
    imageUrl: String,
    caption: String? = null,
    isFromCurrentUser: Boolean
) {
    val bubbleColor = if (isFromCurrentUser) Color(0xFF0B537B) else Color.White
    val alignment   = if (isFromCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .border(
                    width = if (isFromCurrentUser) 0.dp else 1.dp,
                    color = Color(0xFFD0E8F2),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Imagem enviada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .clip(
                            if (caption != null)
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            else
                                RoundedCornerShape(16.dp)
                        )
                )
                // Legenda opcional
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        fontSize = 13.sp,
                        color = if (isFromCurrentUser) Color.White.copy(alpha = 0.9f)
                        else Color(0xFF0D2B3E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Preview da imagem selecionada antes do envio.
 * Aparece acima do campo de texto quando o usuário escolhe uma imagem.
 *
 * [imageUrl]  — URL local (URI convertida para string) para preview
 * [onCancel]  — callback para cancelar a imagem selecionada
 */
@Composable
fun ImagePreviewBar(
    imageUrl: String,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WtcBluePale)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Imagem selecionada", fontSize = 13.sp,
                fontWeight = FontWeight.Medium, color = Color(0xFF0D2B3E))
            Text("Toque em enviar para confirmar", fontSize = 11.sp, color = TextMuted)
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancelar",
                tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}