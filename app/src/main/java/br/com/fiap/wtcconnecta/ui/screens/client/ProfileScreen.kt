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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.ui.components.AvatarPicker
import br.com.fiap.wtcconnecta.viewmodel.ProfileViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
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
    val context = LocalContext.current

    var showPasswordDialog      by remember { mutableStateOf(false) }
    var showEmailDialog         by remember { mutableStateOf(false) }
    var showGroupRequestDialog  by remember { mutableStateOf(false) }
    var showSaveDialog          by remember { mutableStateOf(false) }
    var isEditing               by remember { mutableStateOf(false) }
    var editedName              by remember { mutableStateOf("") }

    LaunchedEffect(clientId) { viewModel.loadProfile(clientId) }
    LaunchedEffect(uiState.client) {
        if (editedName.isBlank()) editedName = uiState.client?.name ?: ""
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            viewModel.clearSuccess()
            isEditing = false
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            snackbarHostState.showSnackbar(
                message  = "Perfil atualizado!",
                duration = SnackbarDuration.Short
            )
            onProfileUpdated()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.emailSuccess) {
        if (uiState.emailSuccess) {
            snackbarHostState.showSnackbar("E-mail alterado! Faça login com o novo e-mail.")
            viewModel.clearEmailState(); showEmailDialog = false; onNavigateBack()
        }
    }
    LaunchedEffect(uiState.avatarError) {
        uiState.avatarError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAvatarError()
        }
    }

    val currentGroup    = uiState.groups.firstOrNull { it.id == uiState.client?.groupId }
    val currentDivision = uiState.divisions.firstOrNull { it.id == currentGroup?.divisionId }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F6FA)
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.client == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding), Alignment.Center
            ) {
                CircularProgressIndicator(color = WtcBlue)
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Header Hero ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .offset(x = 190.dp, y = (-40).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f))
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .offset(x = 250.dp, y = 70.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 32.dp)
                    ) {
                        // Navegação + ações
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick  = onNavigateBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint     = Color.White,
                                    modifier = Modifier.size(18.dp))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isEditing) {
                                    IconButton(
                                        onClick  = { isEditing = false; editedName = uiState.client?.name ?: "" },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar",
                                            tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick  = { showSaveDialog = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Salvar",
                                            tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(
                                        onClick  = { isEditing = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar",
                                            tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Avatar + nome + email
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .background(Color.White.copy(alpha = 0.20f), CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarPicker(
                                    avatarUrl      = uiState.avatarUrl,
                                    displayName    = uiState.client?.name ?: "",
                                    isUploading    = uiState.isUploadingAvatar,
                                    size           = 70,
                                    onPickImage    = { uri -> viewModel.uploadAvatar(uri, context) },
                                    onDeleteAvatar = { viewModel.deleteAvatar() }
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                if (isEditing) {
                                    OutlinedTextField(
                                        value         = editedName,
                                        onValueChange = { editedName = it },
                                        singleLine    = true,
                                        placeholder   = { Text("Seu nome", color = Color.White.copy(alpha = 0.5f)) },
                                        shape         = RoundedCornerShape(12.dp),
                                        colors        = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor      = Color.White.copy(alpha = 0.7f),
                                            unfocusedBorderColor    = Color.White.copy(alpha = 0.3f),
                                            focusedContainerColor   = Color.White.copy(alpha = 0.10f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                                            focusedTextColor        = Color.White,
                                            unfocusedTextColor      = Color.White,
                                            cursorColor             = Color.White
                                        )
                                    )
                                } else {
                                    val nameParts = (uiState.client?.name ?: "").trim().split(" ")
                                    val firstName  = nameParts.firstOrNull() ?: ""
                                    val lastName   = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

                                    Text(firstName, fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold, color = Color.White,
                                        lineHeight = 26.sp)
                                    if (lastName.isNotBlank()) {
                                        Text(lastName, fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.75f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uiState.client?.email ?: "—",
                                    fontSize = 12.sp,
                                    color    = Color.White.copy(alpha = 0.60f))
                            }
                        }
                    }
                }

                // ── Corpo ─────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Informações da conta ──────────────────────────────────
                    Text("INFORMAÇÕES DA CONTA",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 1.2.sp)

                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column {
                            ProfileInfoItem(
                                icon       = Icons.Default.Email,
                                iconBg     = Color(0xFFE8F4FD),
                                iconTint   = WtcBlue,
                                label      = "E-mail",
                                value      = uiState.client?.email ?: "—",
                                actionText = "Alterar",
                                onAction   = { showEmailDialog = true }
                            )
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 16.dp),
                                color     = Color(0xFFF0F6FA), thickness = 1.dp)
                            ProfileInfoItem(
                                icon     = Icons.Default.Business,
                                iconBg   = Color(0xFFEDF7F2),
                                iconTint = Color(0xFF1A7A5E),
                                label    = "Divisão",
                                value    = currentDivision?.name ?: "Não vinculado",
                                badge    = "só leitura"
                            )
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 16.dp),
                                color     = Color(0xFFF0F6FA), thickness = 1.dp)
                            ProfileInfoItem(
                                icon       = Icons.Default.Group,
                                iconBg     = WtcBlueHint,
                                iconTint   = WtcBlueDark,
                                label      = "Grupo",
                                value      = currentGroup?.name ?: "Não vinculado",
                                actionText = "Solicitar troca",
                                onAction   = { showGroupRequestDialog = true }
                            )
                        }
                    }

                    // ── Segurança ─────────────────────────────────────────────
                    Text("SEGURANÇA",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 1.2.sp)

                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        ProfileInfoItem(
                            icon       = Icons.Default.Lock,
                            iconBg     = Color(0xFFF3EEF8),
                            iconTint   = Color(0xFF6B3FA0),
                            label      = "Senha",
                            value      = "Alterar senha da conta",
                            actionIcon = Icons.Default.ChevronRight,
                            onAction   = { showPasswordDialog = true }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog confirmar salvamento ───────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Confirmar alterações", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text  = { Text("Deseja salvar as alterações no perfil?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        viewModel.updateClientProfile(
                            name            = editedName.ifBlank { uiState.client?.name ?: "" },
                            selectedGroupId = uiState.client?.groupId ?: ""
                        )
                    },
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) { Text("Salvar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isLoading = uiState.isLoading, error = uiState.passwordError,
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
            groups         = uiState.groups,
            divisions      = uiState.divisions,
            currentGroupId = uiState.client?.groupId ?: "",
            onDismiss      = { showGroupRequestDialog = false },
            onConfirm      = { groupId, reason ->
                viewModel.requestGroupChange(groupId, reason)
                showGroupRequestDialog = false
            }
        )
    }
}

