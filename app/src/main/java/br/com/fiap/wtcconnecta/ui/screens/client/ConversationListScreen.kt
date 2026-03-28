package br.com.fiap.wtcconnecta.ui.screens.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5FAFD)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(WtcBlue, WtcBlueSoft)))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        "Minhas Conversas",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Selecione uma conversa para continuar",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
                ) {
                    // Seção 1:1
                    item {
                        Text(
                            "ATENDIMENTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    item {
                        val realConversationId = viewModel.getConversationId(clientId)
                        val lastMsg = uiState.lastMessages[realConversationId]
                            ?: uiState.lastMessages[clientId]
                        val lastMessageText = lastMsg?.content ?: "Clique para ver seu chat privado..."

                        AnimatedVisibility(visible = true) {
                            ConversationCard(
                                name        = "Atendimento WTC",
                                lastMessage = lastMessageText,
                                icon        = Icons.Default.Person,
                                accent      = WtcBlue,
                                bgAccent    = WtcBluePale,
                                onClick     = {
                                    onNavigateToChat(realConversationId, "Atendimento WTC", "1on1")
                                }
                            )
                        }
                    }

                    // Seção Grupos
                    if (uiState.groups.isNotEmpty()) {
                        item {
                            Text(
                                "GRUPOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                            )
                        }

                        items(uiState.groups) { group ->
                            val lastMessageText = uiState.lastMessages[group.id]?.content
                                ?: "Clique para ver as mensagens do grupo..."
                            AnimatedVisibility(visible = true) {
                                ConversationCard(
                                    name        = group.name,
                                    lastMessage = lastMessageText,
                                    icon        = Icons.Default.Group,
                                    accent      = Color(0xFF063D5C),
                                    bgAccent    = WtcBlueHint,
                                    onClick     = { onNavigateToChat(group.id, group.name, "group") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(lastMessage, fontSize = 12.sp, color = TextMuted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
            }
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