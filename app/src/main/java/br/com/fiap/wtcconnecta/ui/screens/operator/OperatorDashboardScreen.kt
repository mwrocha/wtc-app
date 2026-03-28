package br.com.fiap.wtcconnecta.ui.screens.operator

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.viewmodel.HomeOperatorViewModel

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val WtcNavy     = Color(0xFF063D5C)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

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
    viewModel: HomeOperatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGroupMessageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.retryFetch() }

    Scaffold(containerColor = Color(0xFFF5FAFD)) { innerPadding ->
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
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (operatorName.isNotBlank()) "Olá, ${operatorName.split(" ").first()}! 👋"
                            else "Painel do Operador",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                        Text("Bem-vindo ao WTC Connecta",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                    }
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil",
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Métricas
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(modifier = Modifier.weight(1f), label = "Clientes",
                        value = uiState.clients.size.toString(), icon = Icons.Default.People)
                    MetricCard(modifier = Modifier.weight(1f), label = "Grupos",
                        value = uiState.groups.size.toString(), icon = Icons.Default.Group)
                }

                Text("AÇÕES RÁPIDAS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = TextMuted, letterSpacing = 1.sp)

                // Grade 3x2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Clientes",
                        icon = Icons.Default.People, onClick = onViewClients)
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Msg Grupo",
                        icon = Icons.Default.Groups, onClick = { showGroupMessageDialog = true })
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Campanhas",
                        icon = Icons.Default.Campaign, onClick = onNavigateToCampaigns)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Tarefas",
                        icon = Icons.Default.TaskAlt, onClick = onNavigateToKanban)
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Grupos",
                        icon = Icons.Default.AccountTree, onClick = onNavigateToGroupManagement)
                    GridDashboardCard(modifier = Modifier.weight(1f), title = "Solicitações",
                        icon = Icons.Default.GroupAdd, onClick = onNavigateToGroupRequests)
                }

                // Auditoria — card largo
                DashboardCard(title = "Auditoria",
                    subtitle = "Histórico de operações realizadas",
                    icon = Icons.Default.History, onClick = onNavigateToAudit)
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
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
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
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(18.dp))
            }
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WtcBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = WtcBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = WtcBlueHint, modifier = Modifier.size(20.dp))
        }
    }
}