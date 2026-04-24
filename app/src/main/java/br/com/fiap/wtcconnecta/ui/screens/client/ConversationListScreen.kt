package br.com.fiap.wtcconnecta.ui.screens.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.viewmodel.ConversationListViewModel
import kotlinx.coroutines.flow.collectLatest

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val WtcBlueDark = Color(0xFF063D5C)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    clientId: String,
    onNavigateToChat: (chatId: String, chatName: String, chatType: String) -> Unit,
    viewModel: ConversationListViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.newMessageEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(clientId) { viewModel.loadClientData(clientId) }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F6FA)
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
                        .padding(top = 28.dp, bottom = 32.dp)
                ) {
                    Text(
                        "Minhas Conversas",
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        lineHeight = 30.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Selecione uma conversa para continuar",
                        fontSize     = 13.sp,
                        color        = Color.White.copy(alpha = 0.65f),
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Pill de status online
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Text(
                                "WTC Connecta online",
                                fontSize   = 11.sp,
                                color      = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Lista ─────────────────────────────────────────────────────────
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Atendimento ───────────────────────────────────────────
                    item {
                        Text(
                            "ATENDIMENTO",
                            fontSize     = 11.sp,
                            fontWeight   = FontWeight.Bold,
                            color        = TextMuted,
                            letterSpacing = 1.2.sp,
                            modifier     = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        val realConversationId = viewModel.getConversationId(clientId)
                        val operatorEmail      = viewModel.getOperatorEmail()
                        val chatId = if (realConversationId.contains("@")) realConversationId
                        else operatorEmail ?: realConversationId
                        val lastMsg = uiState.lastMessages[realConversationId]
                            ?: uiState.lastMessages[clientId]
                        val lastMessageText = lastMsg?.content
                            ?: if (operatorEmail != null) "Toque para iniciar uma conversa..."
                            else "Clique para ver seu chat privado..."

                        // Card principal de atendimento — gradiente azul
                        Card(
                            onClick   = { onNavigateToChat(chatId, "Atendimento WTC", "1on1") },
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors    = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.linearGradient(listOf(WtcBlue, WtcBlueSoft)))
                                    .padding(horizontal = 22.dp, vertical = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 16.dp)
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
                                        Icon(Icons.Default.Person, contentDescription = null,
                                            tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Atendimento WTC", fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(lastMessageText, fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.72f),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 3.dp))
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // ── Grupos ────────────────────────────────────────────────
                    if (uiState.groups.isNotEmpty()) {
                        item {
                            Text(
                                "GRUPOS",
                                fontSize     = 11.sp,
                                fontWeight   = FontWeight.Bold,
                                color        = TextMuted,
                                letterSpacing = 1.2.sp,
                                modifier     = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.groups) { group ->
                            val lastMessageText = uiState.lastMessages[group.id]?.content
                                ?: "Clique para ver as mensagens do grupo..."
                            GroupConversationCard(
                                name        = group.name,
                                lastMessage = lastMessageText,
                                onClick     = { onNavigateToChat(group.id, group.name, "group") }
                            )
                        }
                    }

                    // Rodapé
                    item {
                        Text(
                            "WTC Connecta • Comunicação corporativa",
                            fontSize  = 11.sp,
                            color     = TextMuted.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Card de grupo ─────────────────────────────────────────────────────────────

@Composable
fun GroupConversationCard(
    name: String,
    lastMessage: String,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WtcBlueHint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null,
                    tint = WtcBlueDark, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(lastMessage, fontSize = 12.sp, color = TextMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp))
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(WtcBlueHint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = WtcBlueDark, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ── ConversationCard mantido para compatibilidade ─────────────────────────────

@Composable
fun ConversationCard(
    name: String,
    lastMessage: String,
    icon: ImageVector,
    accent: Color,
    bgAccent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(bgAccent),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(lastMessage, fontSize = 12.sp, color = TextMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
            }
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(bgAccent),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}