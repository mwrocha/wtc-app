package br.com.fiap.wtcconnecta.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import br.com.fiap.wtcconnecta.ui.screens.auth.LoginScreen
import br.com.fiap.wtcconnecta.ui.screens.auth.RegisterScreen
import br.com.fiap.wtcconnecta.ui.screens.client.ChatScreen
import br.com.fiap.wtcconnecta.ui.screens.client.ConversationListScreen
import br.com.fiap.wtcconnecta.ui.screens.client.HomeClientScreen
import br.com.fiap.wtcconnecta.ui.screens.client.CampaignExpressScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.CampaignScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.KanbanScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.AuditScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.GroupRequestsScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.GroupManagementScreen
import br.com.fiap.wtcconnecta.ui.screens.profile.OperatorProfileScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.ClientDetailScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.HomeOperatorScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.OperatorDashboardScreen
import br.com.fiap.wtcconnecta.ui.screens.profile.ProfileScreen
import br.com.fiap.wtcconnecta.ui.screens.client.ClientAttendanceHistoryScreen
import br.com.fiap.wtcconnecta.ui.screens.operator.AttendanceQueueScreen
import br.com.fiap.wtcconnecta.viewmodel.ChatViewModel
import br.com.fiap.wtcconnecta.viewmodel.LoginResult
import br.com.fiap.wtcconnecta.viewmodel.MainViewModel
import br.com.fiap.wtcconnecta.data.repository.AuthRepository

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")
    object OperatorDashboard : Routes("operator_dashboard")
    object OperatorClients : Routes("operator_clients")
    object Campaigns : Routes("campaigns")
    object CampaignExpress : Routes("campaign_express")
    object Kanban : Routes("kanban")
    object GroupManagement : Routes("group_management")
    object Audit : Routes("audit")
    object GroupRequests : Routes("group_requests")
    object AttendanceQueue : Routes("attendance_queue")
    object OperatorProfile : Routes("operator_profile/{operatorId}") {
        fun createRoute(operatorId: String) = "operator_profile/$operatorId"
    }
    object HomeClient : Routes("home_client/{clientId}") {
        fun createRoute(clientId: String) = "home_client/$clientId"
    }
    object ConversationList : Routes("conversation_list/{clientId}") {
        fun createRoute(clientId: String) = "conversation_list/$clientId"
    }
    object Profile : Routes("profile/{clientId}") {
        fun createRoute(clientId: String) = "profile/$clientId"
    }
    object Settings : Routes("settings")
    object ClientDetail : Routes("client_detail/{clientId}") {
        fun createRoute(clientId: String) = "client_detail/$clientId"
    }
    object Chat : Routes("chat/{chatId}/{chatName}/{chatType}")
    object AttendanceHistory : Routes("attendance_history")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel()
) {
    val userSession by mainViewModel.userSession.collectAsState()

    NavHost(navController = navController, startDestination = Routes.Login.route) {

        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = { result: LoginResult ->
                    mainViewModel.onLoginSuccess(result)
                    val navOptions = navOptions {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    if (result.role == "operador") {
                        navController.navigate(Routes.OperatorDashboard.route, navOptions)
                    } else {
                        navController.navigate(Routes.HomeClient.createRoute(result.userId), navOptions)
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.Register.route) }
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.OperatorDashboard.route) {
            val session = userSession
            if (session != null && session.role == "operador") {
                OperatorDashboardScreen(
                    operatorName              = session.name,
                    onViewClients             = { navController.navigate(Routes.OperatorClients.route) },
                    onNavigateToProfile       = { navController.navigate(Routes.OperatorProfile.createRoute(session.id)) },
                    onNavigateToSettings      = { navController.navigate(Routes.Settings.route) },
                    onNavigateToCampaigns     = { navController.navigate(Routes.Campaigns.route) },
                    onNavigateToKanban        = { navController.navigate(Routes.Kanban.route) },
                    onNavigateToGroupManagement = { navController.navigate(Routes.GroupManagement.route) },
                    onNavigateToAudit         = { navController.navigate(Routes.Audit.route) },
                    onNavigateToGroupRequests = { navController.navigate(Routes.GroupRequests.route) },
                    onNavigateToAttendanceQueue = { navController.navigate(Routes.AttendanceQueue.route) },
                    onLogout = {
                        mainViewModel.logout()
                        AuthRepository().logout()
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.OperatorClients.route) {
            val session = userSession
            if (session != null && session.role == "operador") {
                HomeOperatorScreen(
                    currentOperatorId   = session.id,
                    onNavigateToProfile = { navController.navigate(Routes.OperatorProfile.createRoute(session.id)) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                    onClientClick       = { clientId -> navController.navigate(Routes.ClientDetail.createRoute(clientId)) }
                )
            }
        }

        composable(Routes.Campaigns.route) {
            CampaignScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ClientDetail.route) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")
            val session  = userSession
            if (clientId != null && session != null && session.role == "operador") {
                ClientDetailScreen(
                    clientId          = clientId,
                    onBack            = { navController.popBackStack() },
                    currentOperatorId = session.id,
                    navController     = navController
                )
            }
        }

        composable(Routes.HomeClient.route) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")
            val session  = userSession
            if (clientId != null) {
                HomeClientScreen(
                    clientId     = clientId,
                    clientName   = session?.name ?: "",
                    onNavigateToConversationList = {
                        navController.navigate(Routes.ConversationList.createRoute(clientId))
                    },
                    onNavigateToProfile   = { navController.navigate(Routes.Profile.createRoute(clientId)) },
                    onNavigateToCampaigns = { navController.navigate(Routes.CampaignExpress.route) },
                    onNavigateToHistory   = { navController.navigate(Routes.AttendanceHistory.route) },
                    onLogout = {
                        mainViewModel.logout()
                        AuthRepository().logout()
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.ConversationList.route) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")
            if (clientId != null) {
                ConversationListScreen(
                    clientId = clientId,
                    onNavigateToChat = { chatId, chatName, chatType ->
                        navController.navigate("chat/$chatId/$chatName/$chatType")
                    }
                )
            }
        }

        composable(Routes.Chat.route) { backStackEntry ->
            val chatId   = backStackEntry.arguments?.getString("chatId")
            val chatName = backStackEntry.arguments?.getString("chatName")
            val chatType = backStackEntry.arguments?.getString("chatType")
            val session  = userSession
            if (chatId != null && chatName != null && chatType != null && session != null) {
                val chatViewModel: ChatViewModel = viewModel()
                val loggedInUserId = session.email
                LaunchedEffect(chatId, chatType, loggedInUserId) {
                    chatViewModel.loadMessages(chatId, chatType, loggedInUserId)
                }
                ChatScreen(
                    chatId         = chatId,
                    chatName       = chatName,
                    chatType       = chatType,
                    onBack         = { navController.popBackStack() },
                    loggedInUserId = loggedInUserId,
                    viewModel      = chatViewModel
                )
            }
        }

        composable(Routes.CampaignExpress.route) {
            val session = userSession
            CampaignExpressScreen(
                onBack        = { navController.popBackStack() },
                clientId      = session?.id ?: "",
                navController = navController
            )
        }

        composable(Routes.Kanban.route) {
            KanbanScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GroupManagement.route) {
            GroupManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GroupRequests.route) {
            GroupRequestsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.Audit.route) {
            val session = userSession
            AuditScreen(
                onBack           = { navController.popBackStack() },
                currentUserEmail = session?.email ?: ""
            )
        }

        composable(Routes.OperatorProfile.route) { backStackEntry ->
            val operatorId = backStackEntry.arguments?.getString("operatorId") ?: ""
            OperatorProfileScreen(
                operatorId     = operatorId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Profile.route) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            ProfileScreen(
                clientId         = clientId,
                onProfileUpdated = { navController.popBackStack() },
                onNavigateBack   = { navController.popBackStack() }
            )
        }

        composable(Routes.AttendanceHistory.route) {
            ClientAttendanceHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AttendanceQueue.route) {
            AttendanceQueueScreen(
                onBack = { navController.popBackStack() },
                onAssumeAndNavigate = { clientId, _ ->
                    // Após assumir → abre ClientDetailScreen do cliente
                    navController.navigate(Routes.ClientDetail.createRoute(clientId)) {
                        popUpTo(Routes.AttendanceQueue.route) { inclusive = true }
                    }
                },
                // Atendimento já ativo → abre ClientDetailScreen (chat completo com abas)
                onNavigateToActive = { clientId, _ ->
                    navController.navigate(Routes.ClientDetail.createRoute(clientId))
                },
                // Sessão encerrada → abre ClientDetailScreen para ver histórico
                onNavigateToClosed = { clientId ->
                    navController.navigate(Routes.ClientDetail.createRoute(clientId))
                }
            )
        }
    }
}