package br.com.fiap.wtcconnecta.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeClientScreen(
    clientId: String,
    clientName: String = "",
    onNavigateToConversationList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCampaigns: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val firstName = clientName.split(" ").firstOrNull() ?: ""
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFD))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header com gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(WtcBlue, WtcBlueSoft)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            firstName.firstOrNull()?.uppercase() ?: "C",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (firstName.isNotBlank()) "Olá, $firstName 👋" else "Olá! 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Bem-vindo ao WTC Connecta",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }

                    // Botão logout
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Conteúdo
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "O que você precisa?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ClientNavCard(
                    title    = "Minhas Conversas",
                    subtitle = "Chats 1:1 e mensagens de grupo",
                    icon     = Icons.Default.Chat,
                    accent   = WtcBlue,
                    bgAccent = WtcBluePale,
                    onClick  = onNavigateToConversationList
                )

                ClientNavCard(
                    title    = "Campanhas Express",
                    subtitle = "Comunicados e promoções exclusivas",
                    icon     = Icons.Default.Campaign,
                    accent   = Color(0xFF1A6E9A),
                    bgAccent = Color(0xFFDEEFF7),
                    onClick  = onNavigateToCampaigns
                )

                ClientNavCard(
                    title    = "Meu Perfil",
                    subtitle = "Dados cadastrais e configurações",
                    icon     = Icons.Default.Person,
                    accent   = Color(0xFF063D5C),
                    bgAccent = Color(0xFFD0E8F2),
                    onClick  = onNavigateToProfile
                )
            }
        }
    }

    // ── Dialog de confirmação de logout ───────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Sair da conta", fontWeight = FontWeight.Bold, color = Color(0xFF0D2B3E))
            },
            text = {
                Text("Tem certeza que deseja sair?", color = Color(0xFF6E90A0))
            },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B537B)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Sair", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color(0xFF6E90A0))
                }
            }
        )
    }
}

@Composable
fun ClientNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    bgAccent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(bgAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = accent, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp))
            }

            // Indicador sutil
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(bgAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}