// ── ProfileInfoItem — linha de informação rica ────────────────────────────────

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    badge: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null,
                tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, color = TextPrimary,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        }

        when {
            badge != null -> Surface(
                color = WtcBluePale,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(badge, fontSize = 10.sp, color = TextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
            actionText != null -> TextButton(onClick = { onAction?.invoke() }) {
                Text(actionText, fontSize = 13.sp,
                    color = WtcBlue, fontWeight = FontWeight.SemiBold)
            }
            actionIcon != null -> IconButton(
                onClick  = { onAction?.invoke() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(actionIcon, contentDescription = null,
                    tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String, value: String, isReadOnly: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (isReadOnly) {
            Surface(color = WtcBluePale, shape = RoundedCornerShape(4.dp)) {
                Text("só leitura", style = MaterialTheme.typography.labelSmall, color = TextMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    isLoading: Boolean, error: String?, success: Boolean,
    onDismiss: () -> Unit, onConfirm: (current: String, new: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrent     by remember { mutableStateOf(false) }
    var showNew         by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }
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
                    ClientPasswordField(value = currentPassword, onValueChange = { currentPassword = it },
                        label = "Senha atual", visible = showCurrent, onToggleVisibility = { showCurrent = !showCurrent })
                    ClientPasswordField(value = newPassword, onValueChange = { newPassword = it },
                        label = "Nova senha", visible = showNew, onToggleVisibility = { showNew = !showNew })
                    ClientPasswordField(value = confirmPassword, onValueChange = { confirmPassword = it },
                        label = "Confirmar nova senha", visible = showConfirm,
                        onToggleVisibility = { showConfirm = !showConfirm }, isError = passwordMismatch)
                    if (passwordMismatch)
                        Text("As senhas não coincidem.", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    error?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            if (success) {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            } else {
                Button(onClick = { onConfirm(currentPassword, newPassword) },
                    enabled = !isLoading && currentPassword.isNotBlank() &&
                            newPassword.isNotBlank() && !passwordMismatch,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Alterar")
                }
            }
        },
        dismissButton = { if (!success) TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ClientPasswordField(
    value: String, onValueChange: (String) -> Unit,
    label: String, visible: Boolean, onToggleVisibility: () -> Unit, isError: Boolean = false
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        singleLine = true, isError = isError, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
            }
        }
    )
}

@Composable
fun ClientChangeEmailDialog(
    currentEmail: String, isLoading: Boolean, error: String?,
    onDismiss: () -> Unit, onConfirm: (newEmail: String, password: String) -> Unit
) {
    var newEmail      by remember { mutableStateOf("") }
    var confirmEmail  by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    val emailMismatch  = newEmail.isNotBlank() && confirmEmail.isNotBlank() && newEmail != confirmEmail
    val emailUnchanged = newEmail.isNotBlank() && newEmail == currentEmail

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar E-mail", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("E-mail atual: $currentEmail", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(10.dp))
                }
                OutlinedTextField(value = newEmail, onValueChange = { newEmail = it },
                    label = { Text("Novo e-mail") }, singleLine = true,
                    isError = emailMismatch || emailUnchanged,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = confirmEmail, onValueChange = { confirmEmail = it },
                    label = { Text("Confirmar novo e-mail") }, singleLine = true,
                    isError = emailMismatch, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                if (emailMismatch) Text("Os e-mails não coincidem.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                if (emailUnchanged) Text("O novo e-mail é igual ao atual.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                ClientPasswordField(value = password, onValueChange = { password = it },
                    label = "Confirme sua senha", visible = showPassword,
                    onToggleVisibility = { showPassword = !showPassword })
                error?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error) }
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error,
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
            Button(onClick = { onConfirm(newEmail, password) },
                enabled = !isLoading && newEmail.isNotBlank() && !emailMismatch &&
                        !emailUnchanged && password.isNotBlank(),
                shape = RoundedCornerShape(12.dp)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Alterar E-mail")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

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
    val groupsByDivision = groups.groupBy { it.divisionId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar Troca de Grupo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Escolha o grupo desejado. Um operador avaliará sua solicitação.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "Selecione o novo grupo...",
                        onValueChange = {}, readOnly = true, label = { Text("Novo grupo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        isError = selectedGroup?.id == currentGroupId)
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        divisions.forEach { division ->
                            val divGroups = groupsByDivision[division.id] ?: emptyList()
                            if (divGroups.isNotEmpty()) {
                                DropdownMenuItem(text = { Text(division.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = WtcBlue) },
                                    onClick = {}, enabled = false)
                                divGroups.filter { it.id != currentGroupId }.forEach { group ->
                                    DropdownMenuItem(text = { Text("  ${group.name}") },
                                        onClick = { selectedGroup = group; expanded = false })
                                }
                            }
                        }
                    }
                }
                if (selectedGroup?.id == currentGroupId)
                    Text("Este já é seu grupo atual.", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                OutlinedTextField(value = reason, onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") }, modifier = Modifier.fillMaxWidth(),
                    maxLines = 3, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedGroup!!.id, reason) },
                enabled = selectedGroup != null && selectedGroup?.id != currentGroupId,
                shape = RoundedCornerShape(12.dp)) { Text("Enviar Solicitação") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}