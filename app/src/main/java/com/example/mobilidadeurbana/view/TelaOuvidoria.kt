package com.example.mobilidadeurbana.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaOuvidoria(onBack: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // CORES AZUIS
    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)

    var categoria by rememberSaveable { mutableStateOf("") }
    var titulo by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var localizacao by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var showCategoriaMenu by remember { mutableStateOf(false) }

    val categorias = listOf(
        "Atraso no ônibus",
        "Ônibus não passou",
        "Superlotação",
        "Problemas com motorista",
        "Veículo em más condições",
        "Ponto de ônibus danificado",
        "Falta de informação nos pontos",
        "Sugestão de melhoria",
        "Elogio",
        "Outro"
    )

    fun enviarRelatorio() {
        when {
            categoria.isEmpty() -> {
                mensagem = "Selecione uma categoria."
                return
            }
            titulo.isEmpty() -> {
                mensagem = "Preencha o título."
                return
            }
            descricao.isEmpty() -> {
                mensagem = "Preencha a descrição."
                return
            }
        }

        isLoading = true

        val reportId = java.util.UUID.randomUUID().toString()
        val relatorio = hashMapOf(
            "id" to reportId,
            "categoria" to categoria,
            "titulo" to titulo,
            "descricao" to descricao,
            "localizacao" to localizacao,
            "telefone" to telefone,
            "userId" to (user?.uid ?: ""),
            "userEmail" to (user?.email ?: ""),
            "userName" to (user?.displayName ?: "Anônimo"),
            "status" to "Pendente",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("ouvidoria")
            .document(reportId)
            .set(relatorio)
            .addOnSuccessListener {
                isLoading = false
                mensagem = "Relatório enviado com sucesso! Protocolo: ${reportId.take(8).uppercase()}"
                // Limpar campos
                categoria = ""
                titulo = ""
                descricao = ""
                localizacao = ""
                telefone = ""
            }
            .addOnFailureListener { e ->
                isLoading = false
                mensagem = "Erro ao enviar: ${e.message}"
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ouvidoria", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Envie seu feedback ou reporte um problema",
                style = MaterialTheme.typography.titleMedium,
                color = azulEscuro,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // CATEGORIA (Dropdown)
                    ExposedDropdownMenuBox(
                        expanded = showCategoriaMenu,
                        onExpandedChange = { showCategoriaMenu = it }
                    ) {
                        OutlinedTextField(
                            value = categoria,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoria *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoriaMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = azulPrincipal,
                                focusedLabelColor = azulPrincipal
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = showCategoriaMenu,
                            onDismissRequest = { showCategoriaMenu = false }
                        ) {
                            categorias.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        categoria = cat
                                        showCategoriaMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // TÍTULO
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        label = { Text("Título do problema *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // DESCRIÇÃO
                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição detalhada *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        ),
                        supportingText = { Text("Descreva o problema com o máximo de detalhes possível") }
                    )

                    Spacer(Modifier.height(16.dp))

                    // LOCALIZAÇÃO
                    OutlinedTextField(
                        value = localizacao,
                        onValueChange = { localizacao = it },
                        label = { Text("Localização (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        ),
                        placeholder = { Text("Ex: Ponto próximo ao Shopping") }
                    )

                    Spacer(Modifier.height(16.dp))

                    // TELEFONE DE CONTATO
                    OutlinedTextField(
                        value = telefone,
                        onValueChange = {
                            // Aceita apenas números
                            if (it.all { char -> char.isDigit() || char == ' ' || char == '(' || char == ')' || char == '-' }) {
                                telefone = it
                            }
                        },
                        label = { Text("Telefone de contato (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        ),
                        placeholder = { Text("(00) 00000-0000") },
                        supportingText = { Text("Para retorno sobre sua solicitação") }
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { enviarRelatorio() },
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
                            Text("Enviar Relatório", fontWeight = FontWeight.Bold)
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
                        "ℹ️ Informações Importantes",
                        fontWeight = FontWeight.Bold,
                        color = azulEscuro
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Suas informações serão tratadas com confidencialidade\n" +
                                "• Você receberá um número de protocolo para acompanhamento\n" +
                                "• O prazo de resposta é de até 7 dias úteis\n" +
                                "• Campos marcados com * são obrigatórios",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}