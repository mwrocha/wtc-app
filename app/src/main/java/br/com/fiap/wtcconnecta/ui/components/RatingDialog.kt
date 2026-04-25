package br.com.fiap.wtcconnecta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WtcBlue     = Color(0xFF0B537B)
private val WtcBluePale = Color(0xFFEEF6FB)
private val WtcBlueHint = Color(0xFFD0E8F2)
private val TextPrimary = Color(0xFF0D2B3E)
private val TextMuted   = Color(0xFF6E90A0)
private val StarColor   = Color(0xFFFFB300)

@Composable
fun RatingDialog(
    onSubmit: (stars: Int, comment: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    var comment       by remember { mutableStateOf("") }

    val ratingLabel = when (selectedStars) {
        1 -> "Muito ruim"
        2 -> "Ruim"
        3 -> "Regular"
        4 -> "Bom"
        5 -> "Excelente!"
        else -> "Toque para avaliar"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(WtcBluePale),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null,
                        tint = StarColor, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Como foi seu atendimento?",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Sua avaliação nos ajuda a melhorar",
                    fontSize  = 12.sp,
                    color     = TextMuted,
                    textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Estrelas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector        = if (i <= selectedStars) Icons.Default.Star
                            else Icons.Default.StarBorder,
                            contentDescription = "$i estrelas",
                            tint               = if (i <= selectedStars) StarColor
                            else Color(0xFFCFD8DC),
                            modifier           = Modifier
                                .size(40.dp)
                                .clickable { selectedStars = i }
                        )
                    }
                }

                // Label dinâmico
                Text(ratingLabel,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selectedStars > 0) WtcBlue else TextMuted)

                // Comentário opcional
                OutlinedTextField(
                    value         = comment,
                    onValueChange = { comment = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Comentário opcional...",
                        color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp) },
                    shape         = RoundedCornerShape(12.dp),
                    minLines      = 2,
                    maxLines      = 4,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = WtcBlue,
                        unfocusedBorderColor = WtcBlueHint,
                        cursorColor          = WtcBlue
                    )
                )
            }
        },
        confirmButton = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick  = { onSubmit(selectedStars, comment.ifBlank { null }) },
                    enabled  = selectedStars > 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = WtcBlue)
                ) {
                    Text("Enviar avaliação", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agora não", color = TextMuted, fontSize = 13.sp)
                }
            }
        },
        dismissButton = {}
    )
}