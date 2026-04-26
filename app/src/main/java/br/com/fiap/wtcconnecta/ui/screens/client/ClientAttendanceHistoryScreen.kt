package br.com.fiap.wtcconnecta.ui.screens.client

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBlueDark = Color(0xFF063D5C)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)
private val StarColor   = Color(0xFFFFB300)

data class ClientAttendanceItem(
    val sessionId: String,
    val operatorName: String,
    val operatorEmail: String,
    val assumedAt: String,
    val closedAt: String,
    val stars: Int?,
    val comment: String?
)

@Composable
fun ClientAttendanceHistoryScreen(
    onBack: () -> Unit
) {
    var items     by remember { mutableStateOf<List<ClientAttendanceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getMyAttendanceHistory()
            if (response.isSuccessful) {
                items = (response.body() ?: emptyList()).map { map ->
                    ClientAttendanceItem(
                        sessionId     = map["sessionId"]     as? String ?: "",
                        operatorName  = map["operatorName"]  as? String ?: "",
                        operatorEmail = map["operatorEmail"] as? String ?: "",
                        assumedAt     = map["assumedAt"]     as? String ?: "",
                        closedAt      = map["closedAt"]      as? String ?: "",
                        stars         = (map["stars"]        as? Number)?.toInt(),
                        comment       = map["comment"]       as? String
                    )
                }
            } else {
                error = "Erro ao carregar histórico."
            }
        } catch (e: Exception) {
            error = "Erro ao carregar histórico."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F6FA))
    ) {
        // ── Header Hero ───────────────────────────────────────────────────────
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
                modifier = Modifier
                    .fillMaxWidth()
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

                Text("Meus Atendimentos", fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Histórico de atendimentos encerrados",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))

                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("${items.size} atendimento(s)",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                }
            }
        }

        // ── Conteúdo ──────────────────────────────────────────────────────────
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WtcBlue)
            }
            error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(error ?: "", color = TextMuted, fontSize = 14.sp)
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                            .background(WtcBluePale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = null,
                            tint = WtcBlue, modifier = Modifier.size(36.dp))
                    }
                    Text("Nenhum atendimento ainda", fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Seus atendimentos encerrados aparecerão aqui.",
                        fontSize = 13.sp, color = TextMuted)
                }
            }
            else -> LazyColumn(
                modifier        = Modifier.fillMaxSize(),
                contentPadding  = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    ClientAttendanceCard(item = item)
                }
            }
        }
    }
}

@Composable
fun ClientAttendanceCard(item: ClientAttendanceItem) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Operador
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                        .background(WtcBlueHint),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.operatorName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WtcBlueDark)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.operatorName, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Operador de atendimento", fontSize = 11.sp, color = TextMuted)
                }
                // Badge encerrado
                Surface(
                    color = Color(0xFFEDF7F2),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Encerrado", fontSize = 10.sp,
                        color = Color(0xFF1A7A5E), fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F6FA))
            Spacer(modifier = Modifier.height(12.dp))

            // Data de encerramento
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null,
                    tint = TextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Encerrado em: ${formatHistoryDateTime(item.closedAt)}",
                    fontSize = 12.sp, color = TextMuted)
            }

            // Avaliação dada
            if (item.stars != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Sua avaliação:", fontSize = 12.sp,
                        color = TextMuted, fontWeight = FontWeight.Medium)
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < item.stars) Icons.Default.Star
                            else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint     = if (i < item.stars) StarColor else Color(0xFFCFD8DC),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (!item.comment.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color    = Color(0xFFF5FAFD),
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("\"${item.comment}\"",
                            fontSize  = 12.sp,
                            color     = TextMuted,
                            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Não avaliado", fontSize = 12.sp,
                    color = TextMuted.copy(alpha = 0.6f))
            }
        }
    }
}

private fun formatHistoryDateTime(dateStr: String): String {
    return try {
        val date = dateStr.take(10).split("-")
        val time = dateStr.substringAfter("T").take(5)
        if (date.size == 3) "${date[2]}/${date[1]}/${date[0]} às $time" else dateStr
    } catch (_: Exception) { "" }
}