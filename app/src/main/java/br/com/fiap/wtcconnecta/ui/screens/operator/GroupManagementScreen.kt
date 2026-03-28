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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.viewmodel.GroupManagementViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    onBack: () -> Unit,
    viewModel: GroupManagementViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var selectedTab       by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadAll() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
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
                        Text("Grupos e Divisões", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Gerencie a estrutura organizacional",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                    }
                }
            }

            // TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = Color.White,
                contentColor     = WtcBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color    = WtcBlue
                    )
                }
            ) {
                listOf("Divisões" to Icons.Default.Business, "Grupos" to Icons.Default.Group)
                    .forEachIndexed { index, (label, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text = { Text(label, fontSize = 13.sp,
                                color = if (selectedTab == index) WtcBlue else TextMuted) },
                            icon = { Icon(icon, contentDescription = null,
                                tint = if (selectedTab == index) WtcBlue else TextMuted,
                                modifier = Modifier.size(18.dp)) }
                        )
                    }
            }

            when (selectedTab) {
                0 -> DivisionsTab(divisions = uiState.divisions, isLoading = uiState.isLoading,
                    onCreateDivision = { viewModel.createDivision(it) },
                    onEditDivision   = { id, name -> viewModel.updateDivision(id, name) },
                    onDeleteDivision = { viewModel.deleteDivision(it) })
                1 -> GroupsTab(groups = uiState.groups, divisions = uiState.divisions,
                    isLoading = uiState.isLoading,
                    onCreateGroup = { name, divId -> viewModel.createGroup(name, divId) },
                    onEditGroup   = { id, name, divId -> viewModel.updateGroup(id, name, divId) },
                    onDeleteGroup = { viewModel.deleteGroup(it) })
            }
        }
    }
}

// ── Aba Divisões ──────────────────────────────────────────────────────────────

@Composable
fun DivisionsTab(
    divisions: List<Division>, isLoading: Boolean,
    onCreateDivision: (String) -> Unit,
    onEditDivision: (String, String) -> Unit,
    onDeleteDivision: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var divisionToEdit   by remember { mutableStateOf<Division?>(null) }
    var divisionToDelete by remember { mutableStateOf<Division?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de ação
        Row(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Surface(color = WtcBluePale, shape = RoundedCornerShape(8.dp)) {
                Text("${divisions.size} divisão(ões)", fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = WtcBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Button(onClick = { showCreateDialog = true }, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova Divisão", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider(color = WtcBlueHint.copy(alpha = 0.5f))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue) }
            divisions.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(WtcBluePale),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Business, null, tint = WtcBlue, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Nenhuma divisão cadastrada.", color = TextMuted, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(divisions, key = { it.id }) { division ->
                    DivisionCard(division, { divisionToEdit = division }, { divisionToDelete = division })
                }
            }
        }
    }

    if (showCreateDialog) EditNameDialog("Nova Divisão", "Nome da divisão",
        onDismiss = { showCreateDialog = false },
        onConfirm = { onCreateDivision(it); showCreateDialog = false })

    divisionToEdit?.let { d ->
        EditNameDialog("Editar Divisão", "Nome da divisão", d.name,
            onDismiss = { divisionToEdit = null },
            onConfirm = { name -> onEditDivision(d.id, name); divisionToEdit = null })
    }
    divisionToDelete?.let { d ->
        ConfirmDeleteDialog(d.name, { divisionToDelete = null },
            { onDeleteDivision(d.id); divisionToDelete = null })
    }
}

@Composable
fun DivisionCard(division: Division, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(WtcBluePale),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Business, null, tint = WtcBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(division.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Edit, null, tint = WtcBlue, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ── Aba Grupos ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsTab(
    groups: List<Group>, divisions: List<Division>, isLoading: Boolean,
    onCreateGroup: (String, String) -> Unit,
    onEditGroup: (String, String, String) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToEdit      by remember { mutableStateOf<Group?>(null) }
    var groupToDelete    by remember { mutableStateOf<Group?>(null) }
    val groupsByDivision = groups.groupBy { it.divisionId }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Surface(color = WtcBluePale, shape = RoundedCornerShape(8.dp)) {
                Text("${groups.size} grupo(s)", fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = WtcBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Button(onClick = { showCreateDialog = true }, enabled = divisions.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Novo Grupo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider(color = WtcBlueHint.copy(alpha = 0.5f))

        if (divisions.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Crie uma divisão primeiro.", color = TextMuted)
            }
        } else when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue) }
            groups.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(WtcBluePale),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Group, null, tint = WtcBlue, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Nenhum grupo cadastrado.", color = TextMuted, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                divisions.forEach { division ->
                    val divGroups = groupsByDivision[division.id] ?: emptyList()
                    if (divGroups.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                Icon(Icons.Default.Business, null,
                                    tint = WtcBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(division.name, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = WtcBlue, letterSpacing = 0.5.sp)
                            }
                        }
                        items(divGroups, key = { it.id }) { group ->
                            GroupCard(group, { groupToEdit = group }, { groupToDelete = group })
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) GroupFormDialog(divisions, onDismiss = { showCreateDialog = false },
        onConfirm = { name, divId -> onCreateGroup(name, divId); showCreateDialog = false })

    groupToEdit?.let { g ->
        GroupFormDialog(divisions, g.name, g.divisionId,
            onDismiss = { groupToEdit = null },
            onConfirm = { name, divId -> onEditGroup(g.id, name, divId); groupToEdit = null })
    }
    groupToDelete?.let { g ->
        ConfirmDeleteDialog(g.name, { groupToDelete = null },
            { onDeleteGroup(g.id); groupToDelete = null })
    }
}

@Composable
fun GroupCard(group: Group, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(WtcBlueHint),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Group, null, tint = WtcBlueSoft, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(group.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, null, tint = WtcBlue, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun EditNameDialog(
    title: String, placeholder: String, initialValue: String = "",
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text(placeholder) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
                    cursorColor = WtcBlue))
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFormDialog(
    divisions: List<Division>, initialName: String = "", initialDivId: String = "",
    onDismiss: () -> Unit, onConfirm: (String, String) -> Unit
) {
    var name             by remember { mutableStateOf(initialName) }
    var selectedDivision by remember { mutableStateOf(divisions.firstOrNull { it.id == initialDivId }) }
    var expanded         by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isBlank()) "Novo Grupo" else "Editar Grupo",
            fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nome do grupo") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
                        cursorColor = WtcBlue))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedDivision?.name ?: "Selecione uma divisão *",
                        onValueChange = {}, readOnly = true, label = { Text("Divisão") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), isError = selectedDivision == null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint))
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        divisions.forEach { d ->
                            DropdownMenuItem(text = { Text(d.name) },
                                onClick = { selectedDivision = d; expanded = false })
                        }
                    }
                }
                if (selectedDivision == null) {
                    Text("Divisão é obrigatória.", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && selectedDivision != null) onConfirm(name, selectedDivision!!.id) },
                enabled = name.isNotBlank() && selectedDivision != null,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}

@Composable
fun ConfirmDeleteDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remover?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text  = { Text("\"$itemName\" será removido permanentemente.", color = TextMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remover", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}