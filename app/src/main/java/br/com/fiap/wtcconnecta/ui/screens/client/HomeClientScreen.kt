package br.com.fiap.wtcconnecta.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.viewmodel.ProfileViewModel
import coil.compose.AsyncImage
import java.util.Calendar

private val WtcBlue = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val WtcBlueDark = Color(0xFF063D5C)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted = Color(0xFF6E90A0)

private fun greetingByHour(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeClientScreen(
    clientId: String,
    clientName: String = "",
    onNavigateToConversationList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCampaigns: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},   // ← novo
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var unreadCampaignCount by remember { mutableIntStateOf(0) }
    val profileUiState by profileViewModel.uiState.collectAsState()
    val avatarUrl = profileUiState.avatarUrl

    val displayName = remember(profileUiState.client, clientName) {
        profileUiState.client?.name?.takeIf { it.isNotBlank() } ?: clientName
    }

    val nameParts = remember(displayName) { displayName.trim().split(" ") }
    val firstName = remember(nameParts) { nameParts.firstOrNull() ?: "" }
    val lastName = remember(nameParts) {
        if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""
    }


    LaunchedEffect(clientId) {
        profileViewModel.loadProfile(clientId)
        // Polling de mensagens não lidas a cada 30 segundos
        while (true) {
            try {
                val resp = RetrofitClient.instance.getUnreadCount()
                unreadCount = resp.count
            } catch (_: Exception) {
            }
            try {
                val resp = RetrofitClient.instance.getUnreadCampaignCount()
                unreadCampaignCount = resp.count
            } catch (_: Exception) {
            }
            kotlinx.coroutines.delay(30_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F6FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header Hero ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft))
                    )
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .offset(x = 180.dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 240.dp, y = 60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            greetingByHour(),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.70f),
                            letterSpacing = 0.5.sp
                        )
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Sair",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Avatar + nome
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrBlank() && avatarUrl.startsWith("http")) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase()
                                        ?: firstName.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                if (firstName.isNotBlank()) firstName else "Cliente",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 30.sp
                            )
                            if (lastName.isNotBlank()) {
                                Text(
                                    lastName,
                                    fontSize = 15.sp,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Conectando você ao que importa",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // ── Corpo ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "ACESSO RÁPIDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Card principal — Conversas (destaque)
                PrimaryNavCard(
                    title = "Minhas Conversas",
                    subtitle = "Chats 1:1 e mensagens de grupo",
                    icon = Icons.Default.Chat,
                    gradient = Brush.linearGradient(listOf(WtcBlue, WtcBlueSoft)),
                    onClick = onNavigateToConversationList,
                    badgeCount = unreadCount
                )

                // Cards secundários lado a lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SecondaryNavCard(
                        title = "Campanhas",
                        subtitle = "Ofertas\nexclusivas",
                        icon = Icons.Default.Campaign,
                        accent = WtcBlueSoft,
                        bgAccent = Color(0xFFDEEFF7),
                        onClick = onNavigateToCampaigns,
                        modifier = Modifier.weight(1f),
                        badgeCount = unreadCampaignCount
                    )
                    SecondaryNavCard(
                        title = "Meu Perfil",
                        subtitle = "Dados e\nconfigurações",
                        icon = Icons.Default.Person,
                        accent = WtcBlueDark,
                        bgAccent = WtcBlueHint,
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Card Meus Atendimentos ────────────────────────────────────
                ClientNavCard(
                    title = "Meus Atendimentos",
                    subtitle = "Histórico de atendimentos encerrados",
                    icon = Icons.Default.HeadsetMic,
                    accent = Color(0xFF1A7A5E),
                    bgAccent = Color(0xFFEDF7F2),
                    onClick = onNavigateToHistory
                )

                // ── Card Galeria ──────────────────────────────────────────────
                ClientNavCard(
                    title = "Galeria",
                    subtitle = "Imagens e documentos trocados",
                    icon = Icons.Default.PhotoLibrary,
                    accent = WtcBlue,
                    bgAccent = WtcBluePale,
                    onClick = onNavigateToGallery
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "WTC Connecta • Sua conexão com o mundo corporativo",
                    fontSize = 11.sp,
                    color = TextMuted.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }

    // ── Dialog logout ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Tem certeza que deseja sair?", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Sair", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            })
    }
}

// ── Card principal (largo, gradiente azul) ────────────────────────────────────

@Composable
fun PrimaryNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Badge de não lidas
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Card secundário (metade da largura) ───────────────────────────────────────

@Composable
fun SecondaryNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    bgAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(bgAccent), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(bgAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── ClientNavCard mantido para compatibilidade ────────────────────────────────

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
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(bgAccent), contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(bgAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}