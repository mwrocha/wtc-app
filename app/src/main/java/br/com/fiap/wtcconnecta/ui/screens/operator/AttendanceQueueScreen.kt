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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.remote.PendingConversation
import br.com.fiap.wtcconnecta.viewmodel.AttendanceQueueViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)
private val ColorActive = Color(0xFF1A7A5E)
private val ColorQueue  = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceQueueScreen(
    onBack: () -> Unit,
    onAssumeAndNavigate: (clientId: String, clientName: String) -> Unit,
    onNavigateToActive: (clientId: String, clientName: String) -> Unit = { _, _ -> },
    viewModel: AttendanceQueueViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(uiState.assumeSuccess) {
        uiState.assumeSuccess?.let {
            val clientId   = uiState.assumedClientId   ?: ""
            val clientName = uiState.assumedClientName ?: "Cliente"
            if (clientId.isNotBlank()) onAssumeAndNavigate(clientId, clientName)
            viewModel.clearAssumeSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val totalActive  = uiState.activeConversations.size
    val totalPending = uiState.pendingConversations.size

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF0F6FA)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Header Hero ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(WtcBlueDark, WtcBlue, WtcBlueSoft)))
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier.size(180.dp).offset(x = 200.dp, y = (-30).dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.04f))
                )
                Box(
                    modifier = Modifier.size(110.dp).offset(x = 260.dp, y = 40.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.06f))
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 28.dp)
                ) {
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier.size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Atendimentos", fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Gerencie seus atendimentos em andamento",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pills de contagem
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (totalActive > 0) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                                        .background(Color(0xFF4CAF50)))
                                    Text("$totalActive em andamento",
                                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.90f),
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (totalPending > 0) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                                        .background(ColorQueue))
                                    Text("$totalPending aguardando",
                                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.90f),
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                totalActive == 0 && totalPending == 0 -> Box(
                    Modifier.fillMaxSize(), Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                                .background(WtcBluePale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = WtcBlue, modifier = Modifier.size(36.dp))
                        }
                        Text("Tudo em dia!", fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Nenhum atendimento pendente ou ativo.",
                            fontSize = 13.sp, color = TextMuted)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // ── Seção: Meus Atendimentos ──────────────────────────────
                    if (totalActive > 0) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text("MEUS ATENDIMENTOS", fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorActive, letterSpacing = 1.2.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = ColorActive.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("$totalActive", fontSize = 10.sp,
                                        color = ColorActive, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                        items(uiState.activeConversations) { conv ->
                            ActiveConversationCard(
                                conversation = conv,
                                onClick = { onNavigateToActive(conv.clientId, conv.clientName) }
                            )
                        }
                    }

                    // ── Seção: Fila de Espera ─────────────────────────────────
                    if (totalPending > 0) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    top = if (totalActive > 0) 12.dp else 0.dp,
                                    bottom = 4.dp
                                )
                            ) {
                                Text("FILA DE ESPERA", fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorQueue, letterSpacing = 1.2.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = ColorQueue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("$totalPending", fontSize = 10.sp,
                                        color = ColorQueue, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
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
}

// ── Card: Atendimento ativo (IN_PROGRESS) ─────────────────────────────────────

@Composable
fun ActiveConversationCard(
    conversation: PendingConversation,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFEDF7F2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    conversation.clientName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorActive
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.clientName, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(conversation.clientEmail, fontSize = 11.sp, color = TextMuted)
                if (conversation.lastMessagePreview.isNotBlank()) {
                    Text(
                        conversation.lastMessagePreview,
                        fontSize = 12.sp, color = TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = ColorActive.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(ColorActive))
                        Text("Ativo", fontSize = 10.sp,
                            color = ColorActive, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(WtcBlueHint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = WtcBlueDark, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── Card: Conversa pendente (OPEN) ────────────────────────────────────────────

@Composable
fun PendingConversationCard(
    conversation: PendingConversation,
    isAssuming: Boolean,
    onAssume: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                        .background(WtcBlueHint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        conversation.clientName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WtcBlueDark
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(conversation.clientName, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(conversation.clientEmail, fontSize = 11.sp, color = TextMuted)
                }
                Surface(
                    color = ColorQueue.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(ColorQueue))
                        Text("Aguardando", fontSize = 10.sp,
                            color = ColorQueue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (conversation.lastMessagePreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color    = Color(0xFFF5FAFD),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "\"${conversation.lastMessagePreview}\"",
                        fontSize = 12.sp, color = TextMuted,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (conversation.updatedAt.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null,
                        tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Última mensagem: ${formatQueueTime(conversation.updatedAt)}",
                        fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick  = onAssume,
                enabled  = !isAssuming,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = WtcBlue)
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
        val timePart = dateStr.substringAfter("T").take(5)
        val parts    = timePart.split(":")
        if (parts.size >= 2) "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
        else timePart
    } catch (_: Exception) { "" }
}