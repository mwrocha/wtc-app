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
    val uiState          by viewModel.uiState.collectAsState()
    val searchQuery      by viewModel.searchQuery.collectAsState()
    val selectedTags     by viewModel.selectedTags.collectAsState()
    val selectedDivision by viewModel.selectedDivision.collectAsState()
    val selectedGroup    by viewModel.selectedGroup.collectAsState()

    var showGroupMessageDialog by remember { mutableStateOf(false) }
    var showFilterSheet        by remember { mutableStateOf(false) }

    // Grupos filtrados pela divisão selecionada
    val filteredGroups = remember(selectedDivision, uiState.groups) {
        if (selectedDivision != null)
            uiState.groups.filter { it.divisionId == selectedDivision!!.id }
        else uiState.groups
    }

    // Contagem de filtros ativos (exceto busca por nome)
    val activeFilterCount = remember(selectedTags, selectedDivision, selectedGroup) {
        selectedTags.size +
                (if (selectedDivision != null) 1 else 0) +
                (if (selectedGroup != null) 1 else 0)
    }

    Scaffold(containerColor = Color(0xFFF5FAFD)) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(WtcBlue, WtcBlueSoft)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Text("Ver Clientes", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Busque e gerencie seus clientes",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                }
            }

            // ── Barra de busca + botão filtro ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.weight(1f),
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
                            focusedBorderColor      = WtcBlue,
                            unfocusedBorderColor    = WtcBlueHint,
                            cursorColor             = WtcBlue,
                            focusedContainerColor   = WtcBluePale.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color(0xFFFAFCFE)
                        )
                    )

                    // Botão de filtro com badge
                    Box {
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (activeFilterCount > 0) WtcBlue else WtcBluePale,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(Icons.Default.FilterList,
                                contentDescription = "Filtros",
                                tint = if (activeFilterCount > 0) Color.White else WtcBlue,
                                modifier = Modifier.size(22.dp))
                        }
                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE65100))
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activeFilterCount.toString(),
                                    fontSize = 10.sp, color = Color.White,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Chips de filtros ativos ───────────────────────────────────────
            AnimatedVisibility(activeFilterCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedDivision?.let { div ->
                        ActiveFilterChip(label = div.name, icon = Icons.Default.AccountTree) {
                            viewModel.onDivisionSelected(null)
                        }
                    }
                    selectedGroup?.let { grp ->
                        ActiveFilterChip(label = grp.name, icon = Icons.Default.Group) {
                            viewModel.onGroupSelected(null)
                        }
                    }
                    selectedTags.forEach { tag ->
                        ActiveFilterChip(label = tag, icon = Icons.Default.Label) {
                            viewModel.onTagSelected(tag)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.clearAllFilters() },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Limpar", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            // ── Lista de clientes ─────────────────────────────────────────────
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
                        if (activeFilterCount > 0) {
                            TextButton(onClick = { viewModel.clearAllFilters() }) {
                                Text("Limpar filtros", color = WtcBlue)
                            }
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        item {
                            Text("${uiState.clients.size} cliente(s)",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = TextMuted, letterSpacing = 0.5.sp,
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

    // ── Bottom Sheet de Filtros ───────────────────────────────────────────────
    if (showFilterSheet) {
        FilterBottomSheet(
            divisions        = uiState.divisions,
            groups           = filteredGroups,
            allGroups        = uiState.groups,
            availableTags    = uiState.availableTags,
            selectedDivision = selectedDivision,
            selectedGroup    = selectedGroup,
            selectedTags     = selectedTags,
            onDivisionSelect = { viewModel.onDivisionSelected(it) },
            onGroupSelect    = { viewModel.onGroupSelected(it) },
            onTagToggle      = { viewModel.onTagSelected(it) },
            onClearAll       = { viewModel.clearAllFilters() },
            onDismiss        = { showFilterSheet = false }
        )
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

// ── Bottom Sheet de Filtros ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    divisions: List<Division>,
    groups: List<Group>,
    allGroups: List<Group>,
    availableTags: List<String>,
    selectedDivision: Division?,
    selectedGroup: Group?,
    selectedTags: Set<String>,
    onDivisionSelect: (Division?) -> Unit,
    onGroupSelect: (Group?) -> Unit,
    onTagToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var divisionExpanded by remember { mutableStateOf(false) }
    var groupExpanded    by remember { mutableStateOf(false) }

    val groupsForSelectedDivision = remember(selectedDivision, allGroups) {
        if (selectedDivision != null) allGroups.filter { it.divisionId == selectedDivision.id }
        else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtrar clientes", fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = { onClearAll(); onDismiss() }) {
                    Text("Limpar tudo", color = TextMuted, fontSize = 13.sp)
                }
            }

            HorizontalDivider(color = Color(0xFFF0F6FA))

            // ── Divisão ───────────────────────────────────────────────────────
            Text("Divisão", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            ExposedDropdownMenuBox(
                expanded = divisionExpanded,
                onExpandedChange = { divisionExpanded = !divisionExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDivision?.name ?: "Todas as divisões",
                    onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(divisionExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint)
                )
                ExposedDropdownMenu(
                    expanded = divisionExpanded,
                    onDismissRequest = { divisionExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas as divisões", color = TextMuted) },
                        onClick = { onDivisionSelect(null); divisionExpanded = false }
                    )
                    divisions.forEach { div ->
                        DropdownMenuItem(
                            text = { Text(div.name) },
                            onClick = { onDivisionSelect(div); divisionExpanded = false },
                            trailingIcon = {
                                if (selectedDivision?.id == div.id)
                                    Icon(Icons.Default.Check, null, tint = WtcBlue,
                                        modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // ── Grupo ─────────────────────────────────────────────────────────
            Text("Grupo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            ExposedDropdownMenuBox(
                expanded = groupExpanded,
                onExpandedChange = {
                    if (selectedDivision != null) groupExpanded = !groupExpanded
                }
            ) {
                OutlinedTextField(
                    value = selectedGroup?.name
                        ?: if (selectedDivision == null) "Selecione uma divisão primeiro"
                        else "Todos os grupos",
                    onValueChange = {}, readOnly = true,
                    enabled = selectedDivision != null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint)
                )
                ExposedDropdownMenu(
                    expanded = groupExpanded,
                    onDismissRequest = { groupExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos os grupos", color = TextMuted) },
                        onClick = { onGroupSelect(null); groupExpanded = false }
                    )
                    groupsForSelectedDivision.forEach { grp ->
                        DropdownMenuItem(
                            text = { Text(grp.name) },
                            onClick = { onGroupSelect(grp); groupExpanded = false },
                            trailingIcon = {
                                if (selectedGroup?.id == grp.id)
                                    Icon(Icons.Default.Check, null, tint = WtcBlue,
                                        modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // ── Tags ──────────────────────────────────────────────────────────
            if (availableTags.isNotEmpty()) {
                Text("Tags", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTags.contains(tag),
                            onClick  = { onTagToggle(tag) },
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

            // ── Botão aplicar ─────────────────────────────────────────────────
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                Text("Aplicar filtros", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Chip de filtro ativo ──────────────────────────────────────────────────────

@Composable
fun ActiveFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRemove: () -> Unit
) {
    Surface(
        color = WtcBlue,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(12.dp))
            Text(label, fontSize = 11.sp, color = Color.White,
                fontWeight = FontWeight.Medium, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 80.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Close, contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Card do cliente ───────────────────────────────────────────────────────────

@Composable
fun ClientCard(client: Client, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Text(client.name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WtcBlue)
            }
            Spacer(modifier = Modifier.width(12.dp))
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
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = WtcBlueHint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ClientChip(tag: String) {
    Surface(color = WtcBluePale, shape = RoundedCornerShape(6.dp)) {
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
                                selectedDivision = division; selectedGroup = null
                                divisionExpanded = false
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