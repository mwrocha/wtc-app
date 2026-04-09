package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.remote.PendingConversation
import br.com.fiap.wtcconnecta.viewmodel.AttendanceQueueViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceQueueScreen(
    onBack: () -> Unit,
    onAssumeAndNavigate: (clientId: String, clientName: String) -> Unit,
    viewModel: AttendanceQueueViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.load() }

    // Ao assumir com sucesso, navega direto para o ClientDetailScreen
    LaunchedEffect(uiState.assumeSuccess) {
        uiState.assumeSuccess?.let {
            val clientId   = uiState.assumedClientId   ?: ""
            val clientName = uiState.assumedClientName ?: "Cliente"
            if (clientId.isNotBlank()) {
                onAssumeAndNavigate(clientId, clientName)
            }
            viewModel.clearAssumeSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5FAFD)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(WtcBlue, WtcBlueSoft)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Fila de Atendimento", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (uiState.pendingConversations.isEmpty()) "Nenhuma conversa pendente"
                            else "${uiState.pendingConversations.size} aguardando atendimento",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.pendingConversations.isEmpty() -> Box(
                    Modifier.fillMaxSize(), Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(WtcBluePale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = WtcBlue, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tudo em dia!", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Nenhuma conversa aguardando atendimento.",
                            fontSize = 13.sp, color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
                ) {
                    items(uiState.pendingConversations) { conv ->
                        PendingConversationCard(
                            conversation = conv,
                            isAssuming   = uiState.isAssuming,
                            onAssume     = { viewModel.assumeConversation(conv.conversationId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingConversationCard(
    conversation: PendingConversation,
    isAssuming: Boolean,
    onAssume: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar com inicial
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(WtcBluePale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        conversation.clientName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WtcBlue
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(conversation.clientName, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(conversation.clientEmail, fontSize = 11.sp, color = TextMuted)
                }
                // Badge AGUARDANDO
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(Color(0xFFE65100)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aguardando", fontSize = 10.sp,
                            color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preview da última mensagem
            if (conversation.lastMessagePreview.isNotBlank()) {
                Surface(
                    color = WtcBluePale,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "\"${conversation.lastMessagePreview}\"",
                        fontSize = 12.sp, color = TextMuted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tempo aguardando
            if (conversation.updatedAt.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null,
                        tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Última mensagem: ${formatQueueTime(conversation.updatedAt)}",
                        fontSize = 11.sp, color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Botão assumir
            Button(
                onClick   = onAssume,
                enabled   = !isAssuming,
                modifier  = Modifier.fillMaxWidth().height(44.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = WtcBlue)
            ) {
                if (isAssuming) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.HeadsetMic, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assumir atendimento", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatQueueTime(dateStr: String): String {
    return try {
        // "2026-03-30T14:43:41" → "14:43"
        val timePart = dateStr.substringAfter("T").take(5)
        val parts = timePart.split(":")
        if (parts.size >= 2) "${parts[0].padStart(2,'0')}:${parts[1].padStart(2,'0')}"
        else timePart
    } catch (_: Exception) { "" }
}