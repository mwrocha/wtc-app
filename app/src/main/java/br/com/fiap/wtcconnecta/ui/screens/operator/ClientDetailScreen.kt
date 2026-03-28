package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.Note
import androidx.navigation.NavController
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.ui.navigation.Routes
import br.com.fiap.wtcconnecta.ui.components.MessageActionsState
import br.com.fiap.wtcconnecta.ui.components.SwipeableMessageBubble
import br.com.fiap.wtcconnecta.ui.components.TasksBottomSheet
import br.com.fiap.wtcconnecta.viewmodel.ClientDetailViewModel
import br.com.fiap.wtcconnecta.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onBack: () -> Unit,
    currentOperatorId: String,
    navController: NavController? = null,
    viewModel: ClientDetailViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var noteBeingEdited by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(clientId) {
        viewModel.loadAllDetails(clientId)
        viewModel.startPolling(clientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.client?.name ?: "Detalhes do Cliente",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.client != null -> {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Anotações") },
                            icon = { Icon(Icons.Default.Notes, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Chat") },
                            icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Perfil") },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    }

                    when (selectedTabIndex) {
                        0 -> NotesSection(
                            notes = uiState.notes,
                            clientId = clientId,
                            viewModel = viewModel,
                            onDeleteRequest = { noteToDelete = it },
                            onStartEdit = { noteBeingEdited = it }
                        )
                        1 -> ChatSection(
                            messages = uiState.messages,
                            clientId = clientId,
                            clientName = uiState.client?.name ?: "",
                            currentOperatorId = currentOperatorId,
                            viewModel = viewModel,
                            taskViewModel = taskViewModel,
                            getSenderName = { viewModel.getSenderName(it) }
                        )
                        2 -> ClientProfileSection(
                            divisions = uiState.divisions,
                            groups = uiState.groups,
                            currentDivisionId = uiState.client?.divisionId,
                            currentGroupId = uiState.client?.groupId,
                            clientName = uiState.client?.name ?: "",
                            clientEmail = uiState.client?.email ?: "",
                            onSave = { divisionId, groupId ->
                                viewModel.updateClientProfile(divisionId, groupId)
                            }
                        )
                    }
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(text = "Erro: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    noteBeingEdited?.let { note ->
        EditNoteDialog(
            note = note,
            onDismiss = { noteBeingEdited = null },
            onConfirm = { updatedText ->
                viewModel.updateNote(note, updatedText, clientId)
                noteBeingEdited = null
            }
        )
    }

    noteToDelete?.let { note ->
        DeleteConfirmationDialog(
            onDismiss = { noteToDelete = null },
            onConfirm = {
                viewModel.deleteNote(note.id, clientId)
                noteToDelete = null
            }
        )
    }
}

// ── Aba Perfil ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileSection(
    divisions: List<Division>,
    groups: List<Group>,
    currentDivisionId: String?,
    currentGroupId: String?,
    clientName: String,
    clientEmail: String,
    onSave: (divisionId: String, groupId: String) -> Unit
) {
    var selectedDivision by remember { mutableStateOf(divisions.find { it.id == currentDivisionId }) }
    var selectedGroup    by remember { mutableStateOf(groups.find { it.id == currentGroupId }) }
    var divisionExpanded by remember { mutableStateOf(false) }
    var groupExpanded    by remember { mutableStateOf(false) }
    var saved            by remember { mutableStateOf(false) }

    val filteredGroups = remember(selectedDivision) {
        groups.filter { it.divisionId == selectedDivision?.id }
    }

    LaunchedEffect(selectedDivision) {
        if (selectedGroup?.divisionId != selectedDivision?.id) selectedGroup = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(clientEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Text("Divisão", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = divisionExpanded, onExpandedChange = { divisionExpanded = !divisionExpanded }) {
                OutlinedTextField(
                    value = selectedDivision?.name ?: "Selecione uma divisão...",
                    onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = divisionExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = divisionExpanded, onDismissRequest = { divisionExpanded = false }) {
                    divisions.forEach { division ->
                        DropdownMenuItem(text = { Text(division.name) }, onClick = {
                            selectedDivision = division; selectedGroup = null; divisionExpanded = false; saved = false
                        })
                    }
                }
            }
        }

        item {
            Text("Grupo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { if (selectedDivision != null) groupExpanded = !groupExpanded }) {
                OutlinedTextField(
                    value = selectedGroup?.name ?: if (selectedDivision == null) "Selecione uma divisão primeiro" else "Selecione um grupo...",
                    onValueChange = {}, readOnly = true, enabled = selectedDivision != null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                    if (filteredGroups.isEmpty()) {
                        DropdownMenuItem(text = { Text("Nenhum grupo nesta divisão") }, onClick = { groupExpanded = false }, enabled = false)
                    } else {
                        filteredGroups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = {
                                selectedGroup = group; groupExpanded = false; saved = false
                            })
                        }
                    }
                }
            }
        }

        item {
            if (saved) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Perfil atualizado!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onClick = { val divId = selectedDivision?.id ?: return@Button; val grpId = selectedGroup?.id ?: return@Button; onSave(divId, grpId); saved = true },
                enabled = selectedDivision != null && selectedGroup != null,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar alterações")
            }
        }
    }
}

