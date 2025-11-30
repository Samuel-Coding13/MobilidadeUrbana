package com.example.mobilidadeurbana.view.administrador

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilidadeurbana.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCriarAdmin(
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmarSenha by rememberSaveable { mutableStateOf("") }
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }
    var confirmarSenhaVisivel by rememberSaveable { mutableStateOf(false) }

    var mensagem by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Administrador", color = Color.White, fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Cadastrar novo administrador",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha") },
                        visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                                Icon(
                                    imageVector = if (senhaVisivel) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (senhaVisivel) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        ),
                        supportingText = { Text("Mínimo 6 caracteres") }
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmarSenha,
                        onValueChange = { confirmarSenha = it },
                        label = { Text("Confirmar senha") },
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
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            when {
                                nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty() -> {
                                    mensagem = "Preencha todos os campos."
                                }
                                senha != confirmarSenha -> {
                                    mensagem = "As senhas não coincidem."
                                }
                                senha.length < 6 -> {
                                    mensagem = "A senha deve ter pelo menos 6 caracteres."
                                }
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                    mensagem = "Formato de e-mail inválido."
                                }
                                else -> {
                                    mensagem = ""
                                    viewModel.cadastrarAdministrador(
                                        nome = nome,
                                        email = email,
                                        senha = senha,
                                        onSuccess = {
                                            mensagem = "Administrador criado com sucesso! Um e-mail de verificação foi enviado."
                                            nome = ""
                                            email = ""
                                            senha = ""
                                            confirmarSenha = ""
                                        },
                                        onError = { error ->
                                            mensagem = error
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Criar Administrador", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (mensagem.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (mensagem.contains("sucesso", true))
                                    Color(0xFF00C853).copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = mensagem,
                                modifier = Modifier.padding(12.dp),
                                color = if (mensagem.contains("sucesso", true))
                                    Color(0xFF00C853)
                                else
                                    MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = azulPrincipal.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ℹ️ Informações",
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• O administrador será criado com nível de acesso 3\n" +
                                "• Terá acesso total ao painel administrativo\n" +
                                "• Um e-mail de verificação será enviado automaticamente\n" +
                                "• O administrador deverá verificar o e-mail antes do primeiro login",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}