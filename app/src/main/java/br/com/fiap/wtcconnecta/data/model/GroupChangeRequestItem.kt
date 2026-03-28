package br.com.fiap.wtcconnecta.data.model

data class GroupChangeRequestItem(
    val id: String = "",
    val clientName: String = "",
    val clientEmail: String = "",
    val currentGroupName: String = "",
    val requestedGroupName: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val createdAt: String = ""
)