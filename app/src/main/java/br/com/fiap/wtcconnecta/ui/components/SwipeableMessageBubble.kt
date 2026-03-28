package br.com.fiap.wtcconnecta.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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

// ── Modelo de tarefa local (sem backend) ──────────────────────────────────────
data class MessageTask(
    val messageId: String,
    val text: String,
    val createdAt: String? = null
)

// ── Estado global de mensagens importantes e tarefas locais ───────────────────
object MessageActionsState {
    val importantMessages = mutableStateListOf<String>()
    val tasks             = mutableStateListOf<MessageTask>()

    fun toggleImportant(messageId: String) {
        if (importantMessages.contains(messageId)) importantMessages.remove(messageId)
        else importantMessages.add(messageId)
    }

    fun addTask(message: Message) {
        if (tasks.none { it.messageId == message.id }) {
            tasks.add(MessageTask(message.id, message.content, message.createdAt))
        }
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
        targetValue = if (abs(offsetX) > 60) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else Color.Transparent,
        label = "swipe_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > 60) showMenu = true
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-120f, 120f)
                    }
                )
            }
    ) {
        if (abs(animatedOffset) > 20) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center),
                contentAlignment = if (animatedOffset > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.SwipeRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (abs(animatedOffset) / 120f).coerceIn(0f, 1f)
                    ),
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
                    Icon(Icons.Default.Star, contentDescription = "Importante",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp))
                }
                content()
                if (isImportant && isFromCurrentUser) {
                    Icon(Icons.Default.Star, contentDescription = "Importante",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
            }
        }
    }

    if (showMenu) {
        MessageActionsMenu(
            message      = message,
            isImportant  = isImportant,
            showTask     = onCreateTask != null,
            showEdit     = onEdit != null,
            showDelete   = onDelete != null,
            onDismiss    = { showMenu = false },
            onImportant  = {
                MessageActionsState.toggleImportant(message.id)
                showMenu = false
            },
            onCreateTask = {
                showMenu = false
                if (onCreateTask != null) showTaskForm = true
                else MessageActionsState.addTask(message)
            },
            onEdit = {
                onEdit?.invoke()
                showMenu = false
            },
            onDelete = {
                onDelete?.invoke()
                showMenu = false
            },
            onReply = {
                onReply(message)
                showMenu = false
            },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("mensagem", message.content))
                showMenu = false
            }
        )
    }

    // Formulário completo de tarefa — só aparece quando operador passa onCreateTask
    if (showTaskForm && onCreateTask != null) {
        TaskFormDialog(
            clientId   = clientId,
            clientName = clientName,
            messageRef = message.content,
            onDismiss  = { showTaskForm = false },
            onConfirm  = { request ->
                onCreateTask(request)
                showTaskForm = false
            }
        )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ações da mensagem",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MessageActionItem(
                    icon  = if (isImportant) Icons.Default.Star else Icons.Default.StarBorder,
                    label = if (isImportant) "Remover destaque" else "Marcar como importante",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onImportant
                )
                if (showEdit) {
                    MessageActionItem(
                        icon  = Icons.Default.Edit,
                        label = "Editar mensagem",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onEdit
                    )
                }
                if (showDelete) {
                    MessageActionItem(
                        icon  = Icons.Default.Delete,
                        label = "Excluir mensagem",
                        color = MaterialTheme.colorScheme.error,
                        onClick = onDelete
                    )
                }
                if (showTask) {
                    MessageActionItem(
                        icon  = Icons.Default.TaskAlt,
                        label = "Criar tarefa",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onCreateTask
                    )
                }
                MessageActionItem(
                    icon  = Icons.Default.Reply,
                    label = "Responder",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onReply
                )
                MessageActionItem(
                    icon  = Icons.Default.ContentCopy,
                    label = "Copiar texto",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onCopy
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun MessageActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

// ── TasksBottomSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksBottomSheet(onDismiss: () -> Unit) {
    val tasks = MessageActionsState.tasks

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Tarefas criadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhuma tarefa criada ainda.\nDeslize uma mensagem para criar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                tasks.forEach { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                task.createdAt?.let {
                                    Text(it.take(16).replace("T", " às "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 10.sp)
                                }
                            }
                            IconButton(
                                onClick = { MessageActionsState.tasks.remove(task) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remover",
                                    tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}