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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.ui.components.AvatarPicker
import br.com.fiap.wtcconnecta.viewmodel.OperatorProfileViewModel

private val WtcBlue = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorProfileScreen(
    operatorId: String,
    onNavigateBack: () -> Unit,
    viewModel: OperatorProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(operatorId) { viewModel.loadProfile(operatorId) }
    LaunchedEffect(uiState.operator) {
        if (editedName.isBlank()) editedName = uiState.operator?.name ?: ""
    }
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHostState.showSnackbar("Perfil atualizado!")
            viewModel.clearSuccess(); isEditing = false
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
            snackbarHostState.showSnackbar(it); viewModel.clearAvatarError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color(0xFFF0F6FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
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
                        .size(200.dp)
                        .offset(x = 190.dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = 250.dp, y = 60.dp)
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
                            onClick = onNavigateBack, modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isEditing) {
                                IconButton(
                                    onClick = {
                                        isEditing = false; editedName = uiState.operator?.name ?: ""
                                    }, modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        tint = Color.White,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showSaveDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        tint = Color.White,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { isEditing = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        tint = Color.White,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Avatar + nome no hero
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AvatarPicker(
                            avatarUrl = uiState.avatarUrl,
                            displayName = uiState.operator?.name ?: "",
                            isUploading = uiState.isUploadingAvatar,
                            size = 72,
                            onPickImage = { uri -> viewModel.uploadAvatar(uri, context) },
                            onDeleteAvatar = { viewModel.deleteAvatar() })

                        Column {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = it },
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            "Seu nome", color = Color.White.copy(alpha = 0.5f)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White.copy(alpha = 0.7f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.10f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White
                                    )
                                )
                            } else {
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "Operador",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp, vertical = 3.dp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    uiState.operator?.name ?: "—",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 24.sp
                                )
                                Text(
                                    uiState.operator?.email ?: "—",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Corpo ─────────────────────────────────────────────────────────
            when {
                uiState.isLoading && uiState.operator == null -> Box(
                    Modifier
                        .fillMaxSize()
                        .height(300.dp), Alignment.Center
                ) {
                    CircularProgressIndicator(color = WtcBlue)
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Informações da conta ──────────────────────────────────
                    Text(
                        "INFORMAÇÕES DA CONTA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(WtcBluePale), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    null,
                                    tint = WtcBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("E-mail", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    uiState.operator?.email ?: "—",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                            TextButton(
                                onClick = { showEmailDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    "Alterar",
                                    fontSize = 13.sp,
                                    color = WtcBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Segurança ─────────────────────────────────────────────
                    Text(
                        "SEGURANÇA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.2.sp
                    )

                    Card(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(WtcBluePale), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    null,
                                    tint = WtcBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Senha",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    "Alterar senha da conta",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(WtcBlueHint), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = WtcBlueDark,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    "Confirmar alterações", fontWeight = FontWeight.Bold, color = TextPrimary
                )
            },
            text = { Text("Deseja salvar as alterações no perfil?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        viewModel.updateName(editedName.ifBlank { uiState.operator?.name ?: "" })
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) { Text("Salvar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            })
    }

    if (showPasswordDialog) {
        OperatorChangePasswordDialog(
            isLoading = uiState.isLoading,
            error = uiState.passwordError,
            success = uiState.passwordSuccess,
            onDismiss = { showPasswordDialog = false; viewModel.clearPasswordState() },
            onConfirm = { current, new -> viewModel.changePassword(current, new) })
    }

    if (showEmailDialog) {
        OperatorChangeEmailDialog(
            currentEmail = uiState.operator?.email ?: "",
            isLoading = uiState.isLoading,
            error = uiState.emailError,
            onDismiss = { showEmailDialog = false; viewModel.clearEmailState() },
            onConfirm = { newEmail, password -> viewModel.changeEmail(newEmail, password) })
    }
}

// ── PasswordField ─────────────────────────────────────────────────────────────

@Composable
fun PasswordField(
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
                    contentDescription = null,
                    tint = WtcBlueHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint, cursorColor = WtcBlue
        )
    )
}

// ── Dialog alterar senha ──────────────────────────────────────────────────────

@Composable
fun OperatorChangePasswordDialog(
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    val mismatch = newPass.isNotBlank() && confirm.isNotBlank() && newPass != confirm

    LaunchedEffect(success) { if (success) onDismiss() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar Senha", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    current,
                    { current = it },
                    "Senha atual",
                    showCurrent,
                    { showCurrent = !showCurrent })
                PasswordField(
                    newPass,
                    { newPass = it },
                    "Nova senha",
                    showNew,
                    { showNew = !showNew })
                PasswordField(
                    confirm,
                    { confirm = it },
                    "Confirmar nova senha",
                    showConfirm,
                    { showConfirm = !showConfirm },
                    isError = mismatch
                )
                if (mismatch) Text(
                    "As senhas não coincidem.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
                error?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(current, newPass) },
                enabled = !isLoading && current.isNotBlank() && newPass.length >= 6 && !mismatch,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White
                )
                else Text("Alterar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        })
}

// ── Dialog alterar email ──────────────────────────────────────────────────────

@Composable
fun OperatorChangeEmailDialog(
    currentEmail: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var confirmEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val emailMismatch =
        newEmail.isNotBlank() && confirmEmail.isNotBlank() && newEmail != confirmEmail
    val emailUnchanged = newEmail.isNotBlank() && newEmail == currentEmail

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar E-mail", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = WtcBluePale,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "E-mail atual: $currentEmail",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Novo e-mail") },
                    singleLine = true,
                    isError = emailMismatch || emailUnchanged,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint
                    )
                )
                OutlinedTextField(
                    value = confirmEmail,
                    onValueChange = { confirmEmail = it },
                    label = { Text("Confirmar novo e-mail") },
                    singleLine = true,
                    isError = emailMismatch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint
                    )
                )
                if (emailMismatch) Text(
                    "Os e-mails não coincidem.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
                if (emailUnchanged) Text(
                    "O novo e-mail é igual ao atual.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Confirme sua senha",
                    visible = showPassword,
                    onToggleVisibility = { showPassword = !showPassword })
                error?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Você será desconectado e precisará fazer login com o novo e-mail.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newEmail, password) },
                enabled = !isLoading && newEmail.isNotBlank() && !emailMismatch && !emailUnchanged && password.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                if (isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White
                )
                else Text("Alterar E-mail", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        })
}