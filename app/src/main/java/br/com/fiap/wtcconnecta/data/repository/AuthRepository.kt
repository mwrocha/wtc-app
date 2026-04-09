package br.com.fiap.wtcconnecta.data.repository

import android.util.Log
import br.com.fiap.wtcconnecta.data.model.*
import br.com.fiap.wtcconnecta.data.model.Task
import br.com.fiap.wtcconnecta.data.model.TaskRequest
import br.com.fiap.wtcconnecta.data.remote.ApiService
import br.com.fiap.wtcconnecta.data.remote.LoginRequest
import br.com.fiap.wtcconnecta.data.remote.LoginResponse
import br.com.fiap.wtcconnecta.data.remote.MessageRequest
import br.com.fiap.wtcconnecta.data.remote.NoteRequest
import br.com.fiap.wtcconnecta.data.remote.RegisterRequest
import br.com.fiap.wtcconnecta.data.remote.RetrofitClient
import br.com.fiap.wtcconnecta.data.remote.UserPublicDto
import br.com.fiap.wtcconnecta.data.remote.GroupMessageRequest
import br.com.fiap.wtcconnecta.service.InAppNotificationState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthRepository(private val apiService: ApiService = RetrofitClient.instance) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                RetrofitClient.authToken = body.token
                registerFcmToken()
                Result.success(body)
            } else {
                Result.failure(Exception("Credenciais inválidas (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("AuthRepository", "FCM token obtido: $token")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        apiService.updateFcmToken(mapOf("token" to token))
                        Log.d("AuthRepository", "FCM token enviado ao servidor")
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Erro ao enviar FCM token: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Erro ao obter FCM token: ${e.message}")
            }
    }

    fun logout() {
        RetrofitClient.authToken = null
        InAppNotificationState.dismiss() // limpa banner ao fazer logout
    }

    // ── Cadastro ──────────────────────────────────────────────────────────────

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ): Result<Unit> {
        return try {
            val response = apiService.register(
                RegisterRequest(name = name, email = email, password = password, role = role)
            )
            if (response.isSuccessful) {
                response.body()?.token?.let { RetrofitClient.authToken = it }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro no cadastro (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Clientes ──────────────────────────────────────────────────────────────

    suspend fun getClients(): List<Client> = apiService.getClients()

    suspend fun searchClients(
        name: String? = null,
        tag: String? = null,
        groupId: String? = null
    ): List<Client> = apiService.searchClients(name, tag, groupId)

    suspend fun getClientById(id: String): Client = apiService.getClientById(id)

    suspend fun createClient(client: Client): Boolean =
        apiService.createClient(client).isSuccessful

    suspend fun updateClient(id: String, client: Client): Boolean =
        apiService.updateClient(id, client).isSuccessful

    // ── Perfil do cliente ────────────────────────────────────────────────────

    suspend fun getClientProfile(clientId: String): Client =
        apiService.getClientById(clientId)

    suspend fun updateClientProfile(clientId: String, name: String, groupId: String): Boolean {
        val client = apiService.getClientById(clientId)
        val updated = client.copy(name = name, groupId = groupId)
        return apiService.updateClient(clientId, updated).isSuccessful
    }

    // ── Anotações ─────────────────────────────────────────────────────────────

    suspend fun getNotesForClient(clientId: String): List<Note> =
        apiService.getNotesForClient(clientId)

    suspend fun createNote(clientId: String, text: String): Boolean =
        apiService.createNote(clientId, NoteRequest(text = text, clientId = clientId)).isSuccessful

    // ── Mensagens ─────────────────────────────────────────────────────────────

    suspend fun getConversation(conversationId: String): List<Message> =
        apiService.getConversation(conversationId)

    suspend fun getMyConversations(): List<Message> =
        apiService.getMyConversations()

    suspend fun getUnreadMessages(): List<Message> =
        apiService.getUnreadMessages()

    suspend fun getUnreadCount(): Int =
        apiService.getUnreadCount().count

    suspend fun sendGroupMessage(groupId: String, content: String): Boolean =
        apiService.sendGroupMessage(
            GroupMessageRequest(groupId = groupId, body = content)
        ).isSuccessful

    suspend fun sendMessage(receiverId: String, content: String, groupId: String? = null): Boolean =
        apiService.sendMessage(
            MessageRequest(recipientId = receiverId, body = content, groupId = groupId)
        ).isSuccessful

    // ── Usuários ──────────────────────────────────────────────────────────────

    suspend fun getUserByEmail(email: String): UserPublicDto? = try {
        apiService.getUserByEmail(email)
    } catch (e: Exception) { null }

    // ── Mensagens de grupo (filtro local) ────────────────────────────────────

    suspend fun getMessagesForDivision(divisionId: String): List<Message> {
        val groupIds = getGroupsByDivision(divisionId).map { it.id }
        return groupIds.flatMap { groupId ->
            try { apiService.getConversation(groupId) } catch (e: Exception) { emptyList() }
        }
    }

    // ── Divisões e Grupos ─────────────────────────────────────────────────────

    suspend fun getDivisions(): List<Division> = apiService.getDivisions()

    suspend fun createDivision(name: String): Division =
        apiService.createDivision(mapOf("name" to name))

    suspend fun updateDivision(id: String, name: String): Division =
        apiService.updateDivision(id, mapOf("name" to name))

    suspend fun deleteDivision(id: String) = apiService.deleteDivision(id)

    suspend fun getGroups(): List<Group> = apiService.getGroups()

    suspend fun getGroupsByDivision(divisionId: String): List<Group> =
        apiService.getGroupsByDivision(divisionId)

    suspend fun createGroup(name: String, divisionId: String): Group =
        apiService.createGroup(mapOf("name" to name, "divisionId" to divisionId))

    suspend fun updateGroup(id: String, name: String, divisionId: String): Group =
        apiService.updateGroup(id, mapOf("name" to name, "divisionId" to divisionId))

    suspend fun deleteGroup(id: String) = apiService.deleteGroup(id)

    // ── Campanhas ─────────────────────────────────────────────────────────────

    suspend fun getCampaigns(): List<Campaign> = apiService.getCampaigns()

    suspend fun getCampaignsForClient(clientId: String): List<Message> =
        apiService.getCampaignsForClient(clientId)
    suspend fun updateCampaign(id: String, request: CampaignRequest): Campaign = apiService.updateCampaign(id, request)
    suspend fun createCampaign(request: CampaignRequest): Campaign = apiService.createCampaign(request)
    suspend fun dispatchCampaign(id: String): Campaign = apiService.dispatchCampaign(id)

    // ── Mensagens — editar/excluir ───────────────────────────────────────────

    suspend fun editMessage(id: String, newContent: String): Result<Unit> {
        return try {
            val response = apiService.editMessage(id, mapOf("content" to newContent))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(
                when (response.code()) {
                    403  -> "Prazo de edição expirado (5 minutos)"
                    else -> "Erro ao editar mensagem"
                }
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteMessage(id: String): Result<Unit> {
        return try {
            val response = apiService.deleteMessage(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(
                when (response.code()) {
                    403  -> "Prazo de exclusão expirado (5 minutos)"
                    else -> "Erro ao excluir mensagem"
                }
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Solicitação de troca de grupo ────────────────────────────────────────

    suspend fun requestGroupChange(newGroupId: String, reason: String): Boolean {
        return try {
            val response = apiService.requestGroupChange(
                mapOf("newGroupId" to newGroupId, "reason" to reason)
            )
            response.isSuccessful
        } catch (e: Exception) { false }
    }

    // ── Senha e Email ─────────────────────────────────────────────────────────

    suspend fun changePassword(email: String, currentPassword: String, newPassword: String): Boolean {
        return try {
            val response = apiService.changePassword(
                mapOf("currentPassword" to currentPassword, "newPassword" to newPassword)
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun changeEmail(newEmail: String, password: String): Result<Unit> {
        return try {
            val response = apiService.changeEmail(
                mapOf("newEmail" to newEmail, "password" to password)
            )
            if (response.isSuccessful) Result.success(Unit)
            else {
                val code = response.code()
                Result.failure(Exception(
                    when (code) {
                        409  -> "Este e-mail já está em uso"
                        401  -> "Senha incorreta"
                        else -> "Erro ao alterar e-mail ($code)"
                    }
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Tarefas ───────────────────────────────────────────────────────────────

    suspend fun getTasks(): List<Task> = apiService.getTasks()
    suspend fun createTask(request: TaskRequest): Task = apiService.createTask(request)
    suspend fun getTasksByClient(clientId: String): List<Task> = apiService.getTasksByClient(clientId)
    suspend fun updateTaskStatus(taskId: String, status: String): Task =
        apiService.updateTaskStatus(taskId, mapOf("status" to status))
    suspend fun deleteTask(taskId: String) = apiService.deleteTask(taskId)
}