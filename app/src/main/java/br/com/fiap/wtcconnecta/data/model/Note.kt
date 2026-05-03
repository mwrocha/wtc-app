package br.com.fiap.wtcconnecta.data.model

import com.google.gson.annotations.SerializedName

data class Note(
    val id: String = "",

    @SerializedName("content") val text: String = "",

    val createdAt: String? = null,
    val clientId: String? = null,
    val operatorId: String? = null,
    val updatedAt: String? = null
)