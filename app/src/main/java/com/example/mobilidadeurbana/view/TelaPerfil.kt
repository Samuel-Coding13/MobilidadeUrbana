package com.example.mobilidadeurbana.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mobilidadeurbana.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPerfil(onBack: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var senhaAtual by rememberSaveable { mutableStateOf("") }
    var novaSenha by rememberSaveable { mutableStateOf("") }
    var confirmarNovaSenha by rememberSaveable { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var senhaAtualVisivel by rememberSaveable { mutableStateOf(false) }
    var novaSenhaVisivel by rememberSaveable { mutableStateOf(false) }
    var confirmarSenhaVisivel by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = Color(0xFFF0F7FF)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.outline_bus_alert_24),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(azulPrincipal)
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Informações do Usuário",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nome: ", fontWeight = FontWeight.Bold, color = azulEscuro)
                        Text(user?.displayName ?: "—", color = Color.Gray)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Email: ", fontWeight = FontWeight.Bold, color = azulEscuro)
                        Text(user?.email ?: "—", color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
            ) {
                Text("Alterar Senha", fontWeight = FontWeight.Bold)
            }

            if (mensagem.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = mensagem,
                    color = if (mensagem.contains("sucesso", true))
                        Color(0xFF00C853)
                    else
                        MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showChangePasswordDialog = false
                    senhaAtual = ""
                    novaSenha = ""
                    confirmarNovaSenha = ""
                    mensagem = ""
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            senhaAtual.isEmpty() || novaSenha.isEmpty() || confirmarNovaSenha.isEmpty() -> {
                                mensagem = "Preencha todos os campos."
                            }
                            novaSenha != confirmarNovaSenha -> {
                                mensagem = "As senhas não coincidem."
                            }
                            novaSenha.length < 6 -> {
                                mensagem = "A senha deve ter pelo menos 6 caracteres."
                            }
                            else -> {
                                isLoading = true
                                val email = user?.email
                                if (email != null) {
                                    val credential = EmailAuthProvider.getCredential(email, senhaAtual)
                                    user.reauthenticate(credential)
                                        .addOnSuccessListener {
                                            user.updatePassword(novaSenha)
                                                .addOnSuccessListener {
                                                    mensagem = "Senha alterada com sucesso!"
                                                    isLoading = false
                                                    senhaAtual = ""
                                                    novaSenha = ""
                                                    confirmarNovaSenha = ""
                                                    showChangePasswordDialog = false
                                                }
                                                .addOnFailureListener { e ->
                                                    mensagem = "Erro ao alterar senha: ${e.message}"
                                                    isLoading = false
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            mensagem = "Senha atual incorreta."
                                            isLoading = false
                                        }
                                } else {
                                    mensagem = "Erro: usuário não autenticado."
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = azulPrincipal
                        )
                    } else {
                        Text("Alterar", color = azulPrincipal)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePasswordDialog = false
                        senhaAtual = ""
                        novaSenha = ""
                        confirmarNovaSenha = ""
                        mensagem = ""
                    },
                    enabled = !isLoading
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            title = {
                Text("Alterar Senha", color = azulEscuro, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Preencha os campos abaixo para alterar sua senha:")
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = senhaAtual,
                        onValueChange = { senhaAtual = it },
                        label = { Text("Senha atual") },
                        visualTransformation = if (senhaAtualVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { senhaAtualVisivel = !senhaAtualVisivel }) {
                                Icon(
                                    imageVector = if (senhaAtualVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (senhaAtualVisivel) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = novaSenha,
                        onValueChange = { novaSenha = it },
                        label = { Text("Nova senha") },
                        visualTransformation = if (novaSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { novaSenhaVisivel = !novaSenhaVisivel }) {
                                Icon(
                                    imageVector = if (novaSenhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (novaSenhaVisivel) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmarNovaSenha,
                        onValueChange = { confirmarNovaSenha = it },
                        label = { Text("Confirmar nova senha") },
                        visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) {
                                Icon(
                                    imageVector = if (confirmarSenhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmarSenhaVisivel) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }
        )
    }
}