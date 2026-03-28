package br.com.fiap.wtcconnecta.ui.screens.client

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.ui.components.MessageActionsState
import br.com.fiap.wtcconnecta.ui.components.SwipeableMessageBubble
import br.com.fiap.wtcconnecta.viewmodel.ChatViewModel

private val WtcBlue      = Color(0xFF0B537B)
private val WtcBlueSoft  = Color(0xFF1A6E9A)
private val WtcBluePale  = Color(0xFFEEF6FB)
private val WtcBlueHint  = Color(0xFFD0E8F2)
private val BubbleOwn    = Color(0xFF0B537B)
private val BubbleOther  = Color.White
private val TextPrimary  = Color(0xFF0D2B3E)
private val TextMuted    = Color(0xFF6E90A0)

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
    var messageText       by remember { mutableStateOf("") }
    var replyTo           by remember { mutableStateOf<Message?>(null) }
    var messageToEdit     by remember { mutableStateOf<Message?>(null) }
    var messageToDelete   by remember { mutableStateOf<Message?>(null) }
    val listState         = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatId, chatType) {
        Log.d("ChatScreen", "Abrindo chat: id=$chatId, type=$chatType")
        viewModel.loadMessages(chatId, chatType, loggedInUserId)
        viewModel.startPolling()
    }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let { snackbarHostState.showSnackbar(it); viewModel.clearActionError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5FAFD),
        topBar = {
            TopAppBar(
                title = {
                    Text(chatName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                            tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WtcBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F6FA))
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.messages.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, contentDescription = null,
                            tint = WtcBlueHint, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nenhuma mensagem ainda.", color = TextMuted, fontSize = 14.sp)
                        Text("Seja o primeiro a enviar!", color = WtcBlueHint, fontSize = 12.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        state = listState
                    ) {
                        items(uiState.messages) { message ->
                            val isOwn = message.senderId == loggedInUserId
                            SwipeableMessageBubble(
                                message           = message,
                                isFromCurrentUser = isOwn,
                                senderName        = viewModel.getSenderName(message.senderId),
                                onReply           = { replyTo = it },
                                onEdit            = if (isOwn) ({ messageToEdit = message }) else null,
                                onDelete          = if (isOwn) ({ messageToDelete = message }) else null
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
                    LaunchedEffect(uiState.messages.size) {
                        if (uiState.messages.isNotEmpty())
                            listState.animateScrollToItem(uiState.messages.lastIndex)
                    }
                }
            }

            // Quote de reply
            replyTo?.let { reply ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WtcBluePale
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.width(3.dp).height(36.dp),
                            color = WtcBlue,
                            shape = RoundedCornerShape(2.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Respondendo a ${viewModel.getSenderName(reply.senderId)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WtcBlue, fontWeight = FontWeight.SemiBold
                            )
                            Text(reply.displayContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted, maxLines = 1)
                        }
                        IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = TextMuted)
                        }
                    }
                }
            }

            // Input
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                color = Color.White,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        placeholder = {
                            Text(
                                if (replyTo != null) "Digite sua resposta..." else "Mensagem...",
                                color = TextMuted.copy(alpha = 0.6f), fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = Color(0xFFF0F6FA),
                            unfocusedContainerColor = Color(0xFFF0F6FA),
                            focusedBorderColor      = WtcBlue,
                            unfocusedBorderColor    = WtcBlueHint
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val textToSend = if (replyTo != null)
                                    "↩ ${viewModel.getSenderName(replyTo!!.senderId)}: \"${replyTo!!.content.take(30)}...\"\n$messageText"
                                else messageText
                                viewModel.sendMessage(textToSend, chatId, chatType, loggedInUserId)
                                messageText = ""
                                replyTo = null
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = WtcBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // Dialog editar
    messageToEdit?.let { msg ->
        var editText by remember { mutableStateOf(msg.displayContent) }
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = { Text("Editar mensagem", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mensagem") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, cursorColor = WtcBlue
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.editMessage(msg.id, editText); messageToEdit = null },
                    enabled = editText.isNotBlank() && editText != msg.displayContent,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { messageToEdit = null }) { Text("Cancelar", color = TextMuted) } }
        )
    }

    // Dialog excluir
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
                    Text("Excluir", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("Cancelar", color = TextMuted) } }
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalAlignment = alignment
    ) {
        if (!isFromCurrentUser && senderName.isNotEmpty()) {
            Text(senderName, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = WtcBlue,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp), fontSize = 11.sp)
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 18.dp,
                bottomStart = if (isFromCurrentUser) 18.dp else 4.dp
            ),
            color = if (isImportant) bubbleColor.copy(alpha = 0.85f) else bubbleColor,
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
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
                Text(message.displayContent, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.edited == true) {
                        Text("editada", fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.45f))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(formatMessageTime(message.createdAt), fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.55f))
                }
            }
        }
    }
}

private val CircleShape = RoundedCornerShape(50)

private fun formatMessageTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try { createdAt.substringAfter("T").take(5) } catch (e: Exception) { "" }
}