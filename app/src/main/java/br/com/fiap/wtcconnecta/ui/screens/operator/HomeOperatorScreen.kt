package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.viewmodel.HomeOperatorViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOperatorScreen(
    currentOperatorId: String,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onClientClick: (clientId: String) -> Unit,
    viewModel: HomeOperatorViewModel = viewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    var showGroupMessageDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color(0xFFF0F6FA)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Header Hero ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .offset(x = 200.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .offset(x = 260.dp, y = 40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 28.dp)
                ) {
                    Text("CLIENTES", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.60f),
                        letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ver Clientes", fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, lineHeight = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Busque e gerencie seus clientes",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        letterSpacing = 0.2.sp)
                }
            }

            // ── Barra de busca ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F6FA))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nome...", color = TextMuted.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = if (searchQuery.isNotBlank()) WtcBlue else WtcBlueHint,
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = null,
                                    tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = WtcBlue,
                        unfocusedBorderColor = WtcBlueHint,
                        cursorColor          = WtcBlue,
                        focusedContainerColor   = WtcBluePale.copy(alpha = 0.4f),
                        unfocusedContainerColor = Color(0xFFFAFCFE)
                    )
                )
            }

            // Tags
            AnimatedVisibility(uiState.availableTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F6FA))
                        .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableTags) { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick  = { viewModel.onTagSelected(tag) },
                            label    = { Text(tag, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WtcBlue,
                                selectedLabelColor     = Color.White,
                                containerColor         = WtcBluePale,
                                labelColor             = WtcBlue
                            )
                        )
                    }
                }
            }

            // Conteúdo
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = WtcBlue)
                    }
                    uiState.error != null -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null,
                            tint = WtcBlueHint, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error ?: "Erro ao carregar", color = TextMuted)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.retryFetch() },
                            colors = ButtonDefaults.buttonColors(containerColor = WtcBlue),
                            shape = RoundedCornerShape(10.dp)) {
                            Text("Tentar novamente")
                        }
                    }
                    uiState.clients.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null,
                            tint = WtcBlueHint, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Nenhum cliente encontrado.", color = TextMuted, fontSize = 14.sp)
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        item {
                            Text("CLIENTES — ${uiState.clients.size}",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = TextMuted, letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        items(uiState.clients) { client ->
                            ClientCard(client = client, onClick = { onClientClick(client.id) })
                        }
                    }
                }
            }
        }
    }

    if (showGroupMessageDialog) {
        GroupMessageDialog(
            divisions = uiState.divisions,
            groups    = uiState.groups,
            onDismiss = { showGroupMessageDialog = false },
            onSendGroup = { text, groupId ->
                viewModel.sendGroupMessage(text, groupId, currentOperatorId)
                showGroupMessageDialog = false
            },
            onSendDivision = { text, divisionId ->
                viewModel.sendDivisionMessage(text, divisionId, currentOperatorId)
                showGroupMessageDialog = false
            }
        )
    }
}

@Composable
fun ClientCard(client: Client, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar inicial com fundo WtcBlueHint
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WtcBlueHint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    client.name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 19.sp, fontWeight = FontWeight.Bold, color = WtcBlueDark
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(client.email, fontSize = 12.sp, color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!client.tags.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        client.tags.take(3).forEach { tag -> ClientChip(tag) }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(WtcBlueHint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = WtcBlueDark, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
fun ClientChip(tag: String) {
    Surface(
        color = WtcBluePale,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(tag, fontSize = 10.sp, color = WtcBlue, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMessageDialog(
    divisions: List<Division>,
    groups: List<Group>,
    onDismiss: () -> Unit,
    onSendGroup: (text: String, groupId: String) -> Unit,
    onSendDivision: (text: String, divisionId: String) -> Unit
) {
    var messageText        by remember { mutableStateOf("") }
    var divisionExpanded   by remember { mutableStateOf(false) }
    var groupExpanded      by remember { mutableStateOf(false) }
    var selectedDivision   by remember { mutableStateOf<Division?>(null) }
    var selectedGroup      by remember { mutableStateOf<Group?>(null) }
    var sendToFullDivision by remember { mutableStateOf(false) }

    val filteredGroups = remember(selectedDivision) {
        groups.filter { it.divisionId == selectedDivision?.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mensagem para grupo", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        divisions.forEach { division ->
                            DropdownMenuItem(text = { Text(division.name) }, onClick = {
                                selectedDivision = division; selectedGroup = null; divisionExpanded = false
                            })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = !groupExpanded }) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "Selecione um grupo...",
                        onValueChange = {}, readOnly = true,
                        enabled = selectedDivision != null && !sendToFullDivision,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint)
                    )
                    ExposedDropdownMenu(expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }) {
                        filteredGroups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = {
                                selectedGroup = group; groupExpanded = false
                            })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = selectedDivision != null) {
                        sendToFullDivision = !sendToFullDivision
                        if (sendToFullDivision) selectedGroup = null
                    }) {
                    Checkbox(checked = sendToFullDivision,
                        onCheckedChange = { sendToFullDivision = it; if (it) selectedGroup = null },
                        enabled = selectedDivision != null,
                        colors = CheckboxDefaults.colors(checkedColor = WtcBlue))
                    Text("Enviar para toda a divisão", fontSize = 14.sp, color = TextPrimary)
                }

                OutlinedTextField(
                    value = messageText, onValueChange = { messageText = it },
                    label = { Text("Mensagem") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    minLines = 4,
                    maxLines = Int.MAX_VALUE,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
                        cursorColor = WtcBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sendToFullDivision) onSendDivision(messageText, selectedDivision!!.id)
                    else onSendGroup(messageText, selectedGroup!!.id)
                },
                enabled = messageText.isNotBlank() &&
                        (selectedGroup != null || (sendToFullDivision && selectedDivision != null)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}