package br.com.fiap.wtcconnecta.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.R
import br.com.fiap.wtcconnecta.viewmodel.LoginResult
import br.com.fiap.wtcconnecta.viewmodel.LoginViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val WtcBlueDark = Color(0xFF9EC3DC)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (result: LoginResult) -> Unit,
    onNavigateToRegister: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState         by loginViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loginResult) {
        uiState.loginResult?.let {
            onLoginSuccess(it)
            loginViewModel.onLoginHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
    ) {
        // Detalhes geométricos decorativos
        Box(
            modifier = Modifier.size(280.dp).offset(x = 180.dp, y = (-60).dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.04f))
        )
        Box(
            modifier = Modifier.size(180.dp).offset(x = (-60).dp, y = 100.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.05f))
        )
        Box(
            modifier = Modifier.size(120.dp).offset(x = 260.dp, y = 500.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Logo
            Image(
                painter            = painterResource(id = R.drawable.logo_login),
                contentDescription = "WTC Connecta",
                modifier           = Modifier.size(130.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "WTC Connecta",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                letterSpacing = 0.3.sp
            )
            Text(
                "Conectando você ao que importa",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Card do formulário
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(top = 32.dp, bottom = 24.dp)
                ) {
                    Text("Bem-vindo de volta", fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Entre com sua conta para continuar", fontSize = 13.sp,
                        color = TextMuted, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))

                    // E-mail
                    Text("E-mail", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it; if (uiState.error != null) loginViewModel.clearError() },
                        placeholder   = { Text("seu@email.com", color = TextMuted.copy(alpha = 0.6f)) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError       = uiState.error != null,
                        leadingIcon   = {
                            Icon(Icons.Default.Email, contentDescription = null,
                                tint = if (email.isNotBlank()) WtcBlue else WtcBlueHint,
                                modifier = Modifier.size(18.dp))
                        },
                        shape  = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = WtcBlue,
                            unfocusedBorderColor    = WtcBlueHint,
                            cursorColor             = WtcBlue,
                            focusedContainerColor   = WtcBluePale.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color(0xFFFAFCFE)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Senha
                    Text("Senha", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it; if (uiState.error != null) loginViewModel.clearError() },
                        placeholder   = { Text("••••••••", color = TextMuted.copy(alpha = 0.6f)) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        isError       = uiState.error != null,
                        leadingIcon   = {
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
                                    tint     = WtcBlueHint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape  = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = WtcBlue,
                            unfocusedBorderColor    = WtcBlueHint,
                            cursorColor             = WtcBlue,
                            focusedContainerColor   = WtcBluePale.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color(0xFFFAFCFE)
                        )
                    )

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick  = { loginViewModel.login(email, password) },
                        enabled  = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = WtcBlue,
                            disabledContainerColor = WtcBlueHint
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Entrar", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Não tem uma conta?", color = TextMuted, fontSize = 13.sp)
                        TextButton(onClick = onNavigateToRegister,
                            contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text("Cadastre-se", color = WtcBlue, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}