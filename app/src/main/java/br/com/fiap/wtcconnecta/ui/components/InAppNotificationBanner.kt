package br.com.fiap.wtcconnecta.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.service.InAppNotificationState

@Composable
fun InAppNotificationBanner(navController: NavController? = null) {
    val notification by InAppNotificationState.notification.collectAsState()
    val isLoggedIn = RetrofitClient.authToken != null

    AnimatedVisibility(
        visible = notification != null && isLoggedIn,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        notification?.let { notif ->
            val icon = when (notif.type) {
                "CAMPAIGN" -> Icons.Default.Campaign
                "GROUP"         -> Icons.Default.Group
                "GROUP_REQUEST" -> Icons.Default.GroupAdd
                else       -> Icons.Default.Chat
            }
            val containerColor = when (notif.type) {
                "CAMPAIGN" -> MaterialTheme.colorScheme.primaryContainer
                "GROUP"         -> MaterialTheme.colorScheme.secondaryContainer
                "GROUP_REQUEST" -> MaterialTheme.colorScheme.tertiaryContainer
                else       -> MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = when (notif.type) {
                "CAMPAIGN" -> MaterialTheme.colorScheme.onPrimaryContainer
                "GROUP"         -> MaterialTheme.colorScheme.onSecondaryContainer
                "GROUP_REQUEST" -> MaterialTheme.colorScheme.onTertiaryContainer
                else       -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    // Clicável — navega para o chat correspondente
                    .clickable {
                        if (notif.chatId.isNotBlank() && navController != null) {
                            if (notif.chatType == "group_request") {
                                navController.navigate("group_requests")
                            } else navController.navigate(
                                "chat/${notif.chatId}/${notif.chatName}/${notif.chatType}"
                            )
                        }
                        InAppNotificationState.dismiss()
                    },
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notif.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (notif.body.isNotBlank()) {
                            Text(
                                text = notif.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.85f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Toque para abrir →",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { InAppNotificationState.dismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar",
                            tint = contentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}