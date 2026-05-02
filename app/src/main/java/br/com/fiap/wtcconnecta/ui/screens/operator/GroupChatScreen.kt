package br.com.fiap.wtcconnecta.ui.screens.operator

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.remote.GroupMessageRequest
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import br.com.fiap.wtcconnecta.ui.components.ImageMessageBubble
import br.com.fiap.wtcconnecta.ui.components.ImagePickerButton
import br.com.fiap.wtcconnecta.ui.components.ImagePreviewBar
import br.com.fiap.wtcconnecta.ui.components.PdfMessageBubble
import br.com.fiap.wtcconnecta.ui.components.SwipeableMessageBubble
import br.com.fiap.wtcconnecta.viewmodel.ImageUploadViewModel
import br.com.fiap.wtcconnecta.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

private val IMG_REGEX = Regex("""\[img:(images/[^\]]+)]""")
private val PDF_REGEX = Regex("""\[pdf:(images/[^\]]+)]""")

// ── UiState ───────────────────────────────────────────────────────────────────

data class GroupChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val senderNames: Map<String, String> = emptyMap()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class GroupChatViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState = _uiState.asStateFlow()

    private var currentGroupId = ""
    private var currentOperatorId = ""

    fun load(groupId: String, operatorId: String) {
        currentGroupId = groupId
        currentOperatorId = operatorId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchMessages()
        }
    }

    private suspend fun fetchMessages() {
        try {
            val messages = repository.getConversation(currentGroupId)

            // Resolve nomes dos remetentes
            val knownIds = _uiState.value.senderNames.keys
            val unknown  = messages.map { it.senderId }
                .filter { it.isNotBlank() && it !in knownIds }.distinct()
            val newNames = mutableMapOf<String, String>()
            for (id in unknown) {
                try {
                    val user = repository.getUserByEmail(id)
                    if (user != null) newNames[id] = user.name
                } catch (_: Exception) {}
            }

            _uiState.update {
                it.copy(
                    isLoading   = false,
                    messages    = messages.sortedBy { m -> m.createdAt },
                    senderNames = it.senderNames + newNames,
                    error       = null
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar mensagens.") }
        }
    }

    fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                if (currentGroupId.isNotBlank()) {
                    try { fetchMessages() } catch (_: Exception) {}
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentGroupId.isBlank()) return
        viewModelScope.launch {
            try {
                RetrofitClient.instance.sendGroupMessage(
                    GroupMessageRequest(groupId = currentGroupId, body = text)
                )
                fetchMessages()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao enviar mensagem.") }
            }
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newContent)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(contentRaw = newContent, edited = true)
                            else msg
                        })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Erro ao editar mensagem") }
                }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(messages = state.messages.filter { it.id != messageId })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Erro ao excluir mensagem") }
                }
        }
    }

    fun getSenderName(senderId: String): String =
        _uiState.value.senderNames[senderId] ?: senderId

    fun isFromCurrentOperator(senderId: String): Boolean =
        senderId.equals(currentOperatorId, ignoreCase = true)

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    groupName: String,
    operatorId: String,
    onBack: () -> Unit,
    viewModel: GroupChatViewModel = viewModel(),
    taskViewModel: TaskViewModel  = viewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val context      = LocalContext.current

    var messageText     by remember { mutableStateOf("") }
    var replyTo         by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    val uploadViewModel: ImageUploadViewModel = viewModel()
    val uploadState by uploadViewModel.uiState.collectAsState()
    var pendingFileUri  by remember { mutableStateOf<String?>(null) }
    var pendingFileKey  by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var pendingIsPdf    by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.load(groupId, operatorId)
        viewModel.startPolling()
    }

    Scaffold(containerColor = Color(0xFFF0F6FA)) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Header ────────────────────────────────────────────────────────
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(groupName, fontWeight = FontWeight.Bold,
                            color = Color.White, fontSize = 18.sp, maxLines = 1)
                        Text("Chat do grupo", fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f))
                    }
                }
            }

            // ── Mensagens ─────────────────────────────────────────────────────
            when {
                uiState.isLoading -> Box(Modifier.weight(1f), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.messages.isEmpty() -> Box(Modifier.weight(1f), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, contentDescription = null,
                            tint = WtcBlueHint, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Nenhuma mensagem ainda", color = TextMuted, fontSize = 14.sp)
                        Text("Seja o primeiro a escrever para o grupo!",
                            color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
                else -> LazyColumn(
                    modifier      = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                    reverseLayout = true
                ) {
                    items(uiState.messages.reversed()) { message ->
                        val isOwn    = viewModel.isFromCurrentOperator(message.senderId)
                        val imgMatch = IMG_REGEX.find(message.displayContent)
                        val pdfMatch = PDF_REGEX.find(message.displayContent)

                        when {
                            imgMatch != null -> {
                                val objectKey = imgMatch.groupValues[1]
                                val caption   = message.displayContent.replace(imgMatch.value, "").trim()
                                var imageUrl  by remember(objectKey) { mutableStateOf("") }
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
                                    clientId          = groupId,
                                    clientName        = groupName,
                                    onReply           = { replyTo = it },
                                    onCreateTask      = { req: TaskRequest -> taskViewModel.createTask(req) },
                                    onDelete          = if (isOwn) ({ messageToDelete = message }) else null
                                ) {
                                    ImageMessageBubble(
                                        imageUrl          = imageUrl,
                                        caption           = caption.ifBlank { null },
                                        isFromCurrentUser = isOwn
                                    )
                                }
                            }
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
                                    clientId          = groupId,
                                    clientName        = groupName,
                                    onReply           = { replyTo = it },
                                    onCreateTask      = { req: TaskRequest -> taskViewModel.createTask(req) },
                                    onDelete          = if (isOwn) ({ messageToDelete = message }) else null
                                ) {
                                    PdfMessageBubble(
                                        fileName          = fileName,
                                        isFromCurrentUser = isOwn,
                                        onOpen            = {
                                            if (pdfUrl.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                            else -> {
                                SwipeableMessageBubble(
                                    message           = message,
                                    isFromCurrentUser = isOwn,
                                    senderName        = viewModel.getSenderName(message.senderId),
                                    clientId          = groupId,
                                    clientName        = groupName,
                                    onReply           = { replyTo = it },
                                    onCreateTask      = { req: TaskRequest -> taskViewModel.createTask(req) },
                                    onDelete          = if (isOwn) ({ messageToDelete = message }) else null
                                ) {
                                    GroupMessageBubble(
                                        message           = message,
                                        isFromCurrentUser = isOwn,
                                        senderName        = viewModel.getSenderName(message.senderId)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Reply bar ─────────────────────────────────────────────────────
            replyTo?.let { reply ->
                Surface(modifier = Modifier.fillMaxWidth(), color = WtcBluePale) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.width(3.dp).height(36.dp),
                            color = WtcBlue, shape = RoundedCornerShape(2.dp)) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Respondendo", fontSize = 11.sp,
                                color = WtcBlue, fontWeight = FontWeight.SemiBold)
                            Text(reply.displayContent, fontSize = 12.sp,
                                color = TextMuted, maxLines = 1)
                        }
                        IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = TextMuted)
                        }
                    }
                }
            }

            // ── Preview arquivo pendente ──────────────────────────────────────
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

            // ── Input de mensagem ─────────────────────────────────────────────
            Surface(color = Color.White, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(WtcBluePale),
                        contentAlignment = Alignment.Center
                    ) {
                        ImagePickerButton(
                            uploadViewModel = uploadViewModel,
                            onImageReady    = { url, key ->
                                pendingFileUri  = url
                                pendingFileKey  = key
                                pendingIsPdf    = key.endsWith(".pdf", ignoreCase = true)
                                pendingFileName = uploadState.fileName
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value         = messageText,
                        onValueChange = { messageText = it },
                        modifier      = Modifier.weight(1f).heightIn(min = 44.dp),
                        placeholder   = {
                            Text("Mensagem para o grupo...",
                                color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp)
                        },
                        shape  = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = WtcBlue,
                            unfocusedBorderColor    = WtcBlueHint,
                            focusedContainerColor   = Color(0xFFF0F6FA),
                            unfocusedContainerColor = Color(0xFFF0F6FA)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val finalText = buildString {
                                if (!pendingFileKey.isNullOrBlank()) {
                                    if (pendingIsPdf) {
                                        append("[pdf:$pendingFileKey] ")
                                        if (!pendingFileName.isNullOrBlank()) append(pendingFileName)
                                    } else {
                                        append("[img:$pendingFileKey] ")
                                    }
                                }
                                append(messageText)
                            }
                            if (finalText.isNotBlank()) {
                                viewModel.sendMessage(finalText)
                                messageText     = ""
                                pendingFileUri  = null
                                pendingFileKey  = null
                                pendingFileName = null
                                pendingIsPdf    = false
                                uploadViewModel.reset()
                            }
                        },
                        enabled = messageText.isNotBlank() || pendingFileKey != null,
                        shape   = RoundedCornerShape(50),
                        colors  = IconButtonDefaults.filledIconButtonColors(containerColor = WtcBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // ── Dialog excluir mensagem ───────────────────────────────────────────────
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
                        Text(msg.displayContent,
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 3, color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(msg.id)
                    messageToDelete = null
                }) {
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

// ── Bubble de mensagem de grupo ───────────────────────────────────────────────

@Composable
fun GroupMessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    senderName: String
) {
    val alignment   = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isFromCurrentUser) WtcBlue else Color.White
    val textColor   = if (isFromCurrentUser) Color.White else TextPrimary

    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalAlignment = alignment
    ) {
        if (!isFromCurrentUser) {
            Text(senderName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = WtcBlue, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
        }
        Surface(
            color  = bubbleColor,
            shape  = RoundedCornerShape(
                topStart    = 18.dp, topEnd      = 18.dp,
                bottomEnd   = if (isFromCurrentUser) 4.dp else 18.dp,
                bottomStart = if (isFromCurrentUser) 18.dp else 4.dp
            ),
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier        = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(message.displayContent, fontSize = 14.sp,
                    color = textColor, lineHeight = 20.sp)
                if (message.edited) {
                    Text("editada", fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    formatGroupTime(message.createdAt),
                    fontSize  = 10.sp,
                    color     = textColor.copy(alpha = 0.55f),
                    textAlign = TextAlign.End,
                    modifier  = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatGroupTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try { createdAt.drop(11).take(5) } catch (_: Exception) { "" }
}