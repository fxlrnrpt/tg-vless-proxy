package com.tgvlessproxy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tgvlessproxy.ui.screen.LoginScreen
import com.tgvlessproxy.ui.screen.MainScreen
import com.tgvlessproxy.ui.screen.ServerListScreen
import com.tgvlessproxy.viewmodel.MainViewModel

@Composable
fun AppNavigation(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val startDest = if (uiState.subscriptionUrl != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                proxyState = uiState.proxyState,
                errorMessage = uiState.errorMessage,
                onLogin = { url ->
                    viewModel.login(url)
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            )
        }
        composable("main") {
            MainScreen(
                uiState = uiState,
                telegramLink = viewModel.getTelegramLink(),
                onConfigClick = { navController.navigate("servers") },
                onLogout = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
            )
        }
        composable("servers") {
            ServerListScreen(
                servers = uiState.servers,
                selectedIndex = uiState.selectedServerIndex,
                onServerSelected = { index ->
                    viewModel.selectServer(index)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
