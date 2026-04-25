package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.viewmodel.AttendanceQueueViewModel
import br.com.fiap.wtcconnecta.viewmodel.HomeOperatorViewModel
import java.util.Calendar

// ── Stats de atendimento do operador ─────────────────────────────────────────
data class MyAttendanceStats(
    val active: Int = 0,
    val today: Int = 0,
    val thisMonth: Int = 0
)

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

private fun greetingByHour(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11  -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else      -> "Boa noite"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorDashboardScreen(
    operatorName: String = "",
    onViewClients: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCampaigns: () -> Unit = {},
    onNavigateToKanban: () -> Unit = {},
    onNavigateToGroupManagement: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    onNavigateToGroupRequests: () -> Unit = {},
    onNavigateToAttendanceQueue: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeOperatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val queueViewModel: AttendanceQueueViewModel = viewModel()
    val queueState by queueViewModel.uiState.collectAsState()

    var pendingRequestsCount by remember { mutableIntStateOf(0) }
    var attendanceStats      by remember { mutableStateOf(MyAttendanceStats()) }
    var operatorAvatarUrl    by remember { mutableStateOf<String?>(null) }
    var ratingAverage        by remember { mutableStateOf<Double?>(null) }
    var ratingTotal          by remember { mutableIntStateOf(0) }
    var showGroupMessageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog       by remember { mutableStateOf(false) }

    val firstName = operatorName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Operador"

    LaunchedEffect(Unit) {
        viewModel.retryFetch()
        queueViewModel.load()
        queueViewModel.startPolling()
        // Busca avatar do operador uma vez ao entrar
        try {
            val resp = RetrofitClient.instance.getMyAvatar()
            if (resp.isSuccessful) {
                val url = resp.body()?.get("url") as? String
                if (!url.isNullOrBlank()) operatorAvatarUrl = url
            }
        } catch (_: Exception) {}
        while (true) {
            try {
                val requests = RetrofitClient.instance.getGroupChangeRequests()
                pendingRequestsCount = requests.count { it.status == "PENDING" }
            } catch (_: Exception) {}
            try {
                val stats = RetrofitClient.instance.getMyAttendanceStats()
                attendanceStats = MyAttendanceStats(
                    active    = (stats["active"]    ?: 0L).toInt(),
                    today     = (stats["today"]     ?: 0L).toInt(),
                    thisMonth = (stats["thisMonth"] ?: 0L).toInt()
                )
            } catch (_: Exception) {}
            try {
                val ratingResp = RetrofitClient.instance.getMyRatingStats()
                if (ratingResp.isSuccessful) {
                    val body = ratingResp.body()
                    ratingAverage = (body?.get("average") as? Number)?.toDouble()
                    ratingTotal   = (body?.get("total")   as? Number)?.toInt() ?: 0
                }
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(30_000)
        }
    }

    Scaffold(containerColor = Color(0xFFF0F6FA)) { innerPadding ->
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
                    // Saudação + ações
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(greetingByHour(),
                            fontSize     = 13.sp,
                            color        = Color.White.copy(alpha = 0.70f),
                            letterSpacing = 0.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { onNavigateToProfile() },
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarUrl = operatorAvatarUrl
                                if (!avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model              = avatarUrl,
                                        contentDescription = "Avatar do operador",
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Text(
                                        firstName.firstOrNull()?.uppercase() ?: "O",
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White
                                    )
                                }
                            }
                            IconButton(
                                onClick  = { showLogoutDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Sair",
                                    tint     = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(firstName, fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 30.sp)
                    Text("Bem-vindo ao WTC Connecta",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.60f))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pills de métricas
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroMetricPill(label = "Clientes",
                            value = uiState.clients.size.toString(),
                            icon  = Icons.Default.People)
                        HeroMetricPill(label = "Grupos",
                            value = uiState.groups.size.toString(),
                            icon  = Icons.Default.Group)
                        if (queueState.activeConversations.isNotEmpty()) {
                            HeroMetricPill(label = "Ativos",
                                value  = queueState.activeConversations.size.toString(),
                                icon   = Icons.Default.Forum,
                                accent = Color(0xFF81C784))
                        }
                        if (queueState.pendingCount > 0) {
                            HeroMetricPill(label = "Na fila",
                                value  = queueState.pendingCount.toString(),
                                icon   = Icons.Default.HeadsetMic,
                                accent = Color(0xFFFFB74D))
                        }
                    }
                }
            }

            // ── Corpo ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Alerta de fila ────────────────────────────────────────────
                if (queueState.pendingCount > 0) {
                    Card(
                        onClick   = onNavigateToAttendanceQueue,
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        elevation = CardDefaults.cardElevation(3.dp)
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
                                    .background(Color(0xFFFFE0B2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HeadsetMic, null,
                                    tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fila de Atendimento",
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary)
                                Text("${queueState.pendingCount} cliente(s) aguardando",
                                    fontSize = 12.sp, color = Color(0xFFE65100))
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE65100)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(queueState.pendingCount.toString(),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White)
                            }
                        }
                    }
                }

                // ── Meus Atendimentos — card fixo com métricas ───────────────
                Card(
                    onClick   = onNavigateToAttendanceQueue,
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFFEDF7F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.HeadsetMic, null,
                                        tint     = Color(0xFF1A7A5E),
                                        modifier = Modifier.size(20.dp))
                                }
                                Text("Meus Atendimentos",
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = TextPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(WtcBlueHint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChevronRight, null,
                                    tint     = WtcBlueDark,
                                    modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Em andamento
                            AttendanceStatBox(
                                modifier = Modifier.weight(1f),
                                value    = attendanceStats.active.toString(),
                                label    = "Ativos",
                                color    = Color(0xFF1A7A5E),
                                bgColor  = Color(0xFFEDF7F2)
                            )
                            // Encerrados hoje
                            AttendanceStatBox(
                                modifier = Modifier.weight(1f),
                                value    = attendanceStats.today.toString(),
                                label    = "Hoje",
                                color    = WtcBlue,
                                bgColor  = WtcBluePale
                            )
                            // Encerrados este mês
                            AttendanceStatBox(
                                modifier = Modifier.weight(1f),
                                value    = attendanceStats.thisMonth.toString(),
                                label    = "Este mês",
                                color    = TextMuted,
                                bgColor  = Color(0xFFF0F6FA)
                            )
                        }
                    }
                }

                // ── Avaliação média ──────────────────────────────────────────
                if (ratingAverage != null && ratingTotal > 0) {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
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
                                    .background(Color(0xFFFFF8E1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, null,
                                    tint     = Color(0xFFFFB300),
                                    modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Avaliação dos Clientes",
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = TextPrimary)
                                Text("Baseado em $ratingTotal avaliação(ões)",
                                    fontSize = 12.sp,
                                    color    = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                            // Nota média em destaque
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Star, null,
                                        tint     = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp))
                                    Text(
                                        String.format("%.1f", ratingAverage),
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = TextPrimary
                                    )
                                }
                                Text("/ 5.0", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }

                // ── Ações Rápidas ─────────────────────────────────────────────
                Text("AÇÕES RÁPIDAS", fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.2.sp)

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Clientes",
                        icon = Icons.Default.People, onClick = onViewClients)
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Msg Grupo",
                        icon = Icons.Default.Groups, onClick = { showGroupMessageDialog = true })
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Campanhas",
                        icon = Icons.Default.Campaign, onClick = onNavigateToCampaigns)
                }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Tarefas",
                        icon = Icons.Default.TaskAlt, onClick = onNavigateToKanban)
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Grupos",
                        icon = Icons.Default.AccountTree, onClick = onNavigateToGroupManagement)
                    GridDashboardCardWithBadge(modifier = Modifier.weight(1f),
                        title = "Solicitações", icon = Icons.Default.GroupAdd,
                        badgeCount = pendingRequestsCount, onClick = onNavigateToGroupRequests)
                }

                DashboardCard(title = "Auditoria",
                    subtitle = "Histórico de operações realizadas",
                    icon = Icons.Default.History, onClick = onNavigateToAudit)

                Text("WTC Connecta • Painel do operador",
                    fontSize = 11.sp, color = TextMuted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }

    if (showGroupMessageDialog) {
        GroupMessageDialog(
            divisions  = uiState.divisions,
            groups     = uiState.groups,
            onDismiss  = { showGroupMessageDialog = false },
            onSendGroup = { text, groupId ->
                viewModel.sendGroupMessage(text, groupId, "")
                showGroupMessageDialog = false
            },
            onSendDivision = { text, divisionId ->
                viewModel.sendDivisionMessage(text, divisionId, "")
                showGroupMessageDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da conta", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text  = { Text("Tem certeza que deseja sair?", color = TextMuted) },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = WtcBlue),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Sair", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

// ── HeroMetricPill ────────────────────────────────────────────────────────────

@Composable
fun HeroMetricPill(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color = Color.White
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint = accent, modifier = Modifier.size(13.dp))
            Text(value, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.70f))
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(WtcBlueHint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = WtcBlueDark, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(value, fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(label, fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun GridDashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(92.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(18.dp))
            }
            Text(title, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Composable
fun GridDashboardCardWithBadge(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    badgeCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(92.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WtcBluePale),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null,
                        tint = WtcBlue, modifier = Modifier.size(18.dp))
                }
                Text(title, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE65100))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (badgeCount > 9) "9+" else badgeCount.toString(),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AttendanceStatBox(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    color: Color,
    bgColor: Color
) {
    Surface(
        modifier = modifier,
        color     = bgColor,
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier              = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(2.dp)
        ) {
            Text(value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = color)
            Text(label,
                fontSize     = 10.sp,
                fontWeight   = FontWeight.SemiBold,
                color        = color.copy(alpha = 0.70f),
                letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
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
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = WtcBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp))
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