// ── Aba Anotações ─────────────────────────────────────────────────────────────

@Composable
fun NotesSection(notes: List<Note>, clientId: String, viewModel: ClientDetailViewModel, onDeleteRequest: (Note) -> Unit, onStartEdit: (Note) -> Unit) {
    var newNoteText by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(notes) { note -> NoteItem(note = note, onEditClick = { onStartEdit(note) }, onDeleteClick = { onDeleteRequest(note) }) }
        }
        OutlinedTextField(
            value = newNoteText, onValueChange = { newNoteText = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp), label = { Text("Adicionar anotação...") },
            trailingIcon = {
                IconButton(onClick = { if (newNoteText.isNotBlank()) { viewModel.addNote(newNoteText, clientId); newNoteText = "" } }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Salvar anotação")
                }
            }
        )
    }
}

// ── Aba Chat ──────────────────────────────────────────────────────────────────

@Composable
fun ChatSection(
    messages: List<Message>,
    clientId: String,
    clientName: String,
    currentOperatorId: String,
    viewModel: ClientDetailViewModel,
    taskViewModel: TaskViewModel,
    navController: NavController? = null,
    getSenderName: (String) -> String
) {
    var messageText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var replyTo     by remember { mutableStateOf<Message?>(null) }
    var showTasks   by remember { mutableStateOf(false) }
    val taskCount   = MessageActionsState.tasks.size

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Botão de tarefas locais (MessageActionsState)
        if (taskCount > 0) {
            TextButton(
                onClick = { showTasks = true },
                modifier = Modifier.align(Alignment.End).padding(end = 8.dp)
            ) {
                Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$taskCount tarefa(s)", style = MaterialTheme.typography.labelMedium)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                SwipeableMessageBubble(
                    message           = message,
                    isFromCurrentUser = viewModel.isFromOperator(message.senderId),
                    senderName        = getSenderName(message.senderId),
                    clientId          = clientId,
                    clientName        = clientName,
                    onReply           = { replyTo = it },
                    // Callback que salva tarefa no MongoDB via TaskViewModel
                    onCreateTask      = { request: TaskRequest ->
                        taskViewModel.createTask(request)
                    }
                ) {
                    MessageBubble(
                        message           = message,
                        isFromCurrentUser = viewModel.isFromOperator(message.senderId),
                        senderName        = getSenderName(message.senderId),
                        isImportant       = MessageActionsState.isImportant(message.id)
                    )
                }
            }
        }

        // Reply quote
        replyTo?.let { reply ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.width(3.dp).height(36.dp), color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp)) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Respondendo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(reply.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        AnimatedVisibility(suggestions.isNotEmpty()) {
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { command ->
                    AssistChip(onClick = { messageText = command; suggestions = emptyList() }, label = { Text(command) }, leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null) })
                }
            }
        }

        HorizontalDivider()
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it; suggestions = viewModel.getCommandSuggestions(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Digite sua mensagem ou / para comandos...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Gray)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(messageText, clientId, currentOperatorId)
                    messageText = ""; suggestions = emptyList()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showTasks) {
        TasksBottomSheet(onDismiss = { showTasks = false })
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean, senderName: String, isImportant: Boolean = false) {
    val alignment   = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = alignment) {
        if (!isFromCurrentUser) {
            Text(text = senderName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 2.dp))
        }
        Surface(color = bubbleColor, shape = RoundedCornerShape(16.dp), shadowElevation = 4.dp, modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = 8.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.content, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, textAlign = TextAlign.End),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ClientShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Suppress("NewApi")
private fun formatTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try { createdAt.takeLast(8).substring(0, 5) } catch (e: Exception) { "" }
}

@Composable
fun EditNoteDialog(note: Note, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var updatedText by remember { mutableStateOf(note.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(updatedText) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Editar anotação") },
        text = { OutlinedTextField(value = updatedText, onValueChange = { updatedText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Conteúdo da anotação") }) }
    )
}

@Composable
fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Excluir anotação") },
        text = { Text("Tem certeza que deseja excluir esta anotação?") }
    )
}

@Composable
fun NoteItem(note: Note, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = note.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}