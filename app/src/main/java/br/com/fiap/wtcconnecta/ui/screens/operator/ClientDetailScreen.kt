package br.com.fiap.wtcconnecta.ui.screens.operator

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.ui.screens.client.CampaignExpressCard
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.Note
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.ui.components.ImageMessageBubble
import br.com.fiap.wtcconnecta.ui.components.ImagePickerButton
import br.com.fiap.wtcconnecta.ui.components.ImagePreviewBar
import br.com.fiap.wtcconnecta.ui.components.MessageActionsState
import br.com.fiap.wtcconnecta.ui.components.PdfMessageBubble
import br.com.fiap.wtcconnecta.ui.components.SwipeableMessageBubble
import br.com.fiap.wtcconnecta.ui.components.TasksBottomSheet
import br.com.fiap.wtcconnecta.viewmodel.ClientDetailViewModel
import br.com.fiap.wtcconnecta.viewmodel.ImageUploadViewModel
import br.com.fiap.wtcconnecta.viewmodel.TaskViewModel
import coil.compose.AsyncImage

private val WtcBlue = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted = Color(0xFF6E90A0)

private val IMG_REGEX = Regex("""\[img:(images/[^\]]+)]""")
private val PDF_REGEX = Regex("""\[pdf:(images/[^\]]+)]""")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var selectedTabIndex by remember { mutableIntStateOf(1) }
    var noteBeingEdited by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(clientId) {
        viewModel.loadAllDetails(clientId)
        viewModel.startPolling(clientId)
    }

    Scaffold(
        containerColor = Color(0xFFF0F6FA), topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = 210.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = uiState.clientAvatarUrl
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar de ${uiState.client?.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                (uiState.client?.name ?: "?").firstOrNull()?.uppercase() ?: "?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            uiState.client?.name ?: "Detalhes do Cliente",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp,
                            maxLines = 1
                        )
                        Text(
                            uiState.client?.email ?: "",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
            }
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }

                uiState.client != null -> {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.White,
                        contentColor = WtcBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = WtcBlue
                            )
                        }) {
                        listOf(
                            "Anotações" to Icons.Default.Notes,
                            "Chat" to Icons.Default.Chat,
                            "Perfil" to Icons.Default.Person,
                            "Campanhas" to Icons.Default.Campaign
                        ).forEachIndexed { index, (label, icon) ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        color = if (selectedTabIndex == index) WtcBlue else TextMuted
                                    )
                                },
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (selectedTabIndex == index) WtcBlue else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                })
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> NotesSection(
                            notes = uiState.notes,
                            clientId = clientId,
                            viewModel = viewModel,
                            onDeleteRequest = { noteToDelete = it },
                            onStartEdit = { noteBeingEdited = it })

                        1 -> ChatSection(
                            messages = uiState.messages,
                            clientId = clientId,
                            clientName = uiState.client?.name ?: "",
                            currentOperatorId = currentOperatorId,
                            viewModel = viewModel,
                            taskViewModel = taskViewModel,
                            getSenderName = { viewModel.getSenderName(it) },
                            onCloseAttendance = { viewModel.closeAttendance(clientId) })

                        2 -> ClientProfileSection(
                            divisions = uiState.divisions,
                            groups = uiState.groups,
                            currentDivisionId = uiState.client?.divisionId,
                            currentGroupId = uiState.client?.groupId,
                            currentTags = uiState.client?.tags.orEmpty(),
                            clientName = uiState.client?.name ?: "",
                            clientEmail = uiState.client?.email ?: "",
                            onSave = { divisionId, groupId, tags ->
                                viewModel.updateClientProfile(divisionId, groupId, tags)
                            })

                        3 -> ClientCampaignsSection(campaigns = uiState.campaigns)
                    }
                }

                uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Erro: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    noteBeingEdited?.let { note ->
        EditNoteDialog(
            note = note,
            onDismiss = { noteBeingEdited = null },
            onConfirm = { viewModel.updateNote(note, it, clientId); noteBeingEdited = null })
    }
    noteToDelete?.let { note ->
        DeleteConfirmationDialog(
            onDismiss = { noteToDelete = null },
            onConfirm = { viewModel.deleteNote(note.id, clientId); noteToDelete = null })
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
    currentTags: List<String> = emptyList(),
    clientName: String,
    clientEmail: String,
    onSave: (divisionId: String, groupId: String, tags: List<String>) -> Unit
) {
    var selectedDivision by remember { mutableStateOf(divisions.find { it.id == currentDivisionId }) }
    var selectedGroup by remember { mutableStateOf(groups.find { it.id == currentGroupId }) }
    var divisionExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf(currentTags.toMutableList()) }
    var tagInput by remember { mutableStateOf("") }

    val filteredGroups = remember(selectedDivision) {
        groups.filter { it.divisionId == selectedDivision?.id }
    }
    LaunchedEffect(selectedDivision) {
        if (selectedGroup?.divisionId != selectedDivision?.id) selectedGroup = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(WtcBlueHint), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            clientName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = WtcBlueDark
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            clientName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(clientEmail, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        item {
            Text(
                "Divisão",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            ExposedDropdownMenuBox(
                expanded = divisionExpanded,
                onExpandedChange = { divisionExpanded = !divisionExpanded }) {
                OutlinedTextField(
                    value = selectedDivision?.name ?: "Selecione uma divisão...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(divisionExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint
                    )
                )
                ExposedDropdownMenu(
                    expanded = divisionExpanded, onDismissRequest = { divisionExpanded = false }) {
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
            Text(
                "Grupo",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            ExposedDropdownMenuBox(
                expanded = groupExpanded, onExpandedChange = {
                    if (selectedDivision != null) groupExpanded = !groupExpanded
                }) {
                OutlinedTextField(
                    value = selectedGroup?.name
                        ?: if (selectedDivision == null) "Selecione uma divisão primeiro"
                        else "Selecione um grupo...",
                    onValueChange = {},
                    readOnly = true,
                    enabled = selectedDivision != null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint
                    )
                )
                ExposedDropdownMenu(
                    expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                    if (filteredGroups.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Nenhum grupo nesta divisão") },
                            onClick = { groupExpanded = false },
                            enabled = false
                        )
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
            Text(
                "Tags",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(color = WtcBlue, shape = RoundedCornerShape(20.dp)) {
                            Row(
                                modifier = Modifier.padding(
                                    start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    tag,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = {
                                        tags = (tags - tag).toMutableList(); saved = false
                                    }, modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remover tag",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it.lowercase().replace(" ", "_") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nova tag...", color = TextMuted.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue,
                        unfocusedBorderColor = WtcBlueHint,
                        cursorColor = WtcBlue
                    )
                )
                IconButton(
                    onClick = {
                        val t = tagInput.trim()
                        if (t.isNotBlank() && !tags.contains(t)) {
                            tags = (tags + t).toMutableList(); saved = false
                        }
                        tagInput = ""
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(WtcBlue, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Add, contentDescription = "Adicionar tag", tint = Color.White
                    )
                }
            }
        }

        item {
            if (saved) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WtcBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Perfil atualizado!", color = WtcBlue, fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onClick = {
                    val divId = selectedDivision?.id ?: currentDivisionId ?: ""
                    val grpId = selectedGroup?.id ?: currentGroupId ?: ""
                    onSave(divId, grpId, tags.toList()); saved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
    notes: List<Note>,
    clientId: String,
    viewModel: ClientDetailViewModel,
    onDeleteRequest: (Note) -> Unit,
    onStartEdit: (Note) -> Unit
) {
    var newNoteText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F6FA))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes) { note ->
                NoteItem(
                    note = note,
                    onEditClick = { onStartEdit(note) },
                    onDeleteClick = { onDeleteRequest(note) })
            }
        }
        Surface(color = Color.White, shadowElevation = 4.dp) {
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = {
                    Text(
                        "Adicionar anotação...", color = TextMuted.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        if (newNoteText.isNotBlank()) {
                            viewModel.addNote(newNoteText, clientId); newNoteText = ""
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = WtcBlue
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue,
                    unfocusedBorderColor = WtcBlueHint,
                    cursorColor = WtcBlue,
                    focusedContainerColor = WtcBluePale.copy(alpha = 0.4f),
                    unfocusedContainerColor = Color(0xFFFAFCFE)
                )
            )
        }
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
    getSenderName: (String) -> String,
    onCloseAttendance: () -> Unit = {}
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    var showTasks by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    val taskCount = MessageActionsState.tasks.size

    val uploadViewModel: ImageUploadViewModel = viewModel()
    val uploadState by uploadViewModel.uiState.collectAsState()
    var pendingFileUri by remember { mutableStateOf<String?>(null) }
    var pendingFileKey by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var pendingIsPdf by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F6FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (taskCount > 0) {
                TextButton(onClick = { showTasks = true }) {
                    Icon(
                        Icons.Default.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WtcBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$taskCount tarefa(s)", fontSize = 12.sp, color = WtcBlue)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            TextButton(
                onClick = { showCloseDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Encerrar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                val isOwn = viewModel.isFromOperator(message.senderId)
                val imgMatch = IMG_REGEX.find(message.displayContent)
                val pdfMatch = PDF_REGEX.find(message.displayContent)

                when {
                    // ── Imagem ────────────────────────────────────────────────
                    imgMatch != null -> {
                        val objectKey = imgMatch.groupValues[1]
                        val caption = message.displayContent.replace(imgMatch.value, "").trim()
                        var imageUrl by remember(objectKey) { mutableStateOf("") }
                        LaunchedEffect(objectKey) {
                            try {
                                val resp = RetrofitClient.instance.getPresignedUrl(objectKey)
                                if (resp.isSuccessful) imageUrl = resp.body()?.url ?: ""
                            } catch (_: Exception) {
                            }
                        }
                        SwipeableMessageBubble(
                            message = message,
                            isFromCurrentUser = isOwn,
                            senderName = getSenderName(message.senderId),
                            clientId = clientId,
                            clientName = clientName,
                            onReply = { replyTo = it },
                            onCreateTask = { request: TaskRequest ->
                                taskViewModel.createTask(
                                    request
                                )
                            },
                            onDelete = if (isOwn) ({ messageToDelete = message }) else null
                        ) {
                            ImageMessageBubble(
                                imageUrl = imageUrl,
                                caption = caption.ifBlank { null },
                                isFromCurrentUser = isOwn
                            )
                        }
                    }
                    // ── PDF ───────────────────────────────────────────────────
                    pdfMatch != null -> {
                        val objectKey = pdfMatch.groupValues[1]
                        val fileName = message.displayContent.replace(pdfMatch.value, "").trim()
                            .ifBlank { objectKey.substringAfterLast("/") }
                        var pdfUrl by remember(objectKey) { mutableStateOf("") }
                        LaunchedEffect(objectKey) {
                            try {
                                val resp = RetrofitClient.instance.getPresignedUrl(objectKey)
                                if (resp.isSuccessful) pdfUrl = resp.body()?.url ?: ""
                            } catch (_: Exception) {
                            }
                        }
                        SwipeableMessageBubble(
                            message = message,
                            isFromCurrentUser = isOwn,
                            senderName = getSenderName(message.senderId),
                            clientId = clientId,
                            clientName = clientName,
                            onReply = { replyTo = it },
                            onCreateTask = { request: TaskRequest ->
                                taskViewModel.createTask(
                                    request
                                )
                            },
                            onDelete = if (isOwn) ({ messageToDelete = message }) else null
                        ) {
                            PdfMessageBubble(
                                fileName = fileName, isFromCurrentUser = isOwn, onOpen = {
                                    if (pdfUrl.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    }
                                })
                        }
                    }
                    // ── Texto normal ──────────────────────────────────────────
                    else -> {
                        SwipeableMessageBubble(
                            message = message,
                            isFromCurrentUser = isOwn,
                            senderName = getSenderName(message.senderId),
                            clientId = clientId,
                            clientName = clientName,
                            onReply = { replyTo = it },
                            onCreateTask = { request: TaskRequest ->
                                taskViewModel.createTask(
                                    request
                                )
                            }) {
                            MessageBubble(
                                message = message,
                                isFromCurrentUser = isOwn,
                                senderName = getSenderName(message.senderId),
                                isImportant = MessageActionsState.isImportant(message.id)
                            )
                        }
                    }
                }
            }
        }

        replyTo?.let { reply ->
            Surface(modifier = Modifier.fillMaxWidth(), color = WtcBluePale) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp),
                        color = WtcBlue,
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Respondendo",
                            fontSize = 11.sp,
                            color = WtcBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            reply.displayContent, fontSize = 12.sp, color = TextMuted, maxLines = 1
                        )
                    }
                    IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        AnimatedVisibility(suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { command ->
                    AssistChip(
                        onClick = { messageText = command; suggestions = emptyList() },
                        label = { Text(command, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = WtcBluePale, labelColor = WtcBlue
                        )
                    )
                }
            }
        }

        // ── Preview do arquivo pendente ───────────────────────────────────────
        pendingFileUri?.let { uri ->
            ImagePreviewBar(
                imageUrl = uri, isPdf = pendingIsPdf, fileName = pendingFileName, onCancel = {
                    pendingFileUri = null
                    pendingFileKey = null
                    pendingFileName = null
                    pendingIsPdf = false
                    uploadViewModel.reset()
                })
        }

        Surface(color = Color.White, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ── Botão anexar arquivo ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(WtcBluePale), contentAlignment = Alignment.Center
                ) {
                    ImagePickerButton(
                        uploadViewModel = uploadViewModel, onImageReady = { url, key ->
                            pendingFileUri = url
                            pendingFileKey = key
                            pendingIsPdf = key.endsWith(".pdf", ignoreCase = true)
                            pendingFileName = uploadState.fileName
                        })
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = messageText, onValueChange = {
                        messageText = it; suggestions = viewModel.getCommandSuggestions(it)
                    }, modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp), placeholder = {
                        Text(
                            "Mensagem ou / para comandos...",
                            color = TextMuted.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }, shape = RoundedCornerShape(22.dp), colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue,
                        unfocusedBorderColor = WtcBlueHint,
                        focusedContainerColor = Color(0xFFF0F6FA),
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
                            viewModel.sendMessage(finalText, clientId, currentOperatorId)
                            messageText = ""
                            suggestions = emptyList()
                            pendingFileUri = null
                            pendingFileKey = null
                            pendingFileName = null
                            pendingIsPdf = false
                            uploadViewModel.reset()
                        }
                    },
                    enabled = messageText.isNotBlank() || pendingFileKey != null,
                    shape = RoundedCornerShape(50),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = WtcBlue)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showTasks) TasksBottomSheet(onDismiss = { showTasks = false })

    if (showCloseDialog) {
        AlertDialog(onDismissRequest = { showCloseDialog = false }, title = {
            Text(
                "Encerrar atendimento?", fontWeight = FontWeight.Bold, color = TextPrimary
            )
        }, text = {
            Text(
                "O cliente receberá uma mensagem de encerramento e o atendimento " + "voltará para a fila caso entre em contato novamente.",
                color = TextMuted,
                fontSize = 13.sp
            )
        }, confirmButton = {
            Button(
                onClick = { showCloseDialog = false; onCloseAttendance() },
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Encerrar", fontWeight = FontWeight.SemiBold)
            }
        }, dismissButton = {
            TextButton(onClick = { showCloseDialog = false }) {
                Text("Cancelar", color = TextMuted)
            }
        })
    }

    // ── Dialog excluir mensagem ───────────────────────────────────────────────
    messageToDelete?.let { msg ->
        AlertDialog(onDismissRequest = { messageToDelete = null }, title = {
            Text(
                "Excluir mensagem?", fontWeight = FontWeight.Bold, color = TextPrimary
            )
        }, text = {
            Column {
                Text("Esta ação não pode ser desfeita.", color = TextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = WtcBluePale,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        msg.displayContent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        maxLines = 3,
                        color = TextPrimary
                    )
                }
            }
        }, confirmButton = {
            TextButton(onClick = {
                viewModel.deleteMessage(msg.id, clientId)
                messageToDelete = null
            }) {
                Text(
                    "Excluir",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }, dismissButton = {
            TextButton(onClick = { messageToDelete = null }) {
                Text("Cancelar", color = TextMuted)
            }
        })
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
fun MessageBubble(
    message: Message, isFromCurrentUser: Boolean, senderName: String, isImportant: Boolean = false
) {
    val alignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isFromCurrentUser) WtcBlue else Color.White
    val textColor = if (isFromCurrentUser) Color.White else TextPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp), horizontalAlignment = alignment
    ) {
        if (!isFromCurrentUser) {
            Text(
                senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WtcBlue,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 18.dp,
                bottomStart = if (isFromCurrentUser) 18.dp else 4.dp
            ),
            shadowElevation = if (isFromCurrentUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(
                    message.displayContent, fontSize = 14.sp, color = textColor, lineHeight = 20.sp
                )
                Text(
                    formatTime(message.createdAt),
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.55f),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ClientShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = WtcBluePale) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = WtcBlue)
        }
    }
}

private fun formatTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        // createdAt: "2026-05-01T20:24:21.643..." → pega HH:mm a partir do índice 11
        createdAt.drop(11).take(5)
    } catch (e: Exception) {
        ""
    }
}

private fun formatCampaignDate(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val date = createdAt.take(10)
        val time = createdAt.drop(11).take(5)
        val parts = date.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]} às $time"
        else "$date às $time"
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun EditNoteDialog(note: Note, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var updatedText by remember { mutableStateOf(note.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar anotação", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = updatedText,
                onValueChange = { updatedText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Conteúdo") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue, cursorColor = WtcBlue
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(updatedText) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } })
}

@Composable
fun DeleteConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir anotação?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Text("Tem certeza que deseja excluir esta anotação?", color = TextMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Excluir",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } })
}

@Composable
fun NoteItem(note: Note, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(note.text, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = WtcBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Aba Campanhas ─────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ClientCampaignsSection(campaigns: List<Message>) {
    var selectedCampaign by remember { mutableStateOf<Message?>(null) }

    if (campaigns.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WtcBluePale), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Campaign,
                    contentDescription = null,
                    tint = WtcBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Nenhuma campanha recebida",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                "Este cliente ainda não recebeu campanhas.",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                "CAMPANHAS — ${campaigns.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(campaigns) { campaign ->
            Card(
                onClick = { selectedCampaign = campaign },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
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
                            .clip(RoundedCornerShape(13.dp))
                            .background(WtcBluePale), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = WtcBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(campaign.title?.ifBlank { "Campanha WTC" } ?: "Campanha WTC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(campaign.body?.ifBlank { "" } ?: "",
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp))
                        Text(
                            formatCampaignDate(campaign.createdAt),
                            fontSize = 11.sp,
                            color = TextMuted.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(WtcBlueHint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = WtcBlueDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    selectedCampaign?.let { campaign ->
        ModalBottomSheet(
            onDismissRequest = { selectedCampaign = null },
            containerColor = Color(0xFFF5FAFD),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Detalhes da campanha",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                HorizontalDivider(color = Color(0xFFF0F6FA))
                CampaignExpressCard(campaign = campaign)
            }
        }
    }
}