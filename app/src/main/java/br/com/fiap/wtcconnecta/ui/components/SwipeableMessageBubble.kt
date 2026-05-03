package br.com.fiap.wtcconnecta.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import kotlin.math.abs

private val WtcBlue = Color(0xFF0B537B)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted = Color(0xFF6E90A0)

// ── Modelo de tarefa local ────────────────────────────────────────────────────

data class MessageTask(
    val messageId: String, val text: String, val createdAt: String? = null
)

// ── Estado global ─────────────────────────────────────────────────────────────

object MessageActionsState {
    val importantMessages = mutableStateListOf<String>()
    val tasks = mutableStateListOf<MessageTask>()

    fun toggleImportant(messageId: String) {
        if (importantMessages.contains(messageId)) importantMessages.remove(messageId)
        else importantMessages.add(messageId)
    }

    fun addTask(message: Message) {
        if (tasks.none { it.messageId == message.id }) tasks.add(
            MessageTask(
                message.id, message.content, message.createdAt
            )
        )
    }

    fun isImportant(messageId: String) = importantMessages.contains(messageId)
}

// ── SwipeableMessageBubble ────────────────────────────────────────────────────

@Composable
fun SwipeableMessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    senderName: String,
    clientId: String = "",
    clientName: String = "",
    onReply: (Message) -> Unit = {},
    onCreateTask: ((TaskRequest) -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showTaskForm by remember { mutableStateOf(false) }
    val isImportant = MessageActionsState.isImportant(message.id)

    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")
    val bgColor by animateColorAsState(
        targetValue = if (abs(offsetX) > 60) WtcBluePale else Color.Transparent, label = "swipe_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { if (abs(offsetX) > 60) showMenu = true; offsetX = 0f },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-120f, 120f)
                    })
            }) {
        if (abs(animatedOffset) > 20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center),
                contentAlignment = if (animatedOffset > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.SwipeRight,
                    contentDescription = null,
                    tint = WtcBlue.copy(alpha = (abs(animatedOffset) / 120f).coerceIn(0f, 1f)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(modifier = Modifier.offset(x = (animatedOffset * 0.3f).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isImportant && !isFromCurrentUser) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFE8B84B),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                }
                content()
                if (isImportant && isFromCurrentUser) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFE8B84B),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }

    if (showMenu) {
        MessageActionsMenu(
            message = message,
            isImportant = isImportant,
            showTask = onCreateTask != null,
            showEdit = onEdit != null,
            showDelete = onDelete != null,
            onDismiss = { showMenu = false },
            onImportant = { MessageActionsState.toggleImportant(message.id); showMenu = false },
            onCreateTask = {
                showMenu = false
                if (onCreateTask != null) showTaskForm = true
                else MessageActionsState.addTask(message)
            },
            onEdit = { onEdit?.invoke(); showMenu = false },
            onDelete = { onDelete?.invoke(); showMenu = false },
            onReply = { onReply(message); showMenu = false },
            onCopy = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("mensagem", message.content))
                showMenu = false
            })
    }

    if (showTaskForm && onCreateTask != null) {
        TaskFormDialog(
            clientId = clientId,
            clientName = clientName,
            messageRef = message.content,
            onDismiss = { showTaskForm = false },
            onConfirm = { request -> onCreateTask(request); showTaskForm = false })
    }
}

// ── Menu de ações ─────────────────────────────────────────────────────────────

@Composable
fun MessageActionsMenu(
    message: Message,
    isImportant: Boolean,
    showTask: Boolean = true,
    showEdit: Boolean = false,
    showDelete: Boolean = false,
    onDismiss: () -> Unit,
    onImportant: () -> Unit,
    onCreateTask: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReply: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(
            "Ações da mensagem", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary
        )
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Preview da mensagem
            Surface(
                color = WtcBluePale,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    message.content,
                    modifier = Modifier.padding(10.dp),
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Destacar
            MessageActionItem(
                icon = if (isImportant) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (isImportant) "Remover destaque" else "Marcar como importante",
                accent = Color(0xFFE8B84B),
                onClick = onImportant
            )
            // Responder
            MessageActionItem(
                icon = Icons.Default.Reply, label = "Responder", accent = WtcBlue, onClick = onReply
            )
            // Copiar
            MessageActionItem(
                icon = Icons.Default.ContentCopy,
                label = "Copiar texto",
                accent = TextMuted,
                onClick = onCopy
            )
            // Criar tarefa
            if (showTask) MessageActionItem(
                icon = Icons.Default.TaskAlt,
                label = "Criar tarefa",
                accent = Color(0xFF1A7A5E),
                onClick = onCreateTask
            )
            // Editar
            if (showEdit) MessageActionItem(
                icon = Icons.Default.Edit,
                label = "Editar mensagem",
                accent = WtcBlue,
                onClick = onEdit
            )
            // Excluir
            if (showDelete) MessageActionItem(
                icon = Icons.Default.Delete,
                label = "Excluir mensagem",
                accent = Color(0xFFC62828),
                onClick = onDelete
            )
        }
    }, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
    })
}

@Composable
fun MessageActionItem(
    icon: ImageVector, label: String, accent: Color, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

// ── TasksBottomSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksBottomSheet(onDismiss: () -> Unit) {
    val tasks = MessageActionsState.tasks

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tarefas criadas",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(color = WtcBluePale, shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "${tasks.size}",
                        fontSize = 11.sp,
                        color = WtcBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WtcBluePale), contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TaskAlt,
                                null,
                                tint = WtcBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Nenhuma tarefa criada ainda.",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Deslize uma mensagem para criar.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                tasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(WtcBluePale), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TaskAlt,
                                    null,
                                    tint = WtcBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.text,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    fontWeight = FontWeight.Medium
                                )
                                task.createdAt?.let {
                                    Text(
                                        it.take(16).replace("T", " às "),
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { MessageActionsState.tasks.remove(task) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}