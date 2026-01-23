package com.example.mobilidadeurbana

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobilidadeurbana.view.*
import com.example.mobilidadeurbana.view.administrador.TelaAdmin
import com.example.mobilidadeurbana.view.administrador.TelaCriarMotorista
import com.example.mobilidadeurbana.view.administrador.TelaCriarAdmin
import com.example.mobilidadeurbana.view.administrador.TelaGerenciarOuvidoria
import com.example.mobilidadeurbana.viewmodel.AuthViewModel
import com.example.mobilidadeurbana.viewmodel.AdminViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String = "login"
) {
    val adminViewModel: AdminViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            TelaDeLogin(
                viewModel = authViewModel,
                onNavigateToCadastro = {},
                onLoginSuccess = { isAdmin ->
                    if (isAdmin) {
                        navController.navigate("admin") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
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

        composable("ouvidoria") {
            TelaOuvidoria(
                onBack = { navController.popBackStack() }
            )
        }

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

        composable("admin/criar-motorista") {
            TelaCriarMotorista(
                onBack = { navController.popBackStack() },
                viewModel = adminViewModel
            )
        }

        composable("admin/criar-admin") {
            TelaCriarAdmin(
                onBack = { navController.popBackStack() },
                viewModel = adminViewModel
            )
        }

        composable("admin/gerenciar-ouvidoria") {
            TelaGerenciarOuvidoria(
                onBack = { navController.popBackStack() }
            )
        }
    }
}