package com.example.mobilidadeurbana.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class OuvidoriaItem(
    val id: String = "",
    val categoria: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val localizacao: String = "",
    val telefone: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val status: String = "Pendente",
    val resolvido: Boolean = false,
    val resposta: String = "",
    val nomeAdministrador: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val dataResposta: com.google.firebase.Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaOuvidoria(onBack: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    val azulPrincipal = Color(0xFF0066FF)
    val azulEscuro = Color(0xFF003366)
    val verdeResolvido = Color(0xFF00C853)
    val laranjaPendente = Color(0xFFFF9800)

    var selectedTab by remember { mutableStateOf(0) }
    var ouvidoriasPendentes by remember { mutableStateOf<List<OuvidoriaItem>>(emptyList()) }
    var ouvidoriasResolvidas by remember { mutableStateOf<List<OuvidoriaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var ouvidoriaSelecionada by remember { mutableStateOf<OuvidoriaItem?>(null) }

    fun carregarOuvidorias() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("ouvidoria")
                    .whereEqualTo("userId", user?.uid ?: "")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val todas = snapshot.documents.mapNotNull { doc ->
                    try {
                        OuvidoriaItem(
                            id = doc.id,
                            categoria = doc.getString("categoria") ?: "",
                            titulo = doc.getString("titulo") ?: "",
                            descricao = doc.getString("descricao") ?: "",
                            localizacao = doc.getString("localizacao") ?: "",
                            telefone = doc.getString("telefone") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            userName = doc.getString("userName") ?: "",
                            status = doc.getString("status") ?: "Pendente",
                            resolvido = doc.getBoolean("resolvido") ?: false,
                            resposta = doc.getString("resposta") ?: "",
                            nomeAdministrador = doc.getString("nomeAdministrador") ?: "",
                            timestamp = doc.getTimestamp("timestamp"),
                            dataResposta = doc.getTimestamp("dataResposta")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                ouvidoriasPendentes = todas.filter { !it.resolvido }
                ouvidoriasResolvidas = todas.filter { it.resolvido }
            } catch (e: Exception) {
                // Erro ao carregar
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        carregarOuvidorias()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Ouvidoria", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = azulPrincipal)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = azulPrincipal
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pendentes", color = Color.White)
                                if (ouvidoriasPendentes.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(containerColor = laranjaPendente) {
                                        Text("${ouvidoriasPendentes.size}", color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Resolvidas", color = Color.White)
                                if (ouvidoriasResolvidas.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(containerColor = verdeResolvido) {
                                        Text("${ouvidoriasResolvidas.size}", color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Nova", color = Color.White) }
                    )
                }
            }
        },
        containerColor = Color(0xFFF0F7FF)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> ListaOuvidorias(
                    ouvidorias = ouvidoriasPendentes,
                    isLoading = isLoading,
                    onRefresh = { carregarOuvidorias() },
                    onClick = { ouvidoriaSelecionada = it },
                    corStatus = laranjaPendente
                )
                1 -> ListaOuvidorias(
                    ouvidorias = ouvidoriasResolvidas,
                    isLoading = isLoading,
                    onRefresh = { carregarOuvidorias() },
                    onClick = { ouvidoriaSelecionada = it },
                    corStatus = verdeResolvido
                )
                2 -> FormularioNovaOuvidoria(
                    firestore = firestore,
                    user = user,
                    onSuccess = {
                        carregarOuvidorias()
                        selectedTab = 0
                    }
                )
            }
        }
    }

    ouvidoriaSelecionada?.let { ouvidoria ->
        DialogDetalhesOuvidoria(
            ouvidoria = ouvidoria,
            onDismiss = { ouvidoriaSelecionada = null }
        )
    }
}

@Composable
fun ListaOuvidorias(
    ouvidorias: List<OuvidoriaItem>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onClick: (OuvidoriaItem) -> Unit,
    corStatus: Color
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0066FF))
        }
    } else if (ouvidorias.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Nenhuma ouvidoria encontrada",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ouvidorias) { ouvidoria ->
                CardOuvidoria(
                    ouvidoria = ouvidoria,
                    onClick = { onClick(ouvidoria) },
                    corStatus = corStatus
                )
            }
        }
    }
}

