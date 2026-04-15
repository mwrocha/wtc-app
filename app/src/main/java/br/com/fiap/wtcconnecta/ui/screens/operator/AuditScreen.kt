package br.com.fiap.wtcconnecta.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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

// Cores semânticas dos ícones — mantidas intencionalmente
private val ColorCreate   = Color(0xFF2E7D32)  // verde
private val ColorUpdate   = Color(0xFFE65100)  // laranja
private val ColorDelete   = Color(0xFFC62828)  // vermelho
private val ColorDispatch = Color(0xFF1565C0)  // azul
private val ColorLogin    = Color(0xFF6A1B9A)  // roxo

// ── Model ─────────────────────────────────────────────────────────────────────

data class AuditLogItem(
    val id: String = "", val action: String = "", val entity: String = "",
    val entityId: String = "", val performedBy: String = "",
    val description: String = "", val timestamp: String = ""
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class AuditUiState(
    val logs: List<AuditLogItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterAction: String? = null,
    val filterEntity: String? = null,
    val showMineOnly: Boolean = false,
    val filterDate: String? = null
)

class AuditViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState = _uiState.asStateFlow()

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.instance.getAuditLogs()
                _uiState.update { it.copy(logs = response, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar logs.") }
            }
        }
    }

    fun setFilterAction(action: String?) = _uiState.update { it.copy(filterAction = action) }
    fun setFilterEntity(entity: String?) = _uiState.update { it.copy(filterEntity = entity) }
    fun setFilterDate(date: String?)     = _uiState.update { it.copy(filterDate = date) }
    fun toggleMineOnly()                 = _uiState.update { it.copy(showMineOnly = !it.showMineOnly) }

    fun filteredLogs(currentUserEmail: String): List<AuditLogItem> {
        var result = _uiState.value.logs
        if (_uiState.value.showMineOnly) result = result.filter { it.performedBy == currentUserEmail }
        _uiState.value.filterAction?.let { a -> result = result.filter { it.action == a } }
        _uiState.value.filterEntity?.let { e -> result = result.filter { it.entity.equals(e, ignoreCase = true) } }
        _uiState.value.filterDate?.let   { d -> result = result.filter { it.timestamp.startsWith(d) } }
        return result
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    onBack: () -> Unit,
    currentUserEmail: String = "",
    viewModel: AuditViewModel = viewModel()
) {
    val uiState           by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadLogs() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val filteredLogs      = viewModel.filteredLogs(currentUserEmail)
    val availableEntities = uiState.logs.map { it.entity }.distinct().sorted()
    val availableActions  = uiState.logs.map { it.action }.distinct().sorted()

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = Color(0xFFF5FAFD)
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
                            Text("Auditoria", fontSize = 20.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Histórico de operações realizadas",
                                fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
                        }
                    }
                    IconButton(
                        onClick = { viewModel.loadLogs() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Painel de filtros
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Contador + toggle meus logs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = WtcBluePale, shape = RoundedCornerShape(8.dp)) {
                        Text("${filteredLogs.size} registro(s)",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WtcBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Apenas meus", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.showMineOnly,
                            onCheckedChange = { viewModel.toggleMineOnly() },
                            modifier = Modifier.height(24.dp),
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                                checkedTrackColor = WtcBlue)
                        )
                    }
                }

                // Filtro por data
                OutlinedTextField(
                    value = uiState.filterDate ?: "",
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() || it == '-' }.take(10)
                        viewModel.setFilterDate(cleaned.ifBlank { null })
                    },
                    placeholder = { Text("Filtrar por data: aaaa-mm-dd",
                        color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null,
                            tint = if (uiState.filterDate != null) WtcBlue else WtcBlueHint,
                            modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (uiState.filterDate != null) {
                            IconButton(onClick = { viewModel.setFilterDate(null) },
                                modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar",
                                    modifier = Modifier.size(16.dp), tint = TextMuted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WtcBlue, unfocusedBorderColor = WtcBlueHint,
                        cursorColor = WtcBlue,
                        focusedContainerColor = WtcBluePale.copy(alpha = 0.4f),
                        unfocusedContainerColor = Color(0xFFFAFCFE)
                    )
                )

                // Filtro por ação
                if (availableActions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = uiState.filterAction == null,
                                onClick = { viewModel.setFilterAction(null) },
                                label = { Text("Todas", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WtcBlue,
                                    selectedLabelColor     = Color.White))
                        }
                        items(availableActions) { action ->
                            val color = actionColorStatic(action)
                            FilterChip(
                                selected = uiState.filterAction == action,
                                onClick  = { viewModel.setFilterAction(if (uiState.filterAction == action) null else action) },
                                label    = { Text(actionLabel(action), fontSize = 11.sp) },
                                leadingIcon = { Icon(actionIcon(action), contentDescription = null,
                                    modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor     = color,
                                    containerColor         = WtcBluePale,
                                    labelColor             = TextMuted)
                            )
                        }
                    }
                }

                // Filtro por entidade
                if (availableEntities.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = uiState.filterEntity == null,
                                onClick = { viewModel.setFilterEntity(null) },
                                label = { Text("Tudo", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WtcBlue,
                                    selectedLabelColor     = Color.White))
                        }
                        items(availableEntities) { entity ->
                            FilterChip(
                                selected = uiState.filterEntity == entity,
                                onClick  = { viewModel.setFilterEntity(if (uiState.filterEntity == entity) null else entity) },
                                label    = { Text(entityLabel(entity), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WtcBlue,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = WtcBluePale,
                                    labelColor             = TextMuted)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = WtcBlueHint.copy(alpha = 0.5f))

            // Lista
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WtcBlue)
                }
                filteredLogs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(WtcBluePale), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, contentDescription = null,
                                modifier = Modifier.size(36.dp), tint = WtcBlue)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nenhum registro encontrado.", color = TextMuted, fontSize = 14.sp)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        AuditLogCard(log = log, currentUserEmail = currentUserEmail)
                    }
                }
            }
        }
    }
}

// ── Card de log ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogCard(log: AuditLogItem, currentUserEmail: String) {
    val actionColor = actionColorStatic(log.action)
    val isMe        = log.performedBy == currentUserEmail
    var showSheet   by remember { mutableStateOf(false) }

    Card(
        onClick   = { showSheet = true },
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            // Ícone semântico — cores mantidas intencionalmente
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(actionColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(actionIcon(log.action), contentDescription = null,
                    tint = actionColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entityLabel(log.entity), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = actionColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)) {
                            Text(actionLabel(log.action), fontSize = 10.sp,
                                color = actionColor, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(formatTimestamp(log.timestamp), fontSize = 10.sp, color = TextMuted)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(log.description, fontSize = 12.sp, color = TextMuted,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (isMe) WtcBlue else TextMuted.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isMe) "Você (${log.performedBy})" else log.performedBy,
                            fontSize = 10.sp,
                            color = if (isMe) WtcBlue else TextMuted.copy(alpha = 0.7f),
                            fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = WtcBlueHint, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    // ── Bottom Sheet com detalhes completos ───────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Título
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                            .background(actionColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(actionIcon(log.action), contentDescription = null,
                            tint = actionColor, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(entityLabel(log.entity), fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(color = actionColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)) {
                                Text(actionLabel(log.action), fontSize = 11.sp,
                                    color = actionColor, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(formatTimestamp(log.timestamp), fontSize = 11.sp, color = TextMuted)
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F6FA))

                // Descrição completa
                Text("Descrição", fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = TextMuted)
                Text(log.description, fontSize = 14.sp,
                    color = TextPrimary, lineHeight = 20.sp)

                HorizontalDivider(color = Color(0xFFF0F6FA))

                // Detalhes
                DetailRow(label = "Operador", value = log.performedBy,
                    highlight = isMe)
                if (log.entityId.isNotBlank()) {
                    DetailRow(label = "ID do registro", value = log.entityId)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = TextMuted)
        Text(value, fontSize = 12.sp,
            color = if (highlight) WtcBlue else TextPrimary,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun entityLabel(entity: String) = when (entity.lowercase()) {
    "campaign"           -> "Campanha"
    "groupchangerequest" -> "Mudança de Grupo"
    "note"               -> "Anotação"
    "user"               -> "Usuário"
    "message"            -> "Mensagem"
    else                 -> entity
}

fun actionLabel(action: String) = when (action) {
    "CREATE"   -> "Criação"
    "UPDATE"   -> "Edição"
    "DELETE"   -> "Exclusão"
    "DISPATCH" -> "Disparo"
    "LOGIN"    -> "Login"
    else       -> action
}

// Versão sem @Composable — usada em FilterChip e Card
fun actionColorStatic(action: String): Color = when (action) {
    "CREATE"   -> ColorCreate
    "UPDATE"   -> ColorUpdate
    "DELETE"   -> ColorDelete
    "DISPATCH" -> ColorDispatch
    "LOGIN"    -> ColorLogin
    else       -> Color(0xFF6E90A0)
}

// Versão @Composable mantida por compatibilidade com código existente
@Composable
fun actionColor(action: String) = actionColorStatic(action)

fun actionIcon(action: String): ImageVector = when (action) {
    "CREATE"   -> Icons.Default.Add
    "UPDATE"   -> Icons.Default.Edit
    "DELETE"   -> Icons.Default.Delete
    "DISPATCH" -> Icons.Default.Send
    "LOGIN"    -> Icons.Default.Login
    else       -> Icons.Default.Info
}

fun formatTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val date = timestamp.take(10).split("-")  // [2026, 04, 08]
        val time = timestamp.drop(11).take(5)     // 02:41
        "${date[2]}-${date[1]}-${date[0]} às $time"
    } catch (e: Exception) { timestamp }
}