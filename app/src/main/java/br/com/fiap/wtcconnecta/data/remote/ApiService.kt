package br.com.fiap.wtcconnecta.data.remote

import br.com.fiap.wtcconnecta.data.model.Campaign
import br.com.fiap.wtcconnecta.data.model.CampaignRequest
import br.com.fiap.wtcconnecta.data.model.Client
import br.com.fiap.wtcconnecta.data.model.Division
import br.com.fiap.wtcconnecta.data.model.Group
import br.com.fiap.wtcconnecta.data.model.Message
import br.com.fiap.wtcconnecta.data.model.Note
import br.com.fiap.wtcconnecta.data.model.Task
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.data.model.GroupChangeRequestItem
import br.com.fiap.wtcconnecta.ui.screens.operator.AuditLogItem
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val id: String, val role: String, val email: String, val name: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val role: String)
data class RegisterResponse(val token: String, val userId: String)
data class NoteRequest(val text: String, val clientId: String)
data class MessageRequest(val recipientId: String, val title: String? = null, val body: String, val groupId: String? = null)
data class UnreadCountResponse(val count: Int)
data class GroupMessageRequest(val groupId: String, val title: String? = null, val body: String)
data class UserPublicDto(val id: String, val name: String, val email: String, val role: String)

// ── Upload de Imagem ──────────────────────────────────────────────────────────
data class UploadResponse(
    val objectKey: String,
    val url: String,
    val contentType: String,
    val size: Long
)

data class PresignedUrlResponse(
    val url: String
)

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // ── Clients ───────────────────────────────────────────────────────────────
    @GET("api/clients")
    suspend fun getClients(): List<Client>

    @GET("api/clients")
    suspend fun searchClients(
        @Query("name") name: String? = null,
        @Query("tag") tag: String? = null,
        @Query("groupId") groupId: String? = null
    ): List<Client>

    @GET("api/clients/{id}")
    suspend fun getClientById(@Path("id") id: String): Client

    @POST("api/clients")
    suspend fun createClient(@Body client: Client): Response<Client>

    @PUT("api/clients/{id}")
    suspend fun updateClient(@Path("id") id: String, @Body client: Client): Response<Client>

    // ── Notes ─────────────────────────────────────────────────────────────────
    @GET("api/clients/{id}/notes")
    suspend fun getNotesForClient(@Path("id") clientId: String): List<Note>

    @POST("api/clients/{id}/notes")
    suspend fun createNote(@Path("id") clientId: String, @Body note: NoteRequest): Response<Note>

    // ── Campaigns (operador) ──────────────────────────────────────────────────
    @GET("api/campaigns")
    suspend fun getCampaigns(): List<Campaign>

    @POST("api/campaigns")
    suspend fun createCampaign(@Body request: CampaignRequest): Campaign

    @PUT("api/campaigns/{id}")
    suspend fun updateCampaign(@Path("id") id: String, @Body request: CampaignRequest): Campaign

    @POST("api/campaigns/{id}/dispatch")
    suspend fun dispatchCampaign(@Path("id") id: String): Campaign

    // ── Campaigns Express (cliente) ───────────────────────────────────────────
    @GET("api/campaigns-received")
    suspend fun getMyCampaigns(): List<Message>

    // ── Messages ──────────────────────────────────────────────────────────────
    @POST("api/messages/direct")
    suspend fun sendMessage(@Body message: MessageRequest): Response<Message>

    @GET("api/messages/conversation/{conversationId}")
    suspend fun getConversation(@Path("conversationId") conversationId: String): List<Message>

    @GET("api/messages/unread")
    suspend fun getUnreadMessages(): List<Message>

    @GET("api/messages/unread/count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @POST("api/messages/group")
    suspend fun sendGroupMessage(@Body message: GroupMessageRequest): Response<Message>

    @GET("api/messages/my-conversations")
    suspend fun getMyConversations(): List<Message>

    // ── Users ─────────────────────────────────────────────────────────────────
    @GET("api/users/by-email/{email}")
    suspend fun getUserByEmail(@Path("email") email: String): UserPublicDto

    @POST("api/users/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): Response<Unit>

    @POST("api/users/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<Unit>

    @POST("api/users/change-email")
    suspend fun changeEmail(@Body body: Map<String, String>): Response<Unit>

    // ── Divisions & Groups ────────────────────────────────────────────────────
    @GET("api/divisions")
    suspend fun getDivisions(): List<Division>

    @POST("api/divisions")
    suspend fun createDivision(@Body body: Map<String, String>): Division

    @PUT("api/divisions/{id}")
    suspend fun updateDivision(@Path("id") id: String, @Body body: Map<String, String>): Division

    @DELETE("api/divisions/{id}")
    suspend fun deleteDivision(@Path("id") id: String): Response<Unit>

    @GET("api/groups")
    suspend fun getGroups(): List<Group>

    @GET("api/groups")
    suspend fun getGroupsByDivision(@Query("divisionId") divisionId: String): List<Group>

    @POST("api/groups")
    suspend fun createGroup(@Body body: Map<String, String>): Group

    @PUT("api/groups/{id}")
    suspend fun updateGroup(@Path("id") id: String, @Body body: Map<String, String>): Group

    @DELETE("api/groups/{id}")
    suspend fun deleteGroup(@Path("id") id: String): Response<Unit>

    // ── Tasks ─────────────────────────────────────────────────────────────────
    @POST("api/tasks")
    suspend fun createTask(@Body request: TaskRequest): Task

    @GET("api/tasks")
    suspend fun getTasks(): List<Task>

    @GET("api/tasks/client/{clientId}")
    suspend fun getTasksByClient(@Path("clientId") clientId: String): List<Task>

    @PATCH("api/tasks/{taskId}/status")
    suspend fun updateTaskStatus(
        @Path("taskId") taskId: String,
        @Body body: Map<String, String>
    ): Task

    @DELETE("api/tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: String): Response<Unit>

    // ── Mensagens — editar/excluir ────────────────────────────────────────────
    @PUT("api/messages/{id}")
    suspend fun editMessage(@Path("id") id: String, @Body body: Map<String, String>): Response<Message>

    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: String): Response<Unit>

    // ── Solicitação de troca de grupo ─────────────────────────────────────────
    @POST("api/group-change-requests")
    suspend fun requestGroupChange(@Body body: Map<String, String>): Response<Unit>

    // ── Solicitações de troca de grupo (operador) ─────────────────────────────
    @GET("api/group-change-requests")
    suspend fun getGroupChangeRequests(): List<GroupChangeRequestItem>

    @POST("api/group-change-requests/{id}/approve")
    suspend fun approveGroupChangeRequest(@Path("id") id: String): Response<Unit>

    @POST("api/group-change-requests/{id}/reject")
    suspend fun rejectGroupChangeRequest(@Path("id") id: String): Response<Unit>

    // ── Auditoria ─────────────────────────────────────────────────────────────
    @GET("api/audit")
    suspend fun getAuditLogs(): List<AuditLogItem>

    // ── Upload de Imagem (Cloudflare R2) ──────────────────────────────────────
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @GET("api/upload/presigned")
    suspend fun getPresignedUrl(
        @Query("key") objectKey: String
    ): Response<PresignedUrlResponse>

    @Multipart
    @POST("api/users/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>

    @GET("api/users/avatar")
    suspend fun getMyAvatar(): Response<Map<String, String>>

    @DELETE("api/users/avatar")
    suspend fun deleteAvatar(): Response<Map<String, String>>

    @PATCH("api/messages/conversation/{conversationId}/read")
    suspend fun markConversationAsRead(
        @Path("conversationId") conversationId: String
    ): Response<Unit>
}