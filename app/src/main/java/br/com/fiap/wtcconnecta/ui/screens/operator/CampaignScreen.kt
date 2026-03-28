package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.ActionButton
import br.com.fiap.wtcconnecta.data.model.Campaign
import br.com.fiap.wtcconnecta.data.model.CampaignRequest
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.viewmodel.CampaignViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)
private val ColorSent   = Color(0xFF1A7A5E)
private val ColorDraft  = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    onBack: () -> Unit,
    viewModel: CampaignViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var showCreateDialog  by remember { mutableStateOf(false) }
    var campaignToEdit    by remember { mutableStateOf<Campaign?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadData() }
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5FAFD),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { showCreateDialog = true },
                icon           = { Icon(Icons.Default.Add, contentDescription = null) },
                text           = { Text("Nova Campanha", fontWeight = FontWeight.SemiBold) },
                containerColor = WtcBlue,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(14.dp)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Header gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(WtcBlue, WtcBlueSoft)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
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
                        Text("Campanhas", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Gerencie e dispare campanhas",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.campaigns.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(WtcBluePale), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Campaign, contentDescription = null,
                                modifier = Modifier.size(40.dp), tint = WtcBlue)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Nenhuma campanha ainda", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Crie sua primeira campanha abaixo!", fontSize = 13.sp,
                            color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
                ) {
                    item {
                        Text("${uiState.campaigns.size} campanha(s)",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = TextMuted, letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                    items(uiState.campaigns.sortedByDescending { it.createdAt }) { campaign ->
                        CampaignCard(
                            campaign   = campaign,
                            onDispatch = { viewModel.dispatchExisting(campaign.id) },
                            onEdit     = { campaignToEdit = campaign }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCampaignDialog(
            groups = uiState.groups, divisions = uiState.divisions,
            availableTags = uiState.availableTags,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, body, url, groupId, divisionId, tags, actions, actionUrls ->
                viewModel.createAndDispatch(title, body, url, groupId, divisionId, tags, actions, actionUrls)
                showCreateDialog = false
            }
        )
    }

    campaignToEdit?.let { campaign ->
        EditCampaignDialog(
            campaign = campaign, groups = uiState.groups, divisions = uiState.divisions,
            availableTags = uiState.availableTags,
            onDismiss = { campaignToEdit = null },
            onConfirm = { request -> viewModel.updateCampaign(campaign.id, request); campaignToEdit = null }
        )
    }
}

@Composable
fun CampaignCard(campaign: Campaign, onDispatch: () -> Unit, onEdit: () -> Unit) {
    val isSent      = campaign.status == "SENT"
    val statusColor = if (isSent) ColorSent else ColorDraft
    val statusLabel = if (isSent) "Enviada" else "Rascunho"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header do card
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                        .background(WtcBluePale), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Campaign, contentDescription = null,
                            tint = WtcBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(campaign.title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatCampaignDate(campaign.createdAt),
                            fontSize = 11.sp, color = TextMuted)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                        Text(statusLabel, fontSize = 11.sp, color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar",
                            tint = WtcBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F6FA))
            Spacer(modifier = Modifier.height(10.dp))

            Text(campaign.body, fontSize = 13.sp, color = TextMuted,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)

            campaign.url?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null,
                        tint = WtcBlue, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(it, fontSize = 11.sp, color = WtcBlue,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            campaign.actions?.takeIf { it.isNotEmpty() }?.let { actions ->
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(actions) { action ->
                        Surface(color = WtcBluePale, shape = RoundedCornerShape(8.dp)) {
                            Text(action.title, fontSize = 11.sp, color = WtcBlue,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
            }

            if (!isSent) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDispatch,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disparar Agora", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Dialog Editar Campanha ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCampaignDialog(
    campaign: Campaign, groups: List<Group>, divisions: List<Division>,
    availableTags: List<String>, onDismiss: () -> Unit,
    onConfirm: (CampaignRequest) -> Unit
) {
    var title        by remember { mutableStateOf(campaign.title) }
    var body         by remember { mutableStateOf(campaign.body) }
    var url          by remember { mutableStateOf(campaign.url ?: "") }
    var btn1Title    by remember { mutableStateOf(campaign.actions?.getOrNull(0)?.title ?: "") }
    var btn1Url      by remember { mutableStateOf("") }
    var btn2Title    by remember { mutableStateOf(campaign.actions?.getOrNull(1)?.title ?: "") }
    var btn2Url      by remember { mutableStateOf("") }
    var showActions  by remember { mutableStateOf(campaign.actions?.isNotEmpty() == true) }
    var selectedGroup    by remember { mutableStateOf<Group?>(null) }
    var selectedDivision by remember { mutableStateOf<Division?>(null) }
    var selectedTags     by remember { mutableStateOf<Set<String>>(emptySet()) }
    var groupExpanded    by remember { mutableStateOf(false) }
    var divisionExpanded by remember { mutableStateOf(false) }
    var targetMode by remember { mutableStateOf(
        when { campaign.targetGroupId != null -> "group"; campaign.targetDivisionId != null -> "division"; else -> "tags" }
    )}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Campanha", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { CampaignField("Título *", title, { title = it }) }
                item { CampaignField("Mensagem *", body, { body = it }, maxLines = 4) }
                item { CampaignField("URL (opcional)", url, { url = it },
                    icon = Icons.Default.Link) }
                item { TargetModeSelector(targetMode) { targetMode = it } }
                if (targetMode == "group") item {
                    CampaignDropdown("Grupo", selectedGroup?.name
                        ?: groups.firstOrNull { it.id == campaign.targetGroupId }?.name
                        ?: "Selecione...", groupExpanded, { groupExpanded = !groupExpanded }) {
                        groups.forEach { g -> DropdownMenuItem(text = { Text(g.name) },
                            onClick = { selectedGroup = g; groupExpanded = false }) }
                    }
                }
                if (targetMode == "division") item {
                    CampaignDropdown("Divisão", selectedDivision?.name
                        ?: divisions.firstOrNull { it.id == campaign.targetDivisionId }?.name
                        ?: "Selecione...", divisionExpanded, { divisionExpanded = !divisionExpanded }) {
                        divisions.forEach { d -> DropdownMenuItem(text = { Text(d.name) },
                            onClick = { selectedDivision = d; divisionExpanded = false }) }
                    }
                }
                item { ActionButtonsToggle(showActions) { showActions = it } }
                if (showActions) {
                    item { CampaignField("Botão 1 — Texto", btn1Title, { btn1Title = it }) }
                    item { CampaignField("Botão 1 — URL", btn1Url, { btn1Url = it }) }
                    item { CampaignField("Botão 2 — Texto (opcional)", btn2Title, { btn2Title = it }) }
                    item { CampaignField("Botão 2 — URL", btn2Url, { btn2Url = it }) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actions = buildActions(showActions, btn1Title, btn1Url, btn2Title, btn2Url)
                    val actionUrls = buildActionUrls(showActions, btn1Title, btn1Url, btn2Title, btn2Url)
                    onConfirm(CampaignRequest(title = title, body = body,
                        url = url.takeIf { it.isNotBlank() },
                        targetGroupId    = if (targetMode == "group") (selectedGroup?.id ?: campaign.targetGroupId) else null,
                        targetDivisionId = if (targetMode == "division") (selectedDivision?.id ?: campaign.targetDivisionId) else null,
                        targetTags       = if (targetMode == "tags") selectedTags.toList() else emptyList(),
                        actions = actions, actionUrls = actionUrls))
                },
                enabled = title.isNotBlank() && body.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) { Text("Salvar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

// ── Dialog Criar Campanha ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignDialog(
    groups: List<Group>, divisions: List<Division>, availableTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?, String?, List<String>,
                List<ActionButton>, Map<String, String>) -> Unit
) {
    var title        by remember { mutableStateOf("") }
    var body         by remember { mutableStateOf("") }
    var url          by remember { mutableStateOf("") }
    var btn1Title    by remember { mutableStateOf("") }
    var btn1Url      by remember { mutableStateOf("") }
    var btn2Title    by remember { mutableStateOf("") }
    var btn2Url      by remember { mutableStateOf("") }
    var showActions  by remember { mutableStateOf(false) }
    var selectedGroup    by remember { mutableStateOf<Group?>(null) }
    var selectedDivision by remember { mutableStateOf<Division?>(null) }
    var selectedTags     by remember { mutableStateOf<Set<String>>(emptySet()) }
    var groupExpanded    by remember { mutableStateOf(false) }
    var divisionExpanded by remember { mutableStateOf(false) }
    var targetMode by remember { mutableStateOf("group") }

    val isValid = title.isNotBlank() && body.isNotBlank() && when (targetMode) {
        "group"    -> selectedGroup != null
        "division" -> selectedDivision != null
        "tags"     -> selectedTags.isNotEmpty()
        else       -> false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nova Campanha", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { CampaignField("Título *", title, { title = it }) }
                item { CampaignField("Mensagem *", body, { body = it }, maxLines = 4) }
                item { CampaignField("URL (opcional)", url, { url = it },
                    icon = Icons.Default.Link) }
                item { TargetModeSelector(targetMode) { targetMode = it } }
                if (targetMode == "group") item {
                    CampaignDropdown("Grupo", selectedGroup?.name ?: "Selecione...",
                        groupExpanded, { groupExpanded = !groupExpanded }) {
                        DropdownMenuItem(text = { Text("Nenhum") },
                            onClick = { selectedGroup = null; groupExpanded = false })
                        groups.forEach { g -> DropdownMenuItem(text = { Text(g.name) },
                            onClick = { selectedGroup = g; groupExpanded = false }) }
                    }
                }
                if (targetMode == "division") item {
                    CampaignDropdown("Divisão", selectedDivision?.name ?: "Selecione...",
                        divisionExpanded, { divisionExpanded = !divisionExpanded }) {
                        DropdownMenuItem(text = { Text("Nenhuma") },
                            onClick = { selectedDivision = null; divisionExpanded = false })
                        divisions.forEach { d -> DropdownMenuItem(text = { Text(d.name) },
                            onClick = { selectedDivision = d; divisionExpanded = false }) }
                    }
                }
                if (targetMode == "tags" && availableTags.isNotEmpty()) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableTags) { tag ->
                            FilterChip(selected = selectedTags.contains(tag),
                                onClick = { selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag },
                                label = { Text(tag, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WtcBlue, selectedLabelColor = Color.White,
                                    containerColor = WtcBluePale, labelColor = WtcBlue))
                        }
                    }
                }
                item { ActionButtonsToggle(showActions) { showActions = it } }
                if (showActions) {
                    item { CampaignField("Botão 1 — Texto", btn1Title, { btn1Title = it }) }
                    item { CampaignField("Botão 1 — URL", btn1Url, { btn1Url = it }) }
                    item { CampaignField("Botão 2 — Texto (opcional)", btn2Title, { btn2Title = it }) }
                    item { CampaignField("Botão 2 — URL", btn2Url, { btn2Url = it }) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actions    = buildActions(showActions, btn1Title, btn1Url, btn2Title, btn2Url)
                    val actionUrls = buildActionUrls(showActions, btn1Title, btn1Url, btn2Title, btn2Url)
                    onConfirm(title, body, url.takeIf { it.isNotBlank() },
                        if (targetMode == "group") selectedGroup?.id else null,
                        if (targetMode == "division") selectedDivision?.id else null,
                        if (targetMode == "tags") selectedTags.toList() else emptyList(),
                        actions, actionUrls)
                },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) { Text("Criar e Disparar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

// ── Componentes reutilizáveis dos dialogs ─────────────────────────────────────

@Composable
private fun CampaignField(
    label: String, value: String, onValueChange: (String) -> Unit,
    maxLines: Int = 1,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = maxLines == 1, maxLines = maxLines,
        leadingIcon = icon?.let { { Icon(it, contentDescription = null,
            tint = WtcBlueHint, modifier = Modifier.size(18.dp)) } },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
            cursorColor = WtcBlue, focusedLabelColor = WtcBlue,
            focusedContainerColor   = WtcBluePale.copy(alpha = 0.3f),
            unfocusedContainerColor = Color(0xFFFAFCFE))
    )
}

@Composable
private fun TargetModeSelector(targetMode: String, onSelect: (String) -> Unit) {
    Column {
        Text("Enviar para", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("group" to "Grupo", "division" to "Divisão", "tags" to "Tags").forEach { (mode, label) ->
                FilterChip(
                    selected = targetMode == mode, onClick = { onSelect(mode) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WtcBlue, selectedLabelColor = Color.White,
                        containerColor = WtcBluePale, labelColor = WtcBlue)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampaignDropdown(
    label: String, value: String, expanded: Boolean,
    onExpandedChange: () -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { onExpandedChange() }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint)
        )
        ExposedDropdownMenu(expanded = expanded,
            onDismissRequest = { onExpandedChange() }, content = content)
    }
}

@Composable
private fun ActionButtonsToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = WtcBlue))
        Text("Adicionar botões de ação", fontSize = 13.sp, color = TextPrimary)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildActions(
    showActions: Boolean, btn1Title: String, btn1Url: String,
    btn2Title: String, btn2Url: String
): List<ActionButton> {
    if (!showActions) return emptyList()
    val list = mutableListOf<ActionButton>()
    if (btn1Title.isNotBlank()) list.add(ActionButton("btn1", btn1Title))
    if (btn2Title.isNotBlank()) list.add(ActionButton("btn2", btn2Title))
    return list
}

private fun buildActionUrls(
    showActions: Boolean, btn1Title: String, btn1Url: String,
    btn2Title: String, btn2Url: String
): Map<String, String> {
    if (!showActions) return emptyMap()
    val map = mutableMapOf<String, String>()
    if (btn1Title.isNotBlank() && btn1Url.isNotBlank()) map["btn1"] = btn1Url
    if (btn2Title.isNotBlank() && btn2Url.isNotBlank()) map["btn2"] = btn2Url
    return map
}

private fun formatCampaignDate(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try { "${createdAt.substring(0, 10)} às ${createdAt.substring(11, 16)}" }
    catch (e: Exception) { "" }
}