package com.example.mobilidadeurbana.view.administrador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mobilidadeurbana.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAdmin(
    onLogout: () -> Unit,
    navController: NavController,
    viewModel: AdminViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    val motoristas by viewModel.motoristas
    val isLoading by viewModel.isLoading
    var showConfirmLogout by remember { mutableStateOf(false) }

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    // Carrega motoristas
    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.carregarMotoristas()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administração", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = azulPrincipal),
                actions = {
                    TextButton(onClick = { showConfirmLogout = true }) {
                        Text("Sair", color = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("admin/criar-motorista") },
                containerColor = azulPrincipal
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar motorista", tint = Color.White)
            }
        },
        containerColor = Color(0xFFF0F7FF)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = azulPrincipal
                )
            } else {
                if (motoristas.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Nenhum motorista cadastrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Clique no botão + para adicionar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(motoristas) { motorista ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = azulPrincipal,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                motorista.nome,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = azulEscuro
                                            )
                                            Text(
                                                motorista.email,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                            Text(
                                                "UID: ${motorista.uid.take(8)}...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog de confirmação de logout
    if (showConfirmLogout) {
        AlertDialog(
            onDismissRequest = { showConfirmLogout = false },
            title = { Text("Sair", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente sair da área de administração?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmLogout = false
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }) {
                    Text("SIM", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmLogout = false }) {
                    Text("NÃO", color = Color.Gray)
                }
            }
        )
    }
}