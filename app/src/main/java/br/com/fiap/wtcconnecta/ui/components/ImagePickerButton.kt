package br.com.fiap.wtcconnecta.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Botão de anexar arquivo (imagem ou PDF) para a barra de input do ChatScreen.
 */
@Composable
fun ImagePickerButton(
    onImageReady: (url: String, key: String) -> Unit,
    uploadViewModel: ImageUploadViewModel = viewModel()
) {
    val context   = LocalContext.current
    val uiState   by uploadViewModel.uiState.collectAsState()

    // Aceita imagens e PDFs
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadViewModel.uploadFile(it, context) { url, key ->
                onImageReady(url, key)
            }
        }
    }

    IconButton(
        onClick  = { launcher.launch("*/*") },
        enabled  = !uiState.isUploading
    ) {
        if (uiState.isUploading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color       = WtcBlue
            )
        } else {
            Icon(
                imageVector        = Icons.Default.AttachFile,
                contentDescription = "Anexar arquivo",
                tint               = WtcBlue,
                modifier           = Modifier.size(24.dp)
            )
        }
    }

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
 * Bolha de mensagem de imagem.
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
                    model              = imageUrl,
                    contentDescription = "Imagem enviada",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp)
                        .clip(
                            if (caption != null)
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            else
                                RoundedCornerShape(16.dp)
                        )
                )
                if (!caption.isNullOrBlank()) {
                    Text(
                        text     = caption,
                        fontSize = 13.sp,
                        color    = if (isFromCurrentUser) Color.White.copy(alpha = 0.9f)
                        else Color(0xFF0D2B3E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bolha de mensagem de PDF.
 */
@Composable
fun PdfMessageBubble(
    fileName: String,
    onOpen: () -> Unit,
    isFromCurrentUser: Boolean
) {
    val bubbleColor = if (isFromCurrentUser) Color(0xFF0B537B) else Color.White
    val textColor   = if (isFromCurrentUser) Color.White else Color(0xFF0D2B3E)
    val alignment   = if (isFromCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            onClick   = onOpen,
            shape     = RoundedCornerShape(16.dp),
            color     = bubbleColor,
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier  = Modifier.widthIn(max = 260.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFromCurrentUser) Color.White.copy(alpha = 0.15f)
                            else Color(0xFFFFEBEE)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null,
                        tint     = if (isFromCurrentUser) Color.White else Color(0xFFC62828),
                        modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(fileName, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = textColor,
                        maxLines = 2)
                    Text("Toque para abrir", fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.65f))
                }
            }
        }
    }
}

/**
 * Preview do arquivo selecionado antes do envio.
 */
@Composable
fun ImagePreviewBar(
    imageUrl: String,
    fileName: String? = null,
    isPdf: Boolean = false,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WtcBluePale)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPdf) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null,
                    tint = Color(0xFFC62828), modifier = Modifier.size(28.dp))
            }
        } else {
            AsyncImage(
                model              = imageUrl,
                contentDescription = "Preview",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isPdf) (fileName ?: "Arquivo PDF") else "Imagem selecionada",
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D2B3E),
                maxLines = 1
            )
            Text("Toque em enviar para confirmar", fontSize = 11.sp, color = TextMuted)
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancelar",
                tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}