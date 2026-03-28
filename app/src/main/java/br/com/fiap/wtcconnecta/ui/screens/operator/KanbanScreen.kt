package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Task
import br.com.fiap.wtcconnecta.data.model.TaskCategory
import br.com.fiap.wtcconnecta.data.model.TaskPriority
import br.com.fiap.wtcconnecta.data.model.TaskStatus
import br.com.fiap.wtcconnecta.ui.components.TaskFormDialog
import br.com.fiap.wtcconnecta.viewmodel.TaskViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

// Cores semânticas das colunas Kanban
private val ColPending    = Color(0xFFE65100) // laranja
private val ColProgress   = Color(0xFF0B537B) // azul principal
private val ColDone       = Color(0xFF1A7A5E) // verde

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    onBack: () -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var showCreateDialog  by remember { mutableStateOf(false) }
    var taskToDelete      by remember { mutableStateOf<Task?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadTasks() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5FAFD)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Header gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(WtcBlue, WtcBlueSoft)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                                tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Painel de Tarefas", fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Gerencie suas tarefas em andamento",
                                fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                        }
                    }
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nova tarefa",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                else -> Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KanbanColumn(title = "Pendente", status = "PENDING",
                        tasks = viewModel.pendingTasks, accentColor = ColPending,
                        onMove = { task, s -> viewModel.updateStatus(task.id, s) },
                        onDelete = { taskToDelete = it })
                    KanbanColumn(title = "Em andamento", status = "IN_PROGRESS",
                        tasks = viewModel.inProgressTasks, accentColor = ColProgress,
                        onMove = { task, s -> viewModel.updateStatus(task.id, s) },
                        onDelete = { taskToDelete = it })
                    KanbanColumn(title = "Concluída", status = "DONE",
                        tasks = viewModel.doneTasks, accentColor = ColDone,
                        onMove = { task, s -> viewModel.updateStatus(task.id, s) },
                        onDelete = { taskToDelete = it })
                }
            }
        }
    }

    if (showCreateDialog) {
        TaskFormDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { viewModel.createTask(it); showCreateDialog = false }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Remover tarefa?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text  = { Text("\"${task.title}\" será removida permanentemente.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTask(task.id); taskToDelete = null }) {
                    Text("Remover", color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

// ── Coluna Kanban ─────────────────────────────────────────────────────────────

@Composable
fun KanbanColumn(
    title: String, status: String, tasks: List<Task>,
    accentColor: Color,
    onMove: (Task, String) -> Unit,
    onDelete: (Task) -> Unit
) {
    Column(
        modifier = Modifier
            .width(270.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        // Header da coluna
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp))
                    .background(accentColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Surface(color = accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                Text("${tasks.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }

        HorizontalDivider(color = Color(0xFFF0F6FA))
        Spacer(modifier = Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center) {
                Text("Nenhuma tarefa", fontSize = 12.sp, color = TextMuted.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks) { task ->
                    TaskCard(task = task, accentColor = accentColor,
                        onMove = onMove, onDelete = onDelete)
                }
            }
        }
    }
}

// ── Card de tarefa ────────────────────────────────────────────────────────────

@Composable
fun TaskCard(
    task: Task, accentColor: Color,
    onMove: (Task, String) -> Unit,
    onDelete: (Task) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        "HIGH"   -> Color(0xFFC62828)
        "MEDIUM" -> Color(0xFFE65100)
        else     -> Color(0xFF1A7A5E)
    }

    val categoryEmoji = TaskCategory.entries.firstOrNull { it.name == task.category }?.emoji ?: "📌"
    val priorityEmoji = TaskPriority.entries.firstOrNull { it.name == task.priority }?.emoji ?: "🟡"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Barra de cor + título + menu
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.width(3.dp).height(36.dp)
                        .clip(RoundedCornerShape(2.dp)).background(accentColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(task.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null,
                            modifier = Modifier.size(15.dp), tint = TextMuted)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        TaskStatus.entries.filter { it.name != task.status }.forEach { s ->
                            DropdownMenuItem(text = { Text("→ ${s.label}", fontSize = 13.sp) },
                                onClick = { onMove(task, s.name); showMenu = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Remover", color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp) },
                            onClick = { onDelete(task); showMenu = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cliente
            if (task.clientName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = WtcBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(task.clientName, fontSize = 11.sp, color = WtcBlue,
                        fontWeight = FontWeight.Medium)
                }
            }

            // Mensagem de origem
            if (task.messageRef.isNotBlank()) {
                Surface(color = WtcBluePale, shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text(
                        "\"${task.messageRef.take(60)}${if (task.messageRef.length > 60) "..." else ""}\"",
                        fontSize = 10.sp, color = TextMuted,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tags categoria + prioridade
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(color = WtcBluePale, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "$categoryEmoji ${TaskCategory.entries.firstOrNull { it.name == task.category }?.label ?: task.category}",
                        fontSize = 10.sp, color = WtcBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Surface(color = priorityColor.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "$priorityEmoji ${TaskPriority.entries.firstOrNull { it.name == task.priority }?.label ?: task.priority}",
                        fontSize = 10.sp, color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            // Prazo
            if (task.dueDate.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null,
                        tint = TextMuted, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(task.dueDate, fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}