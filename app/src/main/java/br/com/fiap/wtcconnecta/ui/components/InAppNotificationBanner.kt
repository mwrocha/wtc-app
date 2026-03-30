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

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBlueSoft = Color(0xFF1A6E9A)
private val WtcBluePale = Color(0xFFEEF6FB)

@Composable
fun InAppNotificationBanner(navController: NavController? = null) {
    val notification by InAppNotificationState.notification.collectAsState()
    val isLoggedIn = RetrofitClient.authToken != null

    AnimatedVisibility(
        visible = notification != null && isLoggedIn,
        enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        notification?.let { notif ->

            // Ícone e cor accent por tipo — mantendo identidade visual
            val (icon, accentColor) = when (notif.type) {
                "CAMPAIGN"      -> Icons.Default.Campaign  to Color(0xFF1A7A5E)  // verde
                "GROUP"         -> Icons.Default.Group     to WtcBlueSoft        // azul médio
                "GROUP_REQUEST" -> Icons.Default.GroupAdd  to Color(0xFFE65100)  // laranja
                else            -> Icons.Default.Chat      to WtcBlue            // azul principal
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable {
                        if (notif.chatId.isNotBlank() && navController != null) {
                            if (notif.chatType == "group_request")
                                navController.navigate("group_requests")
                            else navController.navigate(
                                "chat/${notif.chatId}/${notif.chatName}/${notif.chatType}"
                            )
                        }
                        InAppNotificationState.dismiss()
                    }
            ) {
                // Barra colorida no topo
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
                        Text("Toque para abrir", fontSize = 10.sp,
                            color = accentColor, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 3.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = { InAppNotificationState.dismiss() },
                        modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar",
                            tint = Color(0xFF6E90A0), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}