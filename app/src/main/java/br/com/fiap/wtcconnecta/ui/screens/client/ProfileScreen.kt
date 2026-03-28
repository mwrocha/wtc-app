package br.com.fiap.wtcconnecta.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.viewmodel.ProfileViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showGroupRequestDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    LaunchedEffect(clientId) { viewModel.loadProfile(clientId) }

    // Preenche o nome editável quando o perfil carrega
    LaunchedEffect(uiState.client) {
        if (editedName.isBlank()) editedName = uiState.client?.name ?: ""
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHostState.showSnackbar("Perfil atualizado com sucesso!")
            viewModel.clearSuccess()
            isEditing = false
            onProfileUpdated()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.emailSuccess) {
        if (uiState.emailSuccess) {
            snackbarHostState.showSnackbar("E-mail alterado! Faça login com o novo e-mail.")
            viewModel.clearEmailState()
            showEmailDialog = false
            onNavigateBack()
        }
    }

    // Grupo e divisão atuais
    val currentGroup    = uiState.groups.firstOrNull { it.id == uiState.client?.groupId }
    val currentDivision = uiState.divisions.firstOrNull { it.id == currentGroup?.divisionId }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { isEditing = false; editedName = uiState.client?.name ?: "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                        }
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Check, contentDescription = "Salvar", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WtcBlue)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue)
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5FAFD))
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Avatar e nome ─────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(WtcBluePale),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.client?.name?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = WtcBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Nome") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
                        } else {
                            Text(
                                text = uiState.client?.name ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.client?.email ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Informações de conta ──────────────────────────────────
                Text("Informações da Conta", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = WtcBlue)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("E-mail", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp)
                                Text(uiState.client?.email ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                            }
                            TextButton(onClick = { showEmailDialog = true }) {
                                Text("Alterar", style = MaterialTheme.typography.labelMedium, color = WtcBlue)
                            }
                        }
                        HorizontalDivider()
                        ProfileInfoRow(
                            icon  = Icons.Default.Business,
                            label = "Divisão",
                            value = currentDivision?.name ?: "Não vinculado",
                            isReadOnly = true
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Grupo", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp)
                                Text(currentGroup?.name ?: "Não vinculado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                            }
                            TextButton(onClick = { showGroupRequestDialog = true }) {
                                Text("Solicitar troca", style = MaterialTheme.typography.labelMedium, color = WtcBlue)
                            }
                        }
                    }
                }

                // ── Segurança ─────────────────────────────────────────────
                Text("Segurança", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = WtcBlue)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null,
                                tint = WtcBlue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Senha", style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text("Alterar senha da conta", style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted)
                            }
                        }
                        IconButton(onClick = { showPasswordDialog = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Alterar senha",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Dialog confirmar salvar nome
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Confirmar alterações") },
            text  = { Text("Deseja salvar as alterações no perfil?") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    viewModel.updateClientProfile(
                        name            = editedName.ifBlank { uiState.client?.name ?: "" },
                        selectedGroupId = uiState.client?.groupId ?: ""
                    )
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog alterar senha
    if (showPasswordDialog) {
        ChangePasswordDialog(
            isLoading = uiState.isLoading,
            error     = uiState.passwordError,
            success   = uiState.passwordSuccess,
            onDismiss = { showPasswordDialog = false; viewModel.clearPasswordState() },
            onConfirm = { current, new -> viewModel.changePassword(current, new) }
        )
    }

    if (showEmailDialog) {
        ClientChangeEmailDialog(
            currentEmail = uiState.client?.email ?: "",
            isLoading    = uiState.isLoading,
            error        = uiState.emailError,
            onDismiss    = { showEmailDialog = false; viewModel.clearEmailState() },
            onConfirm    = { newEmail, password -> viewModel.changeEmail(newEmail, password) }
        )
    }

    if (showGroupRequestDialog) {
        GroupChangeRequestDialog(
            groups        = uiState.groups,
            divisions     = uiState.divisions,
            currentGroupId = uiState.client?.groupId ?: "",
            onDismiss     = { showGroupRequestDialog = false },
            onConfirm     = { groupId: String, reason: String ->
                viewModel.requestGroupChange(groupId, reason)
                showGroupRequestDialog = false
            }
        )
    }
}

// ── Row de informação ─────────────────────────────────────────────────────────

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isReadOnly: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            tint = WtcBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (isReadOnly) {
            Surface(
                color = WtcBluePale,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("só leitura", style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp)
            }
        }
    }
}

// ── Dialog alterar senha ──────────────────────────────────────────────────────

@Composable
fun ChangePasswordDialog(
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (current: String, new: String) -> Unit
) {
    var currentPassword  by remember { mutableStateOf("") }
    var newPassword      by remember { mutableStateOf("") }
    var confirmPassword  by remember { mutableStateOf("") }
    var showCurrent      by remember { mutableStateOf(false) }
    var showNew          by remember { mutableStateOf(false) }
    var showConfirm      by remember { mutableStateOf(false) }
    val passwordMismatch = newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword != confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar Senha", fontWeight = FontWeight.Bold) },
        text = {
            if (success) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Senha alterada com sucesso!", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClientPasswordField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Senha atual",
                        visible = showCurrent,
                        onToggleVisibility = { showCurrent = !showCurrent }
                    )
                    ClientPasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "Nova senha",
                        visible = showNew,
                        onToggleVisibility = { showNew = !showNew }
                    )
                    ClientPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirmar nova senha",
                        visible = showConfirm,
                        onToggleVisibility = { showConfirm = !showConfirm },
                        isError = passwordMismatch
                    )
                    if (passwordMismatch) {
                        Text("As senhas não coincidem.", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    error?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (success) {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            } else {
                Button(
                    onClick = { onConfirm(currentPassword, newPassword) },
                    enabled = !isLoading && currentPassword.isNotBlank() &&
                            newPassword.isNotBlank() && !passwordMismatch,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Alterar")
                }
            }
        },
        dismissButton = {
            if (!success) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
fun ClientPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            }
        }
    )
}

// ── Dialog alterar email (cliente) ────────────────────────────────────────────

@Composable
fun ClientChangeEmailDialog(
    currentEmail: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (newEmail: String, password: String) -> Unit
) {
    var newEmail       by remember { mutableStateOf("") }
    var confirmEmail   by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var showPassword   by remember { mutableStateOf(false) }
    val emailMismatch  = newEmail.isNotBlank() && confirmEmail.isNotBlank() && newEmail != confirmEmail
    val emailUnchanged = newEmail.isNotBlank() && newEmail == currentEmail

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar E-mail", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("E-mail atual: $currentEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp))
                }
                OutlinedTextField(
                    value = newEmail, onValueChange = { newEmail = it },
                    label = { Text("Novo e-mail") }, singleLine = true,
                    isError = emailMismatch || emailUnchanged,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = confirmEmail, onValueChange = { confirmEmail = it },
                    label = { Text("Confirmar novo e-mail") }, singleLine = true,
                    isError = emailMismatch,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                if (emailMismatch) Text("Os e-mails não coincidem.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
                if (emailUnchanged) Text("O novo e-mail é igual ao atual.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
                ClientPasswordField(
                    value = password, onValueChange = { password = it },
                    label = "Confirme sua senha",
                    visible = showPassword,
                    onToggleVisibility = { showPassword = !showPassword }
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Você será desconectado e precisará fazer login com o novo e-mail.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newEmail, password) },
                enabled = !isLoading && newEmail.isNotBlank() && !emailMismatch &&
                        !emailUnchanged && password.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Alterar E-mail")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Dialog solicitar troca de grupo ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChangeRequestDialog(
    groups: List<br.com.fiap.wtcconnecta.data.model.Group>,
    divisions: List<br.com.fiap.wtcconnecta.data.model.Division>,
    currentGroupId: String,
    onDismiss: () -> Unit,
    onConfirm: (groupId: String, reason: String) -> Unit
) {
    var selectedGroup by remember { mutableStateOf<br.com.fiap.wtcconnecta.data.model.Group?>(null) }
    var reason        by remember { mutableStateOf("") }
    var expanded      by remember { mutableStateOf(false) }

    // Agrupa para exibição
    val groupsByDivision = groups.groupBy { it.divisionId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Troca de Grupo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Escolha o grupo desejado. Um operador avaliará sua solicitação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "Selecione o novo grupo...",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Novo grupo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = selectedGroup?.id == currentGroupId
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        divisions.forEach { division ->
                            val divGroups = groupsByDivision[division.id] ?: emptyList()
                            if (divGroups.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(division.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = WtcBlue)
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                divGroups.filter { it.id != currentGroupId }.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text("  ${group.name}") },
                                        onClick = { selectedGroup = group; expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedGroup?.id == currentGroupId) {
                    Text("Este já é seu grupo atual.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3, shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedGroup!!.id, reason) },
                enabled = selectedGroup != null && selectedGroup?.id != currentGroupId,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Enviar Solicitação") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}