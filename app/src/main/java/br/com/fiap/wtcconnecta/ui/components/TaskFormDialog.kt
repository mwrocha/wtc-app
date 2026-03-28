package br.com.fiap.wtcconnecta.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fiap.wtcconnecta.data.model.TaskCategory
import br.com.fiap.wtcconnecta.data.model.TaskPriority
import br.com.fiap.wtcconnecta.data.model.TaskRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDialog(
    clientId: String = "",
    clientName: String = "",
    messageRef: String = "",
    onDismiss: () -> Unit,
    onConfirm: (TaskRequest) -> Unit
) {
    var title       by remember { mutableStateOf(
        if (messageRef.isNotBlank()) messageRef.take(60) else ""
    )}
    var description by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf(TaskCategory.OTHER) }
    var priority    by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate     by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nova Tarefa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mensagem de origem
                if (messageRef.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📩 \"${messageRef.take(80)}${if (messageRef.length > 80) "..." else ""}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Cliente
                if (clientName.isNotBlank()) {
                    Text(
                        text = "👤 Cliente: $clientName",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da tarefa *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Descrição
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                // Categoria
                Text("Categoria", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("${cat.emoji} ${cat.label}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Prioridade
                Text("Prioridade", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { pri ->
                        val color = when (pri) {
                            TaskPriority.HIGH   -> MaterialTheme.colorScheme.error
                            TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            TaskPriority.LOW    -> MaterialTheme.colorScheme.secondary
                        }
                        FilterChip(
                            selected = priority == pri,
                            onClick = { priority = pri },
                            label = { Text("${pri.emoji} ${pri.label}", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }

                // Prazo
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Prazo (ex: 25/04/2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            TaskRequest(
                                title       = title,
                                description = description,
                                clientId    = clientId,
                                clientName  = clientName,
                                category    = category.name,
                                priority    = priority.name,
                                messageRef  = messageRef,
                                dueDate     = dueDate
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Criar Tarefa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}