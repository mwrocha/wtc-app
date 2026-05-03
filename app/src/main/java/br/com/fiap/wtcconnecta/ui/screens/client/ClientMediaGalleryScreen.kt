package br.com.fiap.wtcconnecta.ui.screens.client

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import coil.compose.AsyncImage

private val WtcBlue = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted = Color(0xFF6E90A0)

private val IMG_REGEX = Regex("""\[img:(images/[^\]]+)]""")
private val PDF_REGEX = Regex("""\[pdf:(images/[^\]]+)]""")

data class MediaItem(
    val objectKey: String, val fileName: String, val isPdf: Boolean, val conversationId: String?
)

@Composable
fun ClientMediaGalleryScreen(
    clientId: String, onBack: () -> Unit
) {
    val context = LocalContext.current
    var images by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var pdfs by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var fullscreenImage by remember { mutableStateOf<String?>(null) }

    // Carrega mensagens e filtra mídia
    LaunchedEffect(clientId) {
        try {
            val messages = RetrofitClient.instance.getMyConversations()
            val imgItems = mutableListOf<MediaItem>()
            val pdfItems = mutableListOf<MediaItem>()

            messages.forEach { msg ->
                IMG_REGEX.findAll(msg.displayContent ?: "").forEach { match ->
                    imgItems.add(
                        MediaItem(
                            objectKey = match.groupValues[1],
                            fileName = match.groupValues[1].substringAfterLast("/"),
                            isPdf = false,
                            conversationId = msg.conversationId
                        )
                    )
                }
                PDF_REGEX.findAll(msg.displayContent ?: "").forEach { match ->
                    val caption = msg.displayContent?.replace(match.value, "")?.trim() ?: ""
                    pdfItems.add(
                        MediaItem(
                            objectKey = match.groupValues[1],
                            fileName = caption.ifBlank { match.groupValues[1].substringAfterLast("/") },
                            isPdf = true,
                            conversationId = msg.conversationId
                        )
                    )
                }
            }

            images = imgItems.distinctBy { it.objectKey }
            pdfs = pdfItems.distinctBy { it.objectKey }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F6FA))
    ) {
        // ── Header Hero ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = 200.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .offset(x = 260.dp, y = 40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Galeria", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Imagens e documentos trocados",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pills de contagem
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (images.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${images.size} imagem(ns)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                    if (pdfs.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "${pdfs.size} PDF(s)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Tabs ──────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = WtcBlue
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Imagens", fontSize = 13.sp) },
                icon = {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                })
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Documentos", fontSize = 13.sp) },
                icon = {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                })
        }

        // ── Conteúdo ──────────────────────────────────────────────────────────
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue)
            }

            selectedTab == 0 -> {
                if (images.isEmpty()) {
                    EmptyMediaState(
                        icon = Icons.Default.Image,
                        message = "Nenhuma imagem encontrada",
                        sub = "Imagens trocadas no chat aparecerão aqui."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(images) { item ->
                            ImageGridItem(
                                objectKey = item.objectKey,
                                onOpen = { url -> fullscreenImage = url })
                        }
                    }
                }
            }

            else -> {
                if (pdfs.isEmpty()) {
                    EmptyMediaState(
                        icon = Icons.Default.PictureAsPdf,
                        message = "Nenhum documento encontrado",
                        sub = "PDFs trocados no chat aparecerão aqui."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pdfs) { item ->
                            PdfListItem(
                                item = item, onOpen = { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                })
                        }
                    }
                }
            }
        }
    }

    // ── Visualizador fullscreen de imagem ─────────────────────────────────────
    fullscreenImage?.let { url ->
        Dialog(
            onDismissRequest = { fullscreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "Imagem em tela cheia",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                // Botão fechar
                IconButton(
                    onClick = { fullscreenImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Item do grid de imagens ───────────────────────────────────────────────────

@Composable
fun ImageGridItem(objectKey: String, onOpen: (url: String) -> Unit) {
    var imageUrl by remember(objectKey) { mutableStateOf("") }

    LaunchedEffect(objectKey) {
        try {
            val resp = RetrofitClient.instance.getPresignedUrl(objectKey)
            if (resp.isSuccessful) imageUrl = resp.body()?.url ?: ""
        } catch (_: Exception) {
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFD0E8F2))
            .clickable { if (imageUrl.isNotBlank()) onOpen(imageUrl) }) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagem",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = WtcBlue
                )
            }
        }
    }
}

// ── Item da lista de PDFs ─────────────────────────────────────────────────────

@Composable
fun PdfListItem(item: MediaItem, onOpen: (url: String) -> Unit) {
    var pdfUrl by remember(item.objectKey) { mutableStateOf("") }

    LaunchedEffect(item.objectKey) {
        try {
            val resp = RetrofitClient.instance.getPresignedUrl(item.objectKey)
            if (resp.isSuccessful) pdfUrl = resp.body()?.url ?: ""
        } catch (_: Exception) {
        }
    }

    Card(
        onClick = { if (pdfUrl.isNotBlank()) onOpen(pdfUrl) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFEBEE)), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2
                )
                Text(
                    "Toque para abrir",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = WtcBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Estado vazio ──────────────────────────────────────────────────────────────

@Composable
fun EmptyMediaState(
    icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, sub: String
) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WtcBluePale), contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(36.dp)
                )
            }
            Text(message, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(sub, fontSize = 13.sp, color = TextMuted)
        }
    }
}