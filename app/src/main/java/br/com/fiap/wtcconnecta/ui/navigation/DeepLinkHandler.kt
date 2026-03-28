package br.com.fiap.wtcconnecta.ui.navigation

import androidx.navigation.NavController

/**
 * Deeplinks internos do WTCConnecta
 *
 * Formato: wtcconnecta://<destino>?param=valor
 *
 * Exemplos:
 *   wtcconnecta://chat                        → abre Minhas Conversas
 *   wtcconnecta://campaigns                   → abre Campanhas Express
 *   wtcconnecta://profile                     → abre Meu Perfil
 *   wtcconnecta://chat?clientId=xxx           → operador abre chat com cliente
 *   wtcconnecta://kanban?clientId=xxx         → operador abre Kanban filtrado
 */
object DeepLinkHandler {

    fun handle(url: String, navController: NavController, clientId: String = ""): Boolean {
        if (!url.startsWith("wtcconnecta://")) return false

        val uri    = android.net.Uri.parse(url)
        val host   = uri.host ?: return false
        val paramClientId = uri.getQueryParameter("clientId") ?: clientId

        return when (host) {
            "chat" -> {
                navController.navigate(Routes.ConversationList.createRoute(paramClientId))
                true
            }
            "campaigns" -> {
                navController.navigate(Routes.CampaignExpress.route)
                true
            }
            "profile" -> {
                if (paramClientId.isNotBlank())
                    navController.navigate(Routes.Profile.createRoute(paramClientId))
                true
            }
            "kanban" -> {
                navController.navigate(Routes.Kanban.route)
                true
            }
            else -> false
        }
    }
}