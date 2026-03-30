package br.com.fiap.wtcconnecta.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import coil.compose.AsyncImage

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

/**
 * Componente reutilizável de avatar com:
 * - Borda azul suave
 * - Menu ao clicar: Editar foto / Excluir foto
 * - Spinner durante upload
 * - Fallback para inicial do nome
 *
 * @param avatarUrl       URL pré-assinada da foto atual (null = sem foto)
 * @param displayName     Nome do usuário (usado para a inicial de fallback)
 * @param isUploading     Se está fazendo upload no momento
 * @param size            Tamanho do círculo em dp (padrão 88)
 * @param onPickImage     Callback com a URI selecionada da galeria
 * @param onDeleteAvatar  Callback para excluir o avatar atual
 */
@Composable
fun AvatarPicker(
    avatarUrl: String?,
    displayName: String,
    isUploading: Boolean,
    size: Int = 88,
    onPickImage: (android.net.Uri) -> Unit,
    onDeleteAvatar: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPickImage(it) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {

            // ── Círculo do avatar ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(size.dp)
                    // Borda azul externa (mais visível)
                    .border(2.5.dp, WtcBlue.copy(alpha = 0.5f), CircleShape)
                    .padding(3.dp)
                    // Borda interna branca (separa da foto)
                    .border(2.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(WtcBluePale)
                    .clickable { if (!isUploading) showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> CircularProgressIndicator(
                        modifier = Modifier.size((size * 0.38f).dp),
                        color = WtcBlue, strokeWidth = 3.dp
                    )
                    !avatarUrl.isNullOrBlank() -> AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                    else -> Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = (size * 0.37f).sp,
                        fontWeight = FontWeight.Bold,
                        color = WtcBlue
                    )
                }
            }

            // ── Ícone câmera no canto ─────────────────────────────────
            if (!isUploading) {
                Box(
                    modifier = Modifier
                        .size((size * 0.30f).dp)
                        .clip(CircleShape)
                        .background(WtcBlue)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Alterar foto",
                        tint = Color.White,
                        modifier = Modifier.size((size * 0.15f).dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isUploading) "Enviando foto..." else "Toque para alterar a foto",
            fontSize = 11.sp,
            color = if (isUploading) WtcBlue else TextMuted
        )
    }

    // ── Menu de opções ────────────────────────────────────────────────────────
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = {
                Text("Foto de perfil", fontWeight = FontWeight.Bold,
                    color = TextPrimary, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Editar foto
                    Surface(
                        onClick = {
                            showMenu = false
                            launcher.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = WtcBlue.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WtcBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null,
                                    tint = WtcBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Editar foto", fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("Escolher uma nova foto da galeria",
                                    fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }

                    // Excluir foto (só aparece se tiver foto)
                    if (!avatarUrl.isNullOrBlank()) {
                        Surface(
                            onClick = {
                                showMenu = false
                                onDeleteAvatar()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFC62828).copy(alpha = 0.07f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFC62828).copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null,
                                        tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Excluir foto", fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium, color = Color(0xFFC62828))
                                    Text("Volta a exibir a inicial do seu nome",
                                        fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}