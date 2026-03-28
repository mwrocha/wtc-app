package br.com.fiap.wtcconnecta.data.model

data class ActionButton(
        val action: String,
        val title: String
)

data class Campaign(
        val id: String = "",
        val title: String = "",
        val body: String = "",
        val url: String? = null,
        val actions: List<ActionButton>? = null,
        val actionUrls: Map<String, String>? = null,
        val status: String = "DRAFT",
        val targetTags: List<String>? = null,
        val targetClientIds: List<String>? = null,
        val targetGroupId: String? = null,
        val targetDivisionId: String? = null,
        val createdByUserId: String? = null,
        val scheduledAt: String? = null,
        val sentAt: String? = null,
        val createdAt: String? = null
)

data class CampaignRequest(
        val title: String,
        val body: String,
        val url: String? = null,
        val actions: List<ActionButton>? = null,
        val actionUrls: Map<String, String>? = null,
        val targetTags: List<String>? = null,
        val targetClientIds: List<String>? = null,
        val targetGroupId: String? = null,
        val targetDivisionId: String? = null,
        val scheduledAt: String? = null
)