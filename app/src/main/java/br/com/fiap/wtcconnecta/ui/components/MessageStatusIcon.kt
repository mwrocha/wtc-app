package br.com.fiap.wtcconnecta.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.wtcconnecta.data.model.MessageStatus


/**
 * Ícone de status de mensagem estilo WhatsApp.
 * Exibido apenas nas mensagens enviadas pelo usuário atual.
 *
 * SENDING  → 🕐 relógio cinza
 * SENT     → ✓  check simples cinza
 * DELIVERED→ ✓✓ dois checks cinza
 * READ     → ✓✓ dois checks azul
 * FAILED   → ⚠  ícone vermelho de erro
 */
@Composable
fun MessageStatusIcon(
    status: MessageStatus, modifier: Modifier = Modifier
) {
    val iconColor = when (status) {
        MessageStatus.READ -> Color(0xFF4FC3F7)       // azul claro
        MessageStatus.FAILED -> Color(0xFFEF5350)       // vermelho
        else -> Color.White.copy(alpha = 0.7f) // cinza/branco
    }

    when (status) {
        MessageStatus.SENDING -> Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Enviando",
            tint = iconColor,
            modifier = modifier.size(12.dp)
        )

        MessageStatus.SENT -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Enviado",
            tint = iconColor,
            modifier = modifier.size(12.dp)
        )

        MessageStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Entregue",
            tint = iconColor,
            modifier = modifier.size(12.dp)
        )

        MessageStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Lido",
            tint = iconColor,
            modifier = modifier.size(12.dp)
        )

        MessageStatus.FAILED -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Falha",
                tint = iconColor,
                modifier = modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "Falha", fontSize = 9.sp, color = iconColor
            )
        }
    }
}