package br.com.fiap.wtcconnecta.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.viewmodel.RegisterViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    registerViewModel: RegisterViewModel = viewModel()
) {
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole    by remember { mutableStateOf("CLIENT") }
    val uiState         by registerViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            onRegisterSuccess()
            registerViewModel.onRegisterHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFD))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(colors = listOf(WtcBlue, WtcBlueSoft))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToLogin,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Criar conta", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Preencha os dados abaixo", fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.72f))
                    }
                }
            }

            // Formulário
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nome
                RegisterField(
                    label = "Nome completo",
                    value = name,
                    onValueChange = { name = it; if (uiState.error != null) registerViewModel.clearError() },
                    placeholder = "Seu nome completo",
                    icon = Icons.Default.Person,
                    isError = uiState.error != null
                )

                // Email
                RegisterField(
                    label = "E-mail",
                    value = email,
                    onValueChange = { email = it; if (uiState.error != null) registerViewModel.clearError() },
                    placeholder = "seu@email.com",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    isError = uiState.error != null
                )

                // Senha
                Column {
                    Text("Senha", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; if (uiState.error != null) registerViewModel.clearError() },
                        placeholder = { Text("••••••••", color = TextMuted.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.error != null,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null,
                                tint = if (password.isNotBlank()) WtcBlue else WtcBlueHint,
                                modifier = Modifier.size(18.dp))
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility
                                    else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = WtcBlueHint, modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors()
                    )
                }

                // Tipo de conta
                Column {
                    Text("Tipo de conta", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("CLIENT" to "Cliente", "OPERATOR" to "Operador").forEach { (role, label) ->
                            val selected = selectedRole == role
                            Card(
                                onClick = { selectedRole = role },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) WtcBlue else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    if (selected) 0.dp else 2.dp
                                ),
                                border = if (!selected) androidx.compose.foundation.BorderStroke(
                                    1.dp, WtcBlueHint
                                ) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { selectedRole = role },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color.White,
                                            unselectedColor = WtcBlue
                                        ),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(label, fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else TextPrimary)
                                }
                            }
                        }
                    }
                }

                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { registerViewModel.register(name, email, password, selectedRole) },
                    enabled = !uiState.isLoading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WtcBlue,
                        disabledContainerColor = WtcBlueHint
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Criar conta", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Já tem uma conta?", color = TextMuted, fontSize = 13.sp)
                    TextButton(onClick = onBackToLogin,
                        contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Entrar", color = WtcBlue, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted.copy(alpha = 0.6f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            leadingIcon = {
                Icon(icon, contentDescription = null,
                    tint = if (value.isNotBlank()) WtcBlue else WtcBlueHint,
                    modifier = Modifier.size(18.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = WtcBlue,
    unfocusedBorderColor    = WtcBlueHint,
    focusedLabelColor       = WtcBlue,
    cursorColor             = WtcBlue,
    focusedContainerColor   = WtcBluePale.copy(alpha = 0.4f),
    unfocusedContainerColor = Color(0xFFFAFCFE)
)