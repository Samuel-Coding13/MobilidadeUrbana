package com.example.mobilidadeurbana.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mobilidadeurbana.util.BlinkText
import com.example.mobilidadeurbana.viewmodel.AuthViewModel

@Composable
fun TelaDeLogin(
    viewModel: AuthViewModel,
    onNavigateToCadastro: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    val mensagem by viewModel.mensagem

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by rememberSaveable { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Login", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(onClick = { viewModel.loginUsuario(email, senha) { onLoginSuccess() } }, modifier = Modifier.fillMaxWidth()) {
                Text("Entrar")
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onNavigateToCadastro) {
                    Text("Não tem conta? Cadastre-se")
                }
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Esqueci a senha")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (mensagem.isNotEmpty()) {
                BlinkText(text = mensagem)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.enviarResetSenha(resetEmail)
                    showResetDialog = false
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Recuperar senha") },
            text = {
                Column {
                    Text("Informe o e-mail para receber o link de recuperação:")
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
