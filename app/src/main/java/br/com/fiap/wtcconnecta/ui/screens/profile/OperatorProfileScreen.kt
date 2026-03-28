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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.viewmodel.OperatorProfileViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorProfileScreen(
    operatorId: String,
    onNavigateBack: () -> Unit,
    viewModel: OperatorProfileViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    var isEditing         by remember { mutableStateOf(false) }
    var editedName        by remember { mutableStateOf("") }
    var showSaveDialog    by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog   by remember { mutableStateOf(false) }
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
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                                tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Meu Perfil", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    // Ações editar/salvar/cancelar
                    Row {
                        if (isEditing) {
                            IconButton(onClick = { isEditing = false; editedName = uiState.operator?.name ?: "" },
                                modifier = Modifier.size(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))) {
                                Icon(Icons.Default.Close, tint = Color.White,
                                    contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showSaveDialog = true },
                                modifier = Modifier.size(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))) {
                                Icon(Icons.Default.Check, tint = Color.White,
                                    contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(onClick = { isEditing = true },
                                modifier = Modifier.size(36.dp)
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))) {
                                Icon(Icons.Default.Edit, tint = Color.White,
                                    contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card avatar + nome
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(WtcBluePale), contentAlignment = Alignment.Center) {
                                Text(uiState.operator?.name?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = WtcBlue)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            if (isEditing) {
                                OutlinedTextField(value = editedName, onValueChange = { editedName = it },
                                    label = { Text("Nome") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                    leadingIcon = { Icon(Icons.Default.Person, null,
                                        tint = WtcBlue, modifier = Modifier.size(18.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
                                        cursorColor = WtcBlue))
                            } else {
                                Text(uiState.operator?.name ?: "—", fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uiState.operator?.email ?: "—", fontSize = 13.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(color = WtcBluePale, shape = RoundedCornerShape(20.dp)) {
                                    Text("Operador", fontSize = 11.sp, color = WtcBlue,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // Seção conta
                    Text("INFORMAÇÕES DA CONTA", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 0.8.sp)

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                                .background(WtcBluePale), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Email, null, tint = WtcBlue,
                                    modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("E-mail", fontSize = 11.sp, color = TextMuted)
                                Text(uiState.operator?.email ?: "—", fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                            TextButton(onClick = { showEmailDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Alterar", fontSize = 13.sp, color = WtcBlue,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Seção segurança
                    Text("SEGURANÇA", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 0.8.sp)

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                                .background(WtcBluePale), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lock, null, tint = WtcBlue,
                                    modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Senha", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    color = TextPrimary)
                                Text("Alterar senha da conta", fontSize = 12.sp, color = TextMuted)
                            }
                            IconButton(onClick = { showPasswordDialog = true }) {
                                Icon(Icons.Default.ChevronRight, null, tint = WtcBlueHint)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Confirmar alterações", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text  = { Text("Deseja salvar as alterações no perfil?", color = TextMuted) },
            confirmButton = {
                Button(onClick = { showSaveDialog = false
                    viewModel.updateName(editedName.ifBlank { uiState.operator?.name ?: "" }) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) {
                Text("Cancelar", color = TextMuted) } }
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
        ChangeEmailDialog(
            currentEmail = uiState.operator?.email ?: "",
            isLoading    = uiState.isLoading,
            error        = uiState.emailError,
            onDismiss    = { showEmailDialog = false; viewModel.clearEmailState() },
            onConfirm    = { newEmail, password -> viewModel.changeEmail(newEmail, password) }
        )
    }
}

// ── PasswordField ─────────────────────────────────────────────────────────────

@Composable
fun PasswordField(
    value: String, onValueChange: (String) -> Unit,
    label: String, visible: Boolean, onToggleVisibility: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) }, singleLine = true, isError = isError,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null, tint = WtcBlueHint, modifier = Modifier.size(18.dp))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint, cursorColor = WtcBlue)
    )
}

// ── Dialog alterar email ──────────────────────────────────────────────────────

@Composable
fun ChangeEmailDialog(
    currentEmail: String, isLoading: Boolean, error: String?,
    onDismiss: () -> Unit, onConfirm: (String, String) -> Unit
) {
    var newEmail      by remember { mutableStateOf("") }
    var confirmEmail  by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    val emailMismatch  = newEmail.isNotBlank() && confirmEmail.isNotBlank() && newEmail != confirmEmail
    val emailUnchanged = newEmail.isNotBlank() && newEmail == currentEmail

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar E-mail", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = WtcBluePale, shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("E-mail atual: $currentEmail", fontSize = 12.sp, color = TextMuted,
                        modifier = Modifier.padding(10.dp))
                }
                OutlinedTextField(value = newEmail, onValueChange = { newEmail = it },
                    label = { Text("Novo e-mail") }, singleLine = true,
                    isError = emailMismatch || emailUnchanged,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint))
                OutlinedTextField(value = confirmEmail, onValueChange = { confirmEmail = it },
                    label = { Text("Confirmar novo e-mail") }, singleLine = true,
                    isError = emailMismatch, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint))
                if (emailMismatch) Text("Os e-mails não coincidem.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error)
                if (emailUnchanged) Text("O novo e-mail é igual ao atual.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error)
                PasswordField(value = password, onValueChange = { password = it },
                    label = "Confirme sua senha", visible = showPassword,
                    onToggleVisibility = { showPassword = !showPassword })
                error?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp).padding(top = 1.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Você será desconectado e precisará fazer login com o novo e-mail.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newEmail, password) },
                enabled = !isLoading && newEmail.isNotBlank() && !emailMismatch &&
                        !emailUnchanged && password.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp, color = Color.White)
                else Text("Alterar E-mail", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } }
    )
}