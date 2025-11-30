package com.example.mobilidadeurbana

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobilidadeurbana.view.*
import com.example.mobilidadeurbana.view.administrador.TelaAdmin
import com.example.mobilidadeurbana.view.administrador.TelaCriarMotorista
import com.example.mobilidadeurbana.viewmodel.AuthViewModel
import com.example.mobilidadeurbana.viewmodel.AdminViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String = "login"
) {
    // ViewModel do Admin é criado uma vez e compartilhado entre as telas
    val adminViewModel: AdminViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // TELA DE LOGIN
        composable("login") {
            TelaDeLogin(
                viewModel = authViewModel,
                onNavigateToCadastro = {
                    // Navega para a tela de cadastro (se existir)
                    // Por enquanto, pode deixar vazio ou mostrar mensagem
                },
                onLoginSuccess = { isAdmin ->
                    if (isAdmin) {
                        // Redireciona para área administrativa
                        navController.navigate("admin") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        // Redireciona para área do motorista
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // TELA DO MOTORISTA (HOME)
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

        // TELA DE PERFIL
        composable("perfil") {
            TelaPerfil(
                onBack = { navController.popBackStack() }
            )
        }

        // TELA DE OUVIDORIA
        composable("ouvidoria") {
            TelaOuvidoria(
                onBack = { navController.popBackStack() }
            )
        }

        // TELA PRINCIPAL DO ADMINISTRADOR
        composable("admin") {
            TelaAdmin(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("admin") { inclusive = true }
                    }
                },
                navController = navController,
                viewModel = adminViewModel
            )
        }

        // TELA DE CRIAR MOTORISTA
        composable("admin/criar-motorista") {
            TelaCriarMotorista(
                onBack = { navController.popBackStack() },
                viewModel = adminViewModel
            )
        }
    }
}