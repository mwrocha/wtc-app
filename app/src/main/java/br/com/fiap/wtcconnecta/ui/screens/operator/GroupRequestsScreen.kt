package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.wtcconnecta.data.model.GroupChangeRequestItem
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
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
private val ColorApproved = Color(0xFF1A7A5E)
private val ColorRejected = Color(0xFFC62828)
private val ColorPending  = Color(0xFFE65100)

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class GroupRequestsUiState(
    val requests: List<GroupChangeRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class GroupRequestsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GroupRequestsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val requests = RetrofitClient.instance.getGroupChangeRequests()
                _uiState.update { it.copy(requests = requests, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar solicitações.") }
            }
        }
    }

    fun approve(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.approveGroupChangeRequest(id)
                _uiState.update { state ->
                    state.copy(requests = state.requests.map {
                        if (it.id == id) it.copy(status = "APPROVED") else it
                    }, successMessage = "Solicitação aprovada!")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao aprovar solicitação.") }
            }
        }
    }

    fun reject(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.rejectGroupChangeRequest(id)
                _uiState.update { state ->
                    state.copy(requests = state.requests.map {
                        if (it.id == id) it.copy(status = "REJECTED") else it
                    }, successMessage = "Solicitação rejeitada.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao rejeitar solicitação.") }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupRequestsScreen(
    onBack: () -> Unit,
    viewModel: GroupRequestsViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadRequests() }
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    val pending  = uiState.requests.filter { it.status == "PENDING" }
    val reviewed = uiState.requests.filter { it.status != "PENDING" }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5FAFD)
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

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
                            Text("Solicitações de Grupo", fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Aprovar ou rejeitar trocas de grupo",
                                fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                        }
                    }
                    // Badge de pendentes
                    if (pending.isNotEmpty()) {
                        Surface(
                            color = ColorPending,
                            shape = CircleShape
                        ) {
                            Text("${pending.size}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                uiState.requests.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(WtcBluePale), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                modifier = Modifier.size(36.dp), tint = WtcBlue)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nenhuma solicitação pendente.", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Tudo em dia por aqui!", fontSize = 13.sp,
                            color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)) {
                                Text("PENDENTES", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = ColorPending, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(color = ColorPending.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)) {
                                    Text("${pending.size}", fontSize = 10.sp,
                                        color = ColorPending, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                        items(pending) { request ->
                            GroupRequestCard(request = request,
                                onApprove = { viewModel.approve(request.id) },
                                onReject  = { viewModel.reject(request.id) })
                        }
                    }
                    if (reviewed.isNotEmpty()) {
                        item {
                            Text("HISTÓRICO", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = TextMuted, letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(reviewed) { request ->
                            GroupRequestCard(request = request)
                        }
                    }
                }
            }
        }
    }
}

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
fun GroupRequestCard(
    request: GroupChangeRequestItem,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null
) {
    val statusColor = when (request.status) {
        "APPROVED" -> ColorApproved
        "REJECTED" -> ColorRejected
        else       -> ColorPending
    }
    val statusLabel = when (request.status) {
        "APPROVED" -> "Aprovada"
        "REJECTED" -> "Rejeitada"
        else       -> "Pendente"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header do card
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(WtcBluePale), contentAlignment = Alignment.Center) {
                        Text(request.clientName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WtcBlue)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(request.clientName, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(request.clientEmail, fontSize = 11.sp, color = TextMuted,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)) {
                    Text(statusLabel, fontSize = 11.sp, color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F6FA))
            Spacer(modifier = Modifier.height(10.dp))

            // Troca de grupo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = WtcBluePale, shape = RoundedCornerShape(8.dp)) {
                    Text(request.currentGroupName, fontSize = 12.sp, color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(horizontal = 4.dp),
                    tint = WtcBlueHint)
                Surface(color = WtcBluePale, shape = RoundedCornerShape(8.dp)) {
                    Text(request.requestedGroupName, fontSize = 12.sp, color = WtcBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (request.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = Color(0xFFF8FAFB), shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = TextMuted, modifier = Modifier.size(13.dp)
                                .padding(top = 1.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(request.reason, fontSize = 12.sp, color = TextMuted,
                            lineHeight = 17.sp)
                    }
                }
            }

            // Botões apenas para pendentes
            if (onApprove != null && onReject != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ColorRejected),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, ColorRejected.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rejeitar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorApproved)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aprovar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}