package com.example.mobilidadeurbana

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobilidadeurbana.view.*
import com.example.mobilidadeurbana.viewmodel.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String = "login"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            TelaDeLogin(
                viewModel = authViewModel,
                onNavigateToCadastro = { navController.navigate("cadastro") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("cadastro") {
            TelaDeCadastro(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("cadastro") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            TelaHome(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        composable("perfil") {
            TelaPerfil(
                onBack = { navController.popBackStack() }
            )
        }
    }
}