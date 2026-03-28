package br.com.fiap.wtcconnecta.data.model

data class Client(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "CLIENT",
    val phone: String? = null,
    val status: String? = null,
    val score: Int = 0,
    val tags: List<String> = emptyList(),  // nunca null
    val divisionId: String? = null,
    val groupId: String? = null,
    val noteIds: List<String> = emptyList(),
    val active: Boolean = true
)