@Composable
fun CardOuvidoria(
    ouvidoria: OuvidoriaItem,
    onClick: () -> Unit,
    corStatus: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(12.dp).background(corStatus, shape = MaterialTheme.shapes.small)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ouvidoria.categoria,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
                ouvidoria.timestamp?.let {
                    Text(
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                ouvidoria.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF003366)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                ouvidoria.descricao.take(100) + if (ouvidoria.descricao.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            if (ouvidoria.resolvido && ouvidoria.resposta.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Respondido por ${ouvidoria.nomeAdministrador}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00C853)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogDetalhesOuvidoria(
    ouvidoria: OuvidoriaItem,
    onDismiss: () -> Unit
) {
    val azulPrincipal = Color(0xFF0066FF)
    val verdeResolvido = Color(0xFF00C853)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes da Ouvidoria", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    Text("Categoria", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(ouvidoria.categoria)
                    Spacer(Modifier.height(12.dp))

                    Text("Título", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(ouvidoria.titulo)
                    Spacer(Modifier.height(12.dp))

                    Text("Descrição", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(ouvidoria.descricao)
                    Spacer(Modifier.height(12.dp))

                    if (ouvidoria.localizacao.isNotEmpty()) {
                        Text("Localização", fontWeight = FontWeight.Bold, color = azulPrincipal)
                        Text(ouvidoria.localizacao)
                        Spacer(Modifier.height(12.dp))
                    }

                    if (ouvidoria.telefone.isNotEmpty()) {
                        Text("Telefone", fontWeight = FontWeight.Bold, color = azulPrincipal)
                        Text(ouvidoria.telefone)
                        Spacer(Modifier.height(12.dp))
                    }

                    Text("Data", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(
                        ouvidoria.timestamp?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it.toDate())
                        } ?: "—"
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("Protocolo", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(ouvidoria.id.take(8).uppercase())

                    if (ouvidoria.resolvido) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))

                        Text("Status", fontWeight = FontWeight.Bold, color = verdeResolvido)
                        Text("Resolvida", color = verdeResolvido)
                        Spacer(Modifier.height(12.dp))

                        if (ouvidoria.resposta.isNotEmpty()) {
                            Text("Resposta do Administrador", fontWeight = FontWeight.Bold, color = azulPrincipal)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = verdeResolvido.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    ouvidoria.resposta,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Por: ${ouvidoria.nomeAdministrador}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            ouvidoria.dataResposta?.let {
                                Text(
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = azulPrincipal)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioNovaOuvidoria(
    firestore: FirebaseFirestore,
    user: com.google.firebase.auth.FirebaseUser?,
    onSuccess: () -> Unit
) {
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
            "resolvido" to false,
            "resposta" to "",
            "nomeAdministrador" to "",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("ouvidoria")
            .document(reportId)
            .set(relatorio)
            .addOnSuccessListener {
                isLoading = false
                mensagem = "Relatório enviado com sucesso! Protocolo: ${reportId.take(8).uppercase()}"
                categoria = ""
                titulo = ""
                descricao = ""
                localizacao = ""
                telefone = ""
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading = false
                mensagem = "Erro ao enviar: ${e.message}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
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

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição detalhada *") },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    maxLines = 8,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = azulPrincipal,
                        focusedLabelColor = azulPrincipal
                    ),
                    supportingText = { Text("Descreva o problema com o máximo de detalhes possível") }
                )

                Spacer(Modifier.height(16.dp))

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

                OutlinedTextField(
                    value = telefone,
                    onValueChange = {
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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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