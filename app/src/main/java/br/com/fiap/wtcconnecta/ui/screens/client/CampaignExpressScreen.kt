package br.com.fiap.wtcconnecta.ui.screens.client

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.repository.AuthRepository
import br.com.fiap.wtcconnecta.ui.navigation.DeepLinkHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class CampaignExpressUiState(
    val isLoading: Boolean = false,
    val campaigns: List<Message> = emptyList(),
    val error: String? = null
)

class CampaignExpressViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampaignExpressUiState())
    val uiState = _uiState.asStateFlow()

    fun loadCampaigns() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val campaigns = RetrofitClient.instance.getMyCampaigns()
                _uiState.update { it.copy(isLoading = false, campaigns = campaigns) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar campanhas.") }
            }
        }
    }

    fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                try {
                    val campaigns = RetrofitClient.instance.getMyCampaigns()
                    _uiState.update { it.copy(campaigns = campaigns) }
                } catch (e: Exception) {}
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignExpressScreen(
    onBack: () -> Unit,
    clientId: String = "",
    navController: NavController? = null,
    viewModel: CampaignExpressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCampaigns()
        viewModel.startPolling()
    }

    Scaffold(containerColor = Color(0xFFF5FAFD)) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // Header gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(WtcBlue, WtcBlueSoft)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Campanhas Express", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Comunicados e promoções exclusivas", fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.72f))
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.campaigns.isEmpty() -> EmptyCampaignsState()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
                ) {
                    items(uiState.campaigns) { campaign ->
                        CampaignExpressCard(
                            campaign      = campaign,
                            clientId      = clientId,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCampaignsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(WtcBluePale),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null,
                modifier = Modifier.size(40.dp), tint = WtcBlue)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nenhuma campanha ainda", fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Fique de olho — novidades chegam aqui!", fontSize = 13.sp,
            color = TextMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun CampaignExpressCard(
    campaign: Message,
    clientId: String = "",
    navController: NavController? = null
) {
    val context = LocalContext.current

    // ── Corpo da campanha com expansão ────────────────────────────────────────
    val bodyText = campaign.body?.ifBlank { null } ?: campaign.displayContent.ifBlank { null }
    val hasLongBody = (bodyText?.length ?: 0) > 100
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(WtcBluePale),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null,
                        tint = WtcBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.title ?: "Comunicado WTC",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatCampaignTime(campaign.createdAt),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Surface(color = WtcBluePale, shape = RoundedCornerShape(20.dp)) {
                    Text("Enviada", fontSize = 10.sp, color = WtcBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            // ── Corpo expansível ──────────────────────────────────────────────
            if (!bodyText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F6FA))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = bodyText,
                    fontSize = 14.sp,
                    color = TextMuted,
                    lineHeight = 20.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                )

                if (hasLongBody) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = WtcBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isExpanded) "Ver menos" else "Ver mais",
                            fontSize = 12.sp, color = WtcBlue
                        )
                    }
                }
            }

            // ── URL "Saiba mais" ──────────────────────────────────────────────
            campaign.url?.takeIf { it.isNotBlank() }?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                val isDeepLink = url.startsWith("wtcconnecta://")
                TextButton(
                    onClick = {
                        if (isDeepLink && navController != null)
                            DeepLinkHandler.handle(url, navController, clientId)
                        else runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    Icon(
                        if (isDeepLink) Icons.Default.Launch else Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = WtcBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isDeepLink) "Acessar no app" else "Saiba mais",
                        fontSize = 13.sp, color = WtcBlue, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Botões de ação ────────────────────────────────────────────────
            campaign.actions?.takeIf { it.isNotEmpty() }?.let { actions ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { action ->
                        // Prioridade: actionUrls → url da campanha (se 1 botão) → wtcconnecta://chat
                        val fromActionUrls = campaign.actionUrls?.get(action.action)?.takeIf { it.isNotBlank() }
                        val fromCampaignUrl = if (actions.size == 1) campaign.url?.takeIf { it.isNotBlank() } else null
                        val actionUrl: String = fromActionUrls ?: fromCampaignUrl ?: "wtcconnecta://chat"

                        val isDeepLink = actionUrl.startsWith("wtcconnecta://")

                        Button(
                            onClick = {
                                when {
                                    isDeepLink && navController != null ->
                                        DeepLinkHandler.handle(actionUrl, navController, clientId)
                                    actionUrl.isNotBlank() -> runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl))
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                        ) {
                            if (isDeepLink) {
                                Icon(Icons.Default.Chat, contentDescription = null,
                                    modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(action.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCampaignTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val date = createdAt.take(10)
        val time = createdAt.drop(11).take(5)
        "$date às $time"
    } catch (e: Exception) { "" }
}