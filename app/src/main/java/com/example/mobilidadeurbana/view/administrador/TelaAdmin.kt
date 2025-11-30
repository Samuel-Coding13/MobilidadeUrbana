package com.example.mobilidadeurbana.view.administrador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.mobilidadeurbana.viewmodel.Usuario
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

    val motoristas by viewModel.motoristasFiltrados // MODIFICADO: Usa lista filtrada
    val administradores by viewModel.administradoresFiltrados // MODIFICADO: Usa lista filtrada
    val isLoading by viewModel.isLoading
    val searchQuery by viewModel.searchQuery

    var showConfirmLogout by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    // Carrega dados quando muda de aba
    LaunchedEffect(selectedTab) {
        scope.launch {
            when (selectedTab) {
                0 -> viewModel.carregarMotoristas()
                1 -> viewModel.carregarAdministradores()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Administração", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = azulPrincipal),
                    actions = {
                        TextButton(onClick = { showConfirmLogout = true }) {
                            Text("Sair", color = Color.White)
                        }
                    }
                )

                // NOVO: Barra de pesquisa
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.updateSearchQuery(
                            it,
                            if (selectedTab == 0) "motorista" else "administrador"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Pesquisar por nome ou email...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.limparPesquisa(
                                    if (selectedTab == 0) "motorista" else "administrador"
                                )
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar pesquisa")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = azulPrincipal,
                        focusedLeadingIconColor = azulPrincipal
                    )
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = azulPrincipal
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            viewModel.limparPesquisa("motorista")
                        },
                        text = { Text("Motoristas", color = Color.White) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            viewModel.limparPesquisa("administrador")
                        },
                        text = { Text("Administradores", color = Color.White) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> navController.navigate("admin/criar-motorista")
                        1 -> navController.navigate("admin/criar-admin")
                    }
                },
                containerColor = azulPrincipal
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
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
                when (selectedTab) {
                    0 -> ListaUsuarios(
                        usuarios = motoristas,
                        viewModel = viewModel,
                        navController = navController,
                        tipo = "Motorista",
                        searchQuery = searchQuery
                    )
                    1 -> ListaUsuarios(
                        usuarios = administradores,
                        viewModel = viewModel,
                        navController = navController,
                        tipo = "Administrador",
                        searchQuery = searchQuery
                    )
                }
            }
        }
    }

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

@Composable
private fun ListaUsuarios(
    usuarios: List<Usuario>,
    viewModel: AdminViewModel,
    navController: NavController,
    tipo: String,
    searchQuery: String
) {
    val scope = rememberCoroutineScope()
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    var usuarioParaEditar by remember { mutableStateOf<Usuario?>(null) }
    var usuarioParaExcluir by remember { mutableStateOf<Usuario?>(null) }
    var usuarioParaToggle by remember { mutableStateOf<Usuario?>(null) }

    if (usuarios.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // MODIFICADO: Ícone diferente para cada tipo
            Icon(
                if (tipo == "Motorista") Icons.Default.DirectionsBus else Icons.Default.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (searchQuery.isEmpty()) "Nenhum $tipo cadastrado" else "Nenhum resultado encontrado",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (searchQuery.isEmpty()) "Clique no botão + para adicionar" else "Tente pesquisar com outros termos",
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
            items(usuarios) { usuario ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (usuario.ativo) Color.White else Color.LightGray.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // NOVO: Ícone diferente para motorista e administrador
                            Icon(
                                imageVector = if (tipo == "Motorista") {
                                    Icons.Default.DirectionsBus // Ícone de ônibus para motorista
                                } else {
                                    Icons.Default.AdminPanelSettings // Ícone de escudo para admin
                                },
                                contentDescription = null,
                                tint = azulPrincipal,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        usuario.nome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = azulEscuro
                                    )
                                    if (!usuario.ativo) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "INATIVO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    usuario.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Text(
                                    "UID: ${usuario.uid.take(8)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Botão Editar
                            TextButton(onClick = { usuarioParaEditar = usuario }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = azulPrincipal)
                                Spacer(Modifier.width(4.dp))
                                Text("Editar", color = azulPrincipal)
                            }

                            // Botão Ativar/Desativar
                            TextButton(onClick = { usuarioParaToggle = usuario }) {
                                Icon(
                                    if (usuario.ativo) Icons.Default.Close else Icons.Default.Check,
                                    contentDescription = if (usuario.ativo) "Desativar" else "Ativar",
                                    tint = if (usuario.ativo) Color(0xFFFF9800) else Color(0xFF00C853)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (usuario.ativo) "Desativar" else "Ativar",
                                    color = if (usuario.ativo) Color(0xFFFF9800) else Color(0xFF00C853)
                                )
                            }

                            // Botão Excluir
                            TextButton(onClick = { usuarioParaExcluir = usuario }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                                Spacer(Modifier.width(4.dp))
                                Text("Excluir", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog de Edição
    usuarioParaEditar?.let { usuario ->
        DialogEditarUsuario(
            usuario = usuario,
            onDismiss = { usuarioParaEditar = null },
            onConfirm = { nome, email ->
                viewModel.atualizarUsuario(
                    uid = usuario.uid,
                    nome = nome,
                    email = email,
                    onSuccess = {
                        usuarioParaEditar = null
                        scope.launch {
                            if (tipo == "Motorista") viewModel.carregarMotoristas()
                            else viewModel.carregarAdministradores()
                        }
                    },
                    onError = { msg ->
                        viewModel.mostrarMensagem(msg)
                    }
                )
            }
        )
    }

    // Dialog de Exclusão
    usuarioParaExcluir?.let { usuario ->
        AlertDialog(
            onDismissRequest = { usuarioParaExcluir = null },
            title = { Text("Excluir $tipo", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente excluir ${usuario.nome}?\nEsta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirUsuario(
                        uid = usuario.uid,
                        onSuccess = {
                            usuarioParaExcluir = null
                            scope.launch {
                                if (tipo == "Motorista") viewModel.carregarMotoristas()
                                else viewModel.carregarAdministradores()
                            }
                        },
                        onError = { msg ->
                            viewModel.mostrarMensagem(msg)
                        }
                    )
                }) {
                    Text("EXCLUIR", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioParaExcluir = null }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }

    // Dialog de Ativar/Desativar
    usuarioParaToggle?.let { usuario ->
        AlertDialog(
            onDismissRequest = { usuarioParaToggle = null },
            title = {
                Text(
                    if (usuario.ativo) "Desativar $tipo" else "Ativar $tipo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Deseja realmente ${if (usuario.ativo) "desativar" else "ativar"} ${usuario.nome}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleUsuarioAtivo(
                        uid = usuario.uid,
                        ativoAtual = usuario.ativo,
                        onSuccess = {
                            usuarioParaToggle = null
                            scope.launch {
                                if (tipo == "Motorista") viewModel.carregarMotoristas()
                                else viewModel.carregarAdministradores()
                            }
                        },
                        onError = { msg ->
                            viewModel.mostrarMensagem(msg)
                        }
                    )
                }) {
                    Text(
                        if (usuario.ativo) "DESATIVAR" else "ATIVAR",
                        color = if (usuario.ativo) Color(0xFFFF9800) else Color(0xFF00C853),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioParaToggle = null }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun DialogEditarUsuario(
    usuario: Usuario,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nome by remember { mutableStateOf(usuario.nome) }
    var email by remember { mutableStateOf(usuario.email) }

    val azulPrincipal = Color(0xFF0066FF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Usuário", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nome, email) }) {
                Text("SALVAR", color = azulPrincipal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray)
            }
        }
    )
}