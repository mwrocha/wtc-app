package br.com.fiap.wtcconnecta.ui.screens.client

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.ui.components.ImageMessageBubble
import br.com.fiap.wtcconnecta.ui.components.ImagePickerButton
import br.com.fiap.wtcconnecta.ui.components.ImagePreviewBar
import br.com.fiap.wtcconnecta.ui.components.MessageActionsState
import br.com.fiap.wtcconnecta.ui.components.PdfMessageBubble
import br.com.fiap.wtcconnecta.ui.components.SwipeableMessageBubble
import br.com.fiap.wtcconnecta.viewmodel.ChatViewModel
import br.com.fiap.wtcconnecta.viewmodel.ImageUploadViewModel
import br.com.fiap.wtcconnecta.ui.components.MessageStatusIcon
import br.com.fiap.wtcconnecta.ui.components.RatingDialog

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val WtcBlueDark = Color(0xFF063D5C)
private val BubbleOwn   = Color(0xFF0B537B)
private val BubbleOther = Color.White
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

private val IMG_REGEX = Regex("""\[img:(images/[^\]]+)]""")
private val PDF_REGEX = Regex("""\[pdf:(images/[^\]]+)]""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatName: String,
    chatType: String,
    onBack: () -> Unit,
    loggedInUserId: String,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val context           = LocalContext.current
    var messageText       by remember { mutableStateOf("") }
    var replyTo           by remember { mutableStateOf<Message?>(null) }
    var messageToEdit     by remember { mutableStateOf<Message?>(null) }
    var messageToDelete   by remember { mutableStateOf<Message?>(null) }
    val listState         = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val uploadViewModel: ImageUploadViewModel = viewModel()
    val uploadState by uploadViewModel.uiState.collectAsState()
    var pendingFileUri  by remember { mutableStateOf<String?>(null) }
    var pendingFileKey  by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var pendingIsPdf    by remember { mutableStateOf(false) }

    LaunchedEffect(chatId, chatType) {
        Log.d("ChatScreen", "Abrindo chat: id=$chatId, type=$chatType")
        viewModel.loadMessages(chatId, chatType, loggedInUserId)
        viewModel.startPolling()
    }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let { snackbarHostState.showSnackbar(it); viewModel.clearActionError() }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F6FA),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chatName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(chatName, fontWeight = FontWeight.Bold,
                            color = Color.White, fontSize = 15.sp, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF4CAF50)))
                            Text(if (chatType == "group") "Grupo" else "Atendimento",
                                fontSize = 11.sp, color = Color.White.copy(alpha = 0.70f))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.messages.isEmpty() -> Box(
                    Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                            .background(WtcBluePale), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Chat, contentDescription = null,
                                tint = WtcBlue, modifier = Modifier.size(36.dp))
                        }
                        Text("Nenhuma mensagem ainda.", color = TextPrimary,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Seja o primeiro a enviar!", color = TextMuted, fontSize = 13.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        state = listState
                    ) {
                        items(uiState.messages) { message ->
                            val isOwn    = message.senderId == loggedInUserId
                            val imgMatch = IMG_REGEX.find(message.displayContent)
                            val pdfMatch = PDF_REGEX.find(message.displayContent)

                            when {
                                // ── Imagem ────────────────────────────────────
                                imgMatch != null -> {
                                    val objectKey = imgMatch.groupValues[1]
                                    val caption   = message.displayContent
                                        .replace(imgMatch.value, "").trim()
                                    var imageUrl by remember(objectKey) { mutableStateOf("") }
                                    LaunchedEffect(objectKey) {
                                        try {
                                            val resp = RetrofitClient.instance.getPresignedUrl(objectKey)
                                            if (resp.isSuccessful) imageUrl = resp.body()?.url ?: ""
                                        } catch (_: Exception) {}
                                    }
                                    SwipeableMessageBubble(
                                        message           = message,
                                        isFromCurrentUser = isOwn,
                                        senderName        = viewModel.getSenderName(message.senderId),
                                        onReply           = { replyTo = it },
                                        onEdit            = null,
                                        onDelete          = if (isOwn) ({ messageToDelete = message }) else null
                                    ) {
                                        ImageMessageBubble(
                                            imageUrl          = imageUrl,
                                            caption           = caption.ifBlank { null },
                                            isFromCurrentUser = isOwn
                                        )
                                    }
                                }
                                // ── PDF ───────────────────────────────────────
                                pdfMatch != null -> {
                                    val objectKey = pdfMatch.groupValues[1]
                                    val fileName  = message.displayContent
                                        .replace(pdfMatch.value, "").trim()
                                        .ifBlank { objectKey.substringAfterLast("/") }
                                    var pdfUrl by remember(objectKey) { mutableStateOf("") }
                                    LaunchedEffect(objectKey) {
                                        try {
                                            val resp = RetrofitClient.instance.getPresignedUrl(objectKey)
                                            if (resp.isSuccessful) pdfUrl = resp.body()?.url ?: ""
                                        } catch (_: Exception) {}
                                    }
                                    SwipeableMessageBubble(
                                        message           = message,
                                        isFromCurrentUser = isOwn,
                                        senderName        = viewModel.getSenderName(message.senderId),
                                        onReply           = { replyTo = it },
                                        onEdit            = null,
                                        onDelete          = if (isOwn) ({ messageToDelete = message }) else null
                                    ) {
                                        PdfMessageBubble(
                                            fileName          = fileName,
                                            isFromCurrentUser = isOwn,
                                            onOpen            = {
                                                if (pdfUrl.isNotBlank()) {
                                                    val intent = Intent(Intent.ACTION_VIEW,
                                                        Uri.parse(pdfUrl))
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    context.startActivity(intent)
                                                }
                                            }
                                        )
                                    }
                                }
                                // ── Texto normal ──────────────────────────────
                                else -> {
                                    SwipeableMessageBubble(
                                        message           = message,
                                        isFromCurrentUser = isOwn,
                                        senderName        = viewModel.getSenderName(message.senderId),
                                        onReply           = { replyTo = it },
                                        onEdit   = if (isOwn) ({ messageToEdit = message }) else null,
                                        onDelete = if (isOwn) ({ messageToDelete = message }) else null
                                    ) {
                                        MessageBubble(
                                            message           = message,
                                            isFromCurrentUser = isOwn,
                                            senderName        = viewModel.getSenderName(message.senderId),
                                            isImportant       = MessageActionsState.isImportant(message.id)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(uiState.messages.size) {
                        if (uiState.messages.isNotEmpty())
                            listState.animateScrollToItem(uiState.messages.lastIndex)
                    }
                }
            }

            // ── Quote de reply ────────────────────────────────────────────────
            replyTo?.let { reply ->
                Surface(modifier = Modifier.fillMaxWidth(), color = WtcBluePale) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.width(3.dp).height(36.dp),
                            color = WtcBlue, shape = RoundedCornerShape(2.dp)) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Respondendo a ${viewModel.getSenderName(reply.senderId)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WtcBlue, fontWeight = FontWeight.SemiBold)
                            Text(reply.displayContent, style = MaterialTheme.typography.bodySmall,
                                color = TextMuted, maxLines = 1)
                        }
                        IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = TextMuted)
                        }
                    }
                }
            }

            // ── Preview do arquivo pendente ───────────────────────────────────
            pendingFileUri?.let { uri ->
                ImagePreviewBar(
                    imageUrl = uri,
                    isPdf    = pendingIsPdf,
                    fileName = pendingFileName,
                    onCancel = {
                        pendingFileUri  = null
                        pendingFileKey  = null
                        pendingFileName = null
                        pendingIsPdf    = false
                        uploadViewModel.reset()
                    }
                )
            }

            // ── Barra de input ────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                color          = Color.White,
                shape          = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                            .background(WtcBluePale),
                        contentAlignment = Alignment.Center
                    ) {
                        ImagePickerButton(
                            uploadViewModel = uploadViewModel,
                            onImageReady    = { url, key ->
                                pendingFileUri  = url
                                pendingFileKey  = key
                                pendingIsPdf    = uploadState.isPdf
                                pendingFileName = uploadState.fileName
                            }
                        )
                    }

                    OutlinedTextField(
                        value         = messageText,
                        onValueChange = { messageText = it },
                        modifier      = Modifier.weight(1f).heightIn(min = 44.dp),
                        placeholder   = {
                            Text(
                                if (replyTo != null) "Digite sua resposta..."
                                else "Mensagem ou / para comandos...",
                                color = TextMuted.copy(alpha = 0.6f), fontSize = 14.sp
                            )
                        },
                        shape  = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = Color(0xFFF5FAFD),
                            unfocusedContainerColor = Color(0xFFF5FAFD),
                            focusedBorderColor      = WtcBlue,
                            unfocusedBorderColor    = WtcBlueHint
                        )
                    )

                    FilledIconButton(
                        onClick = {
                            val finalText = buildString {
                                if (!pendingFileKey.isNullOrBlank()) {
                                    if (pendingIsPdf) {
                                        append("[pdf:$pendingFileKey] ")
                                        if (!pendingFileName.isNullOrBlank())
                                            append(pendingFileName)
                                    } else {
                                        append("[img:$pendingFileKey] ")
                                    }
                                }
                                if (replyTo != null)
                                    append("↩ ${viewModel.getSenderName(replyTo!!.senderId)}: \"${replyTo!!.content.take(30)}...\"\n")
                                append(messageText)
                            }
                            if (finalText.isNotBlank()) {
                                viewModel.sendMessage(finalText, chatId, chatType, loggedInUserId)
                                messageText     = ""
                                replyTo         = null
                                pendingFileUri  = null
                                pendingFileKey  = null
                                pendingFileName = null
                                pendingIsPdf    = false
                                uploadViewModel.reset()
                            }
                        },
                        enabled = messageText.isNotBlank() || pendingFileKey != null,
                        shape   = RoundedCornerShape(14.dp),
                        colors  = IconButtonDefaults.filledIconButtonColors(
                            containerColor         = WtcBlue,
                            disabledContainerColor = WtcBlueHint
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // ── Dialog de avaliação ───────────────────────────────────────────────────
    if (uiState.showRatingDialog) {
        RatingDialog(
            onSubmit  = { stars, comment -> viewModel.submitRating(stars, comment) },
            onDismiss = { viewModel.dismissRating() }
        )
    }

    // ── Dialog editar ─────────────────────────────────────────────────────────
    messageToEdit?.let { msg ->
        var editText by remember { mutableStateOf(msg.displayContent) }
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = { Text("Editar mensagem", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(value = editText, onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Mensagem") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, cursorColor = WtcBlue))
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.editMessage(msg.id, editText); messageToEdit = null },
                    enabled  = editText.isNotBlank() && editText != msg.displayContent,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    // ── Dialog excluir ────────────────────────────────────────────────────────
    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Excluir mensagem?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Esta ação não pode ser desfeita.", color = TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = WtcBluePale, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Text(msg.displayContent, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp), maxLines = 3, color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMessage(msg.id); messageToDelete = null }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

// ── MessageBubble ─────────────────────────────────────────────────────────────

@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    senderName: String = "",
    isImportant: Boolean = false
) {
    val bubbleColor = if (isFromCurrentUser) BubbleOwn else BubbleOther
    val textColor   = if (isFromCurrentUser) Color.White else TextPrimary
    val alignment   = if (isFromCurrentUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalAlignment = alignment) {
        if (!isFromCurrentUser && senderName.isNotEmpty()) {
            Text(senderName, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = WtcBlue,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp), fontSize = 11.sp)
        }
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                bottomEnd   = if (isFromCurrentUser) 4.dp else 18.dp,
                bottomStart = if (isFromCurrentUser) 18.dp else 4.dp),
            color           = if (isImportant) bubbleColor.copy(alpha = 0.85f) else bubbleColor,
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier        = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (isImportant) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null,
                            tint = if (isFromCurrentUser) Color.White.copy(alpha = 0.8f)
                            else Color(0xFFE8B84B),
                            modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importante", fontSize = 10.sp,
                            color = if (isFromCurrentUser) Color.White.copy(alpha = 0.8f)
                            else Color(0xFFE8B84B))
                    }
                }
                Text(message.displayContent, fontSize = 14.sp,
                    color = textColor, lineHeight = 20.sp)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically) {
                    if (message.edited == true) {
                        Text("editada", fontSize = 9.sp, color = textColor.copy(alpha = 0.45f))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(formatMessageTime(message.createdAt), fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.55f))
                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

private val CircleShape = RoundedCornerShape(50)

private fun formatMessageTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val timePart = when {
            createdAt.contains("T") -> createdAt.substringAfter("T").take(5)
            createdAt.contains(" ") -> createdAt.substringAfter(" ").take(5)
            else                    -> createdAt.take(5)
        }
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
        } else timePart
    } catch (e: Exception) { "" }
}