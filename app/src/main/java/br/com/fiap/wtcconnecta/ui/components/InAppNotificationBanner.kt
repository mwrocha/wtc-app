package br.com.fiap.wtcconnecta.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.service.InAppNotificationState
import br.com.fiap.wtcconnecta.ui.navigation.Routes
import kotlinx.coroutines.launch

// ── Helpers JWT ───────────────────────────────────────────────────────────────
private fun getRoleFromToken(token: String?): String? {
    if (token == null) return null
    return try {
        val payload = token.split(".")[1]
        val decoded = android.util.Base64.decode(
            payload.padEnd((payload.length + 3) / 4 * 4, '='),
            android.util.Base64.URL_SAFE
        )
        val json = String(decoded)
        Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    } catch (e: Exception) { null }
}

private fun getEmailFromToken(token: String?): String? {
    if (token == null) return null
    return try {
        val payload = token.split(".")[1]
        val decoded = android.util.Base64.decode(
            payload.padEnd((payload.length + 3) / 4 * 4, '='),
            android.util.Base64.URL_SAFE
        )
        val json = String(decoded)
        Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
    } catch (e: Exception) { null }
}

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)

@Composable
fun InAppNotificationBanner(navController: NavController? = null) {
    val notification  by InAppNotificationState.notification.collectAsState()
    val isLoggedIn    = RetrofitClient.authToken != null
    val isOperator    = getRoleFromToken(RetrofitClient.authToken) == "OPERATOR"
    val operatorEmail = getEmailFromToken(RetrofitClient.authToken)
    val scope         = rememberCoroutineScope()

    AnimatedVisibility(
        visible = notification != null && isLoggedIn,
        enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        notification?.let { notif ->

            val (icon, accentColor) = when (notif.type) {
                "CAMPAIGN"      -> Icons.Default.Campaign  to Color(0xFF1A7A5E)
                "GROUP"         -> Icons.Default.Group     to WtcBlueSoft
                "GROUP_REQUEST" -> Icons.Default.GroupAdd  to Color(0xFFE65100)
                else            -> Icons.Default.Chat      to WtcBlue
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable {
                        if (navController != null) {
                            when {
                                notif.chatType == "group_request" ->
                                    navController.navigate("group_requests")

                                // Operador com push DIRECT:
                                // verifica se já assumiu → vai ao ClientDetail
                                // ou ainda não assumiu → vai à fila
                                isOperator && notif.type == "DIRECT" -> {
                                    scope.launch {
                                        try {
                                            val conversationId = notif.chatId
                                            val status = RetrofitClient.instance
                                                .getConversationStatus(conversationId)

                                            if (status.status == "IN_PROGRESS" &&
                                                status.assignedOperatorEmail == operatorEmail) {
                                                // Já assumiu → extrai email do cliente
                                                // e busca o clientId para navegar
                                                val mid = conversationId.indexOf(
                                                    "_", conversationId.indexOf("@")
                                                )
                                                val emailA = conversationId.substring(0, mid)
                                                val emailB = conversationId.substring(mid + 1)
                                                val clientEmail = if (emailA != operatorEmail)
                                                    emailA else emailB

                                                val clients = RetrofitClient.instance.getClients()
                                                val client  = clients.find { it.email == clientEmail }
                                                if (client != null) {
                                                    navController.navigate(
                                                        Routes.ClientDetail.createRoute(client.id)
                                                    )
                                                } else {
                                                    navController.navigate(Routes.AttendanceQueue.route)
                                                }
                                            } else {
                                                // Ainda não assumiu → vai para a fila
                                                navController.navigate(Routes.AttendanceQueue.route)
                                            }
                                        } catch (e: Exception) {
                                            navController.navigate(Routes.AttendanceQueue.route)
                                        }
                                    }
                                }

                                // Cliente, grupo, campanha → navega normalmente
                                notif.chatId.isNotBlank() ->
                                    navController.navigate(
                                        "chat/${notif.chatId}/${notif.chatName}/${notif.chatType}"
                                    )
                            }
                        }
                        InAppNotificationState.dismiss()
                    }
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor)
                    .align(Alignment.TopCenter))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 15.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null,
                            tint = accentColor, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D2B3E), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        if (notif.body.isNotBlank()) {
                            Text(notif.body, fontSize = 12.sp,
                                color = Color(0xFF6E90A0), maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(
                            if (isOperator && notif.type == "DIRECT") "Toque para abrir o chat"
                            else "Toque para abrir",
                            fontSize = 10.sp, color = accentColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { InAppNotificationState.dismiss() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar",
                            tint = Color(0xFF6E90A0), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}