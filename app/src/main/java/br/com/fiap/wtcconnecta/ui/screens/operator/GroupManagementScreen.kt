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

private val WtcBlue = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    onBack: () -> Unit, viewModel: GroupManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadAll() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color(0xFFF0F6FA)
    ) { innerPadding ->
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
                    .statusBarsPadding()
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
                        .padding(top = 16.dp, bottom = 28.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Grupos e Divisões",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Gerencie a estrutura organizacional",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        letterSpacing = 0.2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pills de contagem
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Business,
                                    null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "${uiState.divisions.size} divisões",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "${uiState.groups.size} grupos",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── TabRow ────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = WtcBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WtcBlue
                    )
                }) {
                listOf(
                    "Divisões" to Icons.Default.Business,
                    "Grupos" to Icons.Default.Group
                ).forEachIndexed { index, (label, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) WtcBlue else TextMuted
                                )
                            },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (selectedTab == index) WtcBlue else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            })
                    }
            }

            when (selectedTab) {
                0 -> DivisionsTab(
                    divisions = uiState.divisions,
                    isLoading = uiState.isLoading,
                    onCreateDivision = { viewModel.createDivision(it) },
                    onEditDivision = { id, name -> viewModel.updateDivision(id, name) },
                    onDeleteDivision = { viewModel.deleteDivision(it) })

                1 -> GroupsTab(
                    groups = uiState.groups,
                    divisions = uiState.divisions,
                    isLoading = uiState.isLoading,
                    onCreateGroup = { name, divId -> viewModel.createGroup(name, divId) },
                    onEditGroup = { id, name, divId -> viewModel.updateGroup(id, name, divId) },
                    onDeleteGroup = { viewModel.deleteGroup(it) })
            }
        }
    }
}

// ── Aba Divisões ──────────────────────────────────────────────────────────────

@Composable
fun DivisionsTab(
    divisions: List<Division>,
    isLoading: Boolean,
    onCreateDivision: (String) -> Unit,
    onEditDivision: (String, String) -> Unit,
    onDeleteDivision: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var divisionToEdit by remember { mutableStateOf<Division?>(null) }
    var divisionToDelete by remember { mutableStateOf<Division?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de ação
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F6FA))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DIVISÕES — ${divisions.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.2.sp
            )
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                Icon(
                    Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nova Divisão", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue)
            }

            divisions.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(WtcBluePale), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Business,
                            null,
                            tint = WtcBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        "Nenhuma divisão cadastrada.",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Crie a primeira divisão acima.", color = TextMuted, fontSize = 13.sp)
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(divisions, key = { it.id }) { division ->
                    DivisionCard(
                        division,
                        onEdit = { divisionToEdit = division },
                        onDelete = { divisionToDelete = division })
                }
            }
        }
    }

    if (showCreateDialog) EditNameDialog(
        "Nova Divisão",
        "Nome da divisão",
        onDismiss = { showCreateDialog = false },
        onConfirm = { onCreateDivision(it); showCreateDialog = false })

    divisionToEdit?.let { d ->
        EditNameDialog(
            "Editar Divisão",
            "Nome da divisão",
            d.name,
            onDismiss = { divisionToEdit = null },
            onConfirm = { name -> onEditDivision(d.id, name); divisionToEdit = null })
    }
    divisionToDelete?.let { d ->
        ConfirmDeleteDialog(
            d.name,
            { divisionToDelete = null },
            { onDeleteDivision(d.id); divisionToDelete = null })
    }
}

@Composable
fun DivisionCard(division: Division, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFCEE6F0)), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    null,
                    tint = Color(0xFF074365),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                division.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit, null, tint = WtcBlue, modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Aba Grupos ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsTab(
    groups: List<Group>,
    divisions: List<Division>,
    isLoading: Boolean,
    onCreateGroup: (String, String) -> Unit,
    onEditGroup: (String, String, String) -> Unit,
    onDeleteGroup: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    val groupsByDivision = groups.groupBy { it.divisionId }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F6FA))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "GRUPOS — ${groups.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.2.sp
            )
            Button(
                onClick = { showCreateDialog = true },
                enabled = divisions.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Grupo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (divisions.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Crie uma divisão primeiro.", color = TextMuted, fontSize = 14.sp)
            }
        } else when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue)
            }

            groups.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(WtcBluePale), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Group,
                            null,
                            tint = WtcBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        "Nenhum grupo cadastrado.",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Crie o primeiro grupo acima.", color = TextMuted, fontSize = 13.sp)
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                divisions.forEach { division ->
                    val divGroups = groupsByDivision[division.id] ?: emptyList()
                    if (divGroups.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFCEE6F0))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    division.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF074365),
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        items(divGroups, key = { it.id }) { group ->
                            GroupCard(
                                group,
                                onEdit = { groupToEdit = group },
                                onDelete = { groupToDelete = group })
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) GroupFormDialog(
        divisions,
        onDismiss = { showCreateDialog = false },
        onConfirm = { name, divId -> onCreateGroup(name, divId); showCreateDialog = false })

    groupToEdit?.let { g ->
        GroupFormDialog(
            divisions,
            g.name,
            g.divisionId,
            onDismiss = { groupToEdit = null },
            onConfirm = { name, divId -> onEditGroup(g.id, name, divId); groupToEdit = null })
    }
    groupToDelete?.let { g ->
        ConfirmDeleteDialog(
            g.name,
            { groupToDelete = null },
            { onDeleteGroup(g.id); groupToDelete = null })
    }
}

@Composable
fun GroupCard(group: Group, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(WtcBlueHint), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group, null, tint = WtcBlueDark, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                group.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit, null, tint = WtcBlue, modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun EditNameDialog(
    title: String,
    placeholder: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue,
                    unfocusedBorderColor = WtcBlueHint,
                    cursorColor = WtcBlue
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) { Text("Salvar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFormDialog(
    divisions: List<Division>,
    initialName: String = "",
    initialDivId: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedDivision by remember { mutableStateOf(divisions.firstOrNull { it.id == initialDivId }) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(
            if (initialName.isBlank()) "Novo Grupo" else "Editar Grupo",
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do grupo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WtcBlue,
                    unfocusedBorderColor = WtcBlueHint,
                    cursorColor = WtcBlue
                )
            )
            ExposedDropdownMenuBox(
                expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedDivision?.name ?: "Selecione uma divisão *",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Divisão") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = selectedDivision == null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false }) {
                    divisions.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.name) },
                            onClick = { selectedDivision = d; expanded = false })
                    }
                }
            }
            if (selectedDivision == null) {
                Text(
                    "Divisão é obrigatória.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }, confirmButton = {
        Button(
            onClick = {
                if (name.isNotBlank() && selectedDivision != null) onConfirm(
                    name,
                    selectedDivision!!.id
                )
            },
            enabled = name.isNotBlank() && selectedDivision != null,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
        ) { Text("Salvar", fontWeight = FontWeight.SemiBold) }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
    })
}

@Composable
fun ConfirmDeleteDialog(itemName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remover?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Text("\"$itemName\" será removido permanentemente.", color = TextMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "Remover",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        })
}