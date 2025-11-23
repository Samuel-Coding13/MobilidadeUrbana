package com.example.mobilidadeurbana.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mobilidadeurbana.viewmodel.AuthViewModel

@Composable
fun TelaDeCadastro(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }

    val mensagem by viewModel.mensagem

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Cadastro de Usuário",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            // Observação: campo de foto foi removido conforme solicitado.
            Text(
                "Observação: o cadastro agora não solicita ou armazena fotos de perfil.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmarSenha,
                onValueChange = { confirmarSenha = it },
                label = { Text("Confirmar senha") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (nome.isBlank() || email.isBlank() || senha.isBlank() || confirmarSenha.isBlank()) {
                        viewModel.mostrarMensagem("Preencha todos os campos.")
                        return@Button
                    }

                    if (senha != confirmarSenha) {
                        viewModel.mostrarMensagem("As senhas não coincidem.")
                        return@Button
                    }

                    // Chamamos o viewModel sem qualquer imagem
                    viewModel.cadastrarUsuario(nome, email, senha) {
                        onNavigateToLogin()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cadastrar")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Já tem conta? Faça login")
            }

            Spacer(Modifier.height(16.dp))

            if (mensagem.isNotEmpty()) {
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
