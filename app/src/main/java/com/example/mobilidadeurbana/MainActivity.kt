package com.example.mobilidadeurbana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilidadeurbana.view.TelaDeCadastro
import com.example.mobilidadeurbana.view.TelaDeLogin
import com.example.mobilidadeurbana.view.TelaHome
import com.example.mobilidadeurbana.view.TelaPerfil
import com.example.mobilidadeurbana.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val user = FirebaseAuth.getInstance().currentUser
            val startDestination = if (user != null) "home" else "login"

            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {

                        // Tela de Login
                        composable("login") {
                            TelaDeLogin(
                                viewModel = viewModel,
                                onNavigateToCadastro = { navController.navigate("cadastro") },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Tela de Cadastro
                        composable("cadastro") {
                            TelaDeCadastro(
                                viewModel = viewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("cadastro") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Tela Home (mapa + rastreamento)
                        composable("home") {
                            TelaHome(
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                navController = navController // 🔥 necessário para navegar até o perfil
                            )
                        }

                        // Tela de Perfil (nova)
                        composable("perfil") {
                            TelaPerfil(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
