package br.com.fiap.wtcconnecta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.rememberNavController
import br.com.fiap.wtcconnecta.ui.components.InAppNotificationBanner
import br.com.fiap.wtcconnecta.ui.navigation.NavGraph
import br.com.fiap.wtcconnecta.ui.theme.WTCConnectaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WTCConnectaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    Box(modifier = Modifier.fillMaxSize()) {

                        NavGraph(navController = navController)

                        // Banner clicável com acesso ao navController
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                                .zIndex(10f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            InAppNotificationBanner(navController = navController)
                        }
                    }
                }
            }
        }
    }
}