package br.com.fiap.wtcconnecta.data.model

import com.google.gson.annotations.SerializedName

data class Task(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val clientId: String = "",
        val clientName: String = "",
        val operatorId: String = "",
        val category: String = "OTHER",   // BILLING, SUPPORT, COMMERCIAL, OTHER
        val priority: String = "MEDIUM",  // HIGH, MEDIUM, LOW
        val status: String = "PENDING",   // PENDING, IN_PROGRESS, DONE
        val messageRef: String = "",
        val dueDate: String = "",
        val createdAt: String = "",
        val updatedAt: String = ""
)

data class TaskRequest(
        val title: String,
        val description: String = "",
        val clientId: String = "",
        val clientName: String = "",
        val category: String = "OTHER",
        val priority: String = "MEDIUM",
        val status: String = "PENDING",
        val messageRef: String = "",
        val dueDate: String = ""
)

// Enums para UI
enum class TaskCategory(val label: String, val emoji: String) {
    BILLING("Cobrança", "💰"),
    SUPPORT("Suporte", "🔧"),
    COMMERCIAL("Comercial", "📈"),
    OTHER("Outros", "📌")
}

enum class TaskPriority(val label: String, val emoji: String) {
    HIGH("Alta", "🔴"),
    MEDIUM("Média", "🟡"),
    LOW("Baixa", "🟢")
}

enum class TaskStatus(val label: String) {
    PENDING("Pendente"),
    IN_PROGRESS("Em andamento"),
    DONE("Concluída")
}