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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.Note
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.ui.components.ImageMessageBubble
import br.com.fiap.wtcconnecta.ui.components.ImagePickerButton
import br.com.fiap.wtcconnecta.ui.components.ImagePreviewBar
import br.com.fiap.wtcconnecta.ui.components.MessageActionsState
import br.com.fiap.wtcconnecta.ui.components.SwipeableMessageBubble
import br.com.fiap.wtcconnecta.ui.components.TasksBottomSheet
import br.com.fiap.wtcconnecta.viewmodel.ClientDetailViewModel
import br.com.fiap.wtcconnecta.viewmodel.ImageUploadViewModel
import br.com.fiap.wtcconnecta.viewmodel.TaskViewModel
import br.com.fiap.wtcconnecta.ui.components.MessageStatusIcon
import br.com.fiap.wtcconnecta.data.model.MessageStatus


private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

// Regex para detectar mensagens com imagem
private val IMG_REGEX = Regex("""\[img:(images/[^\]]+)]""")

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
    val uiState          by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var noteBeingEdited  by remember { mutableStateOf<Note?>(null) }
    var noteToDelete     by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(clientId) {
        viewModel.loadAllDetails(clientId)
        viewModel.startPolling(clientId)
    }

    Scaffold(
        containerColor = Color(0xFFF5FAFD),
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.client?.name ?: "Detalhes do Cliente",
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WtcBlue)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.client != null -> {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor   = Color.White,
                        contentColor     = WtcBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color    = WtcBlue
                            )
                        }
                    ) {
                        listOf(
                            "Anotações" to Icons.Default.Notes,
                            "Chat"      to Icons.Default.Chat,
                            "Perfil"    to Icons.Default.Person
                        ).forEachIndexed { index, (label, icon) ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick  = { selectedTabIndex = index },
                                text     = { Text(label, fontSize = 12.sp,
                                    color = if (selectedTabIndex == index) WtcBlue else TextMuted) },
                                icon     = { Icon(icon, contentDescription = null,
                                    tint = if (selectedTabIndex == index) WtcBlue else TextMuted,
                                    modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> NotesSection(notes = uiState.notes, clientId = clientId,
                            viewModel = viewModel, onDeleteRequest = { noteToDelete = it },
                            onStartEdit = { noteBeingEdited = it })
                        1 -> ChatSection(
                            messages          = uiState.messages,
                            clientId          = clientId,
                            clientName        = uiState.client?.name ?: "",
                            currentOperatorId = currentOperatorId,
                            viewModel         = viewModel,
                            taskViewModel     = taskViewModel,
                            getSenderName     = { viewModel.getSenderName(it) }
                        )
                        2 -> ClientProfileSection(
                            divisions         = uiState.divisions,
                            groups            = uiState.groups,
                            currentDivisionId = uiState.client?.divisionId,
                            currentGroupId    = uiState.client?.groupId,
                            clientName        = uiState.client?.name ?: "",
                            clientEmail       = uiState.client?.email ?: "",
                            onSave            = { divisionId, groupId ->
                                viewModel.updateClientProfile(divisionId, groupId) }
                        )
                    }
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Erro: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    noteBeingEdited?.let { note ->
        EditNoteDialog(note = note, onDismiss = { noteBeingEdited = null },
            onConfirm = { viewModel.updateNote(note, it, clientId); noteBeingEdited = null })
    }
    noteToDelete?.let { note ->
        DeleteConfirmationDialog(onDismiss = { noteToDelete = null },
            onConfirm = { viewModel.deleteNote(note.id, clientId); noteToDelete = null })
    }
}

// ── Aba Chat ──────────────────────────────────────────────────────────────────

@Composable
fun ChatSection(
    messages: List<Message>, clientId: String, clientName: String,
    currentOperatorId: String, viewModel: ClientDetailViewModel,
    taskViewModel: TaskViewModel, navController: NavController? = null,
    getSenderName: (String) -> String
) {
    var messageText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var replyTo     by remember { mutableStateOf<Message?>(null) }
    var showTasks   by remember { mutableStateOf(false) }
    val taskCount   = MessageActionsState.tasks.size

    // ── Estado de upload de imagem ────────────────────────────────────────────
    val uploadViewModel: ImageUploadViewModel = viewModel()
    var pendingImageUri by remember { mutableStateOf<String?>(null) }
    var pendingImageKey by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F6FA))) {

        if (taskCount > 0) {
            TextButton(
                onClick = { showTasks = true },
                modifier = Modifier.align(Alignment.End).padding(end = 8.dp)
            ) {
                Icon(Icons.Default.TaskAlt, contentDescription = null,
                    modifier = Modifier.size(16.dp), tint = WtcBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$taskCount tarefa(s)", fontSize = 12.sp, color = WtcBlue)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                val isOwn    = viewModel.isFromOperator(message.senderId)
                val imgMatch = IMG_REGEX.find(message.displayContent)

                if (imgMatch != null) {
                    // ── Mensagem com imagem ───────────────────────────────────
                    val objectKey = imgMatch.groupValues[1]
                    val caption   = message.displayContent.replace(imgMatch.value, "").trim()

                    var imageUrl by remember(objectKey) { mutableStateOf("") }
                    LaunchedEffect(objectKey) {
                        try {
                            val resp = RetrofitClient.instance.getPresignedUrl(objectKey)
                            if (resp.isSuccessful) imageUrl = resp.body()?.url ?: ""
                        } catch (_: Exception) {}
                    }

                    ImageMessageBubble(
                        imageUrl          = imageUrl,
                        caption           = caption.ifBlank { null },
                        isFromCurrentUser = isOwn
                    )
                } else {
                    // ── Mensagem de texto normal ──────────────────────────────
                    SwipeableMessageBubble(
                        message           = message,
                        isFromCurrentUser = isOwn,
                        senderName        = getSenderName(message.senderId),
                        clientId          = clientId,
                        clientName        = clientName,
                        onReply           = { replyTo = it },
                        onCreateTask      = { request: TaskRequest -> taskViewModel.createTask(request) }
                    ) {
                        MessageBubble(
                            message           = message,
                            isFromCurrentUser = isOwn,
                            senderName        = getSenderName(message.senderId),
                            isImportant       = MessageActionsState.isImportant(message.id)
                        )
                    }
                }
            }
        }

        // ── Quote de reply ────────────────────────────────────────────────────
        replyTo?.let { reply ->
            Surface(modifier = Modifier.fillMaxWidth(), color = WtcBluePale) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
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

        // ── Preview de imagem selecionada ─────────────────────────────────────
        pendingImageUri?.let { uri ->
            ImagePreviewBar(
                imageUrl = uri,
                onCancel = {
                    pendingImageUri = null
                    pendingImageKey = null
                    uploadViewModel.reset()
                }
            )
        }

        // ── Sugestões de comandos ─────────────────────────────────────────────
        AnimatedVisibility(suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { command ->
                    AssistChip(
                        onClick = { messageText = command; suggestions = emptyList() },
                        label   = { Text(command, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null,
                            modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = WtcBluePale, labelColor = WtcBlue)
                    )
                }
            }
        }

        // ── Barra de input ────────────────────────────────────────────────────
        Surface(color = Color.White, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão de imagem
                ImagePickerButton(
                    uploadViewModel = uploadViewModel,
                    onImageReady = { url, key ->
                        pendingImageUri = url
                        pendingImageKey = key
                    }
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it; suggestions = viewModel.getCommandSuggestions(it) },
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    placeholder = { Text("Mensagem ou / para comandos...",
                        color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = WtcBlue,
                        unfocusedBorderColor    = WtcBlueHint,
                        focusedContainerColor   = Color(0xFFF0F6FA),
                        unfocusedContainerColor = Color(0xFFF0F6FA)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Botão enviar
                FilledIconButton(
                    onClick = {
                        val finalText = buildString {
                            if (!pendingImageKey.isNullOrBlank())
                                append("[img:$pendingImageKey] ")
                            append(messageText)
                        }
                        if (finalText.isNotBlank()) {
                            viewModel.sendMessage(finalText, clientId, currentOperatorId)
                            messageText     = ""
                            suggestions     = emptyList()
                            pendingImageUri = null
                            pendingImageKey = null
                            uploadViewModel.reset()
                        }
                    },
                    enabled = messageText.isNotBlank() || pendingImageKey != null,
                    shape = RoundedCornerShape(50),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = WtcBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar",
                        tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showTasks) TasksBottomSheet(onDismiss = { showTasks = false })
}

// ── Aba Perfil ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileSection(
    divisions: List<Division>, groups: List<Group>,
    currentDivisionId: String?, currentGroupId: String?,
    clientName: String, clientEmail: String,
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(WtcBluePale), contentAlignment = Alignment.Center) {
                        Text(clientName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WtcBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(clientName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(clientEmail, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
        item {
            Text("Divisão", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
            ExposedDropdownMenuBox(expanded = divisionExpanded,
                onExpandedChange = { divisionExpanded = !divisionExpanded }) {
                OutlinedTextField(
                    value = selectedDivision?.name ?: "Selecione uma divisão...",
                    onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(divisionExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint)
                )
                ExposedDropdownMenu(expanded = divisionExpanded,
                    onDismissRequest = { divisionExpanded = false }) {
                    divisions.forEach { div ->
                        DropdownMenuItem(text = { Text(div.name) }, onClick = {
                            selectedDivision = div; selectedGroup = null
                            divisionExpanded = false; saved = false
                        })
                    }
                }
            }
        }
        item {
            Text("Grupo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
            ExposedDropdownMenuBox(expanded = groupExpanded,
                onExpandedChange = { if (selectedDivision != null) groupExpanded = !groupExpanded }) {
                OutlinedTextField(
                    value = selectedGroup?.name
                        ?: if (selectedDivision == null) "Selecione uma divisão primeiro"
                        else "Selecione um grupo...",
                    onValueChange = {}, readOnly = true, enabled = selectedDivision != null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint)
                )
                ExposedDropdownMenu(expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false }) {
                    if (filteredGroups.isEmpty()) {
                        DropdownMenuItem(text = { Text("Nenhum grupo nesta divisão") },
                            onClick = { groupExpanded = false }, enabled = false)
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
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = WtcBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Perfil atualizado!", color = WtcBlue, fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onClick = {
                    val divId = selectedDivision?.id ?: return@Button
                    val grpId = selectedGroup?.id ?: return@Button
                    onSave(divId, grpId); saved = true
                },
                enabled = selectedDivision != null && selectedGroup != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar alterações", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Aba Anotações ─────────────────────────────────────────────────────────────

@Composable
fun NotesSection(
    notes: List<Note>, clientId: String, viewModel: ClientDetailViewModel,
    onDeleteRequest: (Note) -> Unit, onStartEdit: (Note) -> Unit
) {
    var newNoteText by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5FAFD))) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes) { note ->
                NoteItem(note = note, onEditClick = { onStartEdit(note) },
                    onDeleteClick = { onDeleteRequest(note) })
            }
        }
        Surface(color = Color.White, shadowElevation = 4.dp) {
            OutlinedTextField(
                value = newNoteText, onValueChange = { newNoteText = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Adicionar anotação...", color = TextMuted.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        if (newNoteText.isNotBlank()) {
                            viewModel.addNote(newNoteText, clientId); newNoteText = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = WtcBlue)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
                    cursorColor = WtcBlue,
                    focusedContainerColor   = WtcBluePale.copy(alpha = 0.4f),
                    unfocusedContainerColor = Color(0xFFFAFCFE)
                )
            )
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
fun MessageBubble(
    message: Message, isFromCurrentUser: Boolean,
    senderName: String, isImportant: Boolean = false
) {
    val alignment   = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isFromCurrentUser) WtcBlue else Color.White
    val textColor   = if (isFromCurrentUser) Color.White else TextPrimary

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalAlignment = alignment) {
        if (!isFromCurrentUser) {
            Text(senderName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = WtcBlue, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
        }
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 18.dp,
                bottomStart = if (isFromCurrentUser) 18.dp else 4.dp),
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(message.displayContent, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)

                // ── Hora + status (apenas mensagens próprias) ─────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(message.createdAt), fontSize = 10.sp,
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
@Composable
fun ClientShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = WtcBluePale) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = WtcBlue)
        }
    }
}

@Suppress("NewApi")
private fun formatTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val timePart = when {
            createdAt.contains("T") -> createdAt.substringAfter("T").take(5)
            createdAt.contains(" ") -> createdAt.substringAfter(" ").take(5)
            else                    -> createdAt.take(5)
        }
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            val hour   = parts[0].padStart(2, '0')
            val minute = parts[1].padStart(2, '0')
            "$hour:$minute"
        } else timePart
    } catch (e: Exception) { "" }
}


@Composable
fun EditNoteDialog(note: Note, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var updatedText by remember { mutableStateOf(note.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar anotação", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            OutlinedTextField(value = updatedText, onValueChange = { updatedText = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Conteúdo") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue, cursorColor = WtcBlue))
        },
        confirmButton = {
            Button(onClick = { onConfirm(updatedText) }, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

@Composable
fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir anotação?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text  = { Text("Tem certeza que deseja excluir esta anotação?", color = TextMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

@Composable
fun NoteItem(note: Note, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(note.text, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar",
                        tint = WtcBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}