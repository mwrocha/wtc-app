package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.remote.GroupMessageRequest
import br.com.fiap.wtcconnecta.ui.components.ImagePickerButton
import br.com.fiap.wtcconnecta.ui.components.ImagePreviewBar
import br.com.fiap.wtcconnecta.viewmodel.ImageUploadViewModel
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

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    val error: String? = null
)

class GroupListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val groups    = RetrofitClient.instance.getGroups()
                val divisions = RetrofitClient.instance.getDivisions()
                _uiState.update { it.copy(isLoading = false, groups = groups, divisions = divisions) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar grupos.") }
            }
        }
    }

    // Envia mensagem para todos os grupos de uma divisão
    fun sendToDivision(text: String, divisionId: String) {
        val groups = _uiState.value.groups.filter { it.divisionId == divisionId }
        if (groups.isEmpty() || text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            var successCount = 0
            for (group in groups) {
                try {
                    RetrofitClient.instance.sendGroupMessage(
                        GroupMessageRequest(groupId = group.id, body = text)
                    )
                    successCount++
                } catch (_: Exception) {}
            }
            _uiState.update { it.copy(
                isSending   = false,
                sendSuccess = successCount > 0,
                error       = if (successCount == 0) "Erro ao enviar para os grupos." else null
            ) }
        }
    }

    // Envia mensagem para um grupo específico (disparo único)
    fun sendToGroup(text: String, groupId: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                RetrofitClient.instance.sendGroupMessage(
                    GroupMessageRequest(groupId = groupId, body = text)
                )
                _uiState.update { it.copy(isSending = false, sendSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, error = "Erro ao enviar mensagem.") }
            }
        }
    }

    fun clearStatus() { _uiState.update { it.copy(sendSuccess = false, error = null) } }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onBack: () -> Unit,
    onGroupClick: (groupId: String, groupName: String) -> Unit,
    viewModel: GroupListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDivisionDialog by remember { mutableStateOf(false) }
    var selectedDivisionForDispatch by remember { mutableStateOf<Division?>(null) }

    val groupsByDivision = remember(uiState.groups) {
        uiState.groups.groupBy { it.divisionId.ifBlank { "sem_divisao" } }
    }

    // Snackbar de feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.sendSuccess, uiState.error) {
        when {
            uiState.sendSuccess -> {
                snackbarHostState.showSnackbar("Mensagem enviada com sucesso!")
                viewModel.clearStatus()
            }
            uiState.error != null -> {
                snackbarHostState.showSnackbar(uiState.error ?: "Erro")
                viewModel.clearStatus()
            }
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F6FA)
    ) { innerPadding ->
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grupos e Divisões", fontWeight = FontWeight.Bold,
                            color = Color.White, fontSize = 20.sp)
                        Text("Chat por grupo ou disparo por divisão",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    }
                    // Botão de disparo em massa
                    IconButton(
                        onClick  = { showDivisionDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar para divisão",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Conteúdo ──────────────────────────────────────────────────────
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.groups.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null,
                            tint = WtcBlueHint, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Nenhum grupo cadastrado.", color = TextMuted, fontSize = 14.sp)
                    }
                }
                else -> LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Grupos por divisão
                    uiState.divisions.forEach { division ->
                        val divGroups = groupsByDivision[division.id] ?: emptyList()
                        if (divGroups.isNotEmpty()) {
                            item {
                                // Header de divisão com botão de disparo rápido
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        division.name.uppercase(),
                                        fontSize     = 11.sp,
                                        fontWeight   = FontWeight.Bold,
                                        color        = TextMuted,
                                        letterSpacing = 1.2.sp
                                    )
                                    // Botão de disparo rápido para esta divisão
                                    Surface(
                                        onClick = {
                                            selectedDivisionForDispatch = division
                                            showDivisionDialog = true
                                        },
                                        color  = WtcBluePale,
                                        shape  = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null,
                                                tint = WtcBlue, modifier = Modifier.size(12.dp))
                                            Text("Disparar para divisão", fontSize = 11.sp,
                                                color = WtcBlue, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            items(divGroups) { group ->
                                GroupListCard(
                                    group    = group,
                                    division = division.name,
                                    onClick  = { onGroupClick(group.id, group.name) }
                                )
                            }
                        }
                    }

                    // Grupos sem divisão
                    val noDivisionGroups = groupsByDivision["sem_divisao"] ?: emptyList()
                    if (noDivisionGroups.isNotEmpty()) {
                        item {
                            Text("SEM DIVISÃO", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = TextMuted, letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                        }
                        items(noDivisionGroups) { group ->
                            GroupListCard(
                                group    = group,
                                division = "",
                                onClick  = { onGroupClick(group.id, group.name) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialog de disparo em massa ────────────────────────────────────────────
    if (showDivisionDialog) {
        DivisionBroadcastDialog(
            divisions          = uiState.divisions,
            preSelectedDivision = selectedDivisionForDispatch,
            isSending          = uiState.isSending,
            onDismiss = {
                showDivisionDialog = false
                selectedDivisionForDispatch = null
            },
            onSendToDivision = { text, divisionId ->
                viewModel.sendToDivision(text, divisionId)
                showDivisionDialog = false
                selectedDivisionForDispatch = null
            }
        )
    }
}

// ── BottomSheet de disparo em massa para divisão (com anexo) ─────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivisionBroadcastDialog(
    divisions: List<Division>,
    preSelectedDivision: Division? = null,
    isSending: Boolean = false,
    onDismiss: () -> Unit,
    onSendToDivision: (text: String, divisionId: String) -> Unit
) {
    var messageText      by remember { mutableStateOf("") }
    var selectedDivision by remember { mutableStateOf(preSelectedDivision) }
    var expanded         by remember { mutableStateOf(false) }

    val uploadViewModel: ImageUploadViewModel = viewModel()
    val uploadState by uploadViewModel.uiState.collectAsState()
    var pendingFileUri  by remember { mutableStateOf<String?>(null) }
    var pendingFileKey  by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var pendingIsPdf    by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Título
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Send, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(20.dp))
                Text("Disparar para Divisão", fontWeight = FontWeight.Bold,
                    color = TextPrimary, fontSize = 16.sp)
            }

            HorizontalDivider(color = Color(0xFFF0F6FA))

            // Aviso informativo
            Surface(color = WtcBluePale, shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = WtcBlue, modifier = Modifier.size(16.dp))
                    Text("A mensagem será enviada para todos os grupos desta divisão.",
                        fontSize = 12.sp, color = WtcBlue)
                }
            }

            // Dropdown de divisão
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedDivision?.name ?: "Selecione uma divisão...",
                    onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = WtcBlue,
                        unfocusedBorderColor = WtcBlueHint)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    divisions.forEach { division ->
                        DropdownMenuItem(
                            text = { Text(division.name) },
                            onClick = { selectedDivision = division; expanded = false }
                        )
                    }
                }
            }

            // Campo de mensagem
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Mensagem (opcional se houver anexo)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                minLines = 3,
                maxLines = Int.MAX_VALUE,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = WtcBlue,
                    unfocusedBorderColor = WtcBlueHint,
                    cursorColor          = WtcBlue)
            )

            // Preview do arquivo pendente
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

            // Linha inferior: botão de anexo + botão disparar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão anexar
                Box(
                    modifier = Modifier
                        .size(48.dp)
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

                // Botão disparar
                Button(
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
                        onSendToDivision(finalText, selectedDivision!!.id)
                    },
                    enabled = (messageText.isNotBlank() || pendingFileKey != null)
                            && selectedDivision != null && !isSending,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White,
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disparar para divisão", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Card de grupo ─────────────────────────────────────────────────────────────

@Composable
fun GroupListCard(
    group: Group,
    division: String,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (division.isNotBlank()) {
                    Text(division, fontSize = 12.sp,
                        color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                }
            }
            // Ícone de chat indicando que é clicável para conversar
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Chat, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}