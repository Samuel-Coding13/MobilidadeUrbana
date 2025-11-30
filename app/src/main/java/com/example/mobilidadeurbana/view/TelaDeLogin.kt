package com.example.mobilidadeurbana.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import com.example.mobilidadeurbana.R
import com.example.mobilidadeurbana.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDeLogin(
    viewModel: AuthViewModel,
    onNavigateToCadastro: () -> Unit,
    onLoginSuccess: (Boolean) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var emailReset by rememberSaveable { mutableStateOf("") }

    val mensagem by viewModel.mensagem

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulClaro = Color(0xFF00D4FF)
    val azulEscuro = Color(0xFF003366)

    LaunchedEffect(Unit) {
        viewModel.limparMensagem()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = azulPrincipal)
            )
        },
        containerColor = Color(0xFFF0F7FF)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.outline_bus_alert_24),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        colorFilter = ColorFilter.tint(azulPrincipal)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Bem-vindo!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Esqueci a senha", color = azulPrincipal)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            when {
                                email.isEmpty() || senha.isEmpty() -> {
                                    viewModel.mostrarMensagem("Preencha todos os campos.")
                                }
                                else -> {
                                    viewModel.limparMensagem()
                                    viewModel.loginUsuario(email, senha) { isAdmin ->
                                        onLoginSuccess(isAdmin)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = azulPrincipal),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (mensagem.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = mensagem,
                            color = if (mensagem.contains("sucesso", true))
                                azulPrincipal
                            else
                                MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Dialog de Reset de Senha
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Recuperar Senha", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Informe o e-mail para receber o link de recuperação:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailReset,
                        onValueChange = { emailReset = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.enviarResetSenha(emailReset)
                    showResetDialog = false
                    emailReset = ""
                }) {
                    Text("Enviar", color = azulPrincipal)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    emailReset = ""
                }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}