package com.example.mobilidadeurbana.view.administrador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class OuvidoriaAdmin(
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
fun TelaGerenciarOuvidoria(onBack: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    val azulPrincipal = Color(0xFF0066FF)
    val verdeResolvido = Color(0xFF00C853)
    val laranjaPendente = Color(0xFFFF9800)

    var selectedTab by remember { mutableStateOf(0) }
    var ouvidoriasPendentes by remember { mutableStateOf<List<OuvidoriaAdmin>>(emptyList()) }
    var ouvidoriasResolvidas by remember { mutableStateOf<List<OuvidoriaAdmin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var categoriaFiltro by remember { mutableStateOf("Todas") }
    var ouvidoriaSelecionada by remember { mutableStateOf<OuvidoriaAdmin?>(null) }

    val categorias = listOf(
        "Todas",
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

    fun carregarOuvidorias() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("ouvidoria")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val todas = snapshot.documents.mapNotNull { doc ->
                    try {
                        OuvidoriaAdmin(
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

    fun filtrarOuvidorias(lista: List<OuvidoriaAdmin>): List<OuvidoriaAdmin> {
        var filtrada = lista

        if (categoriaFiltro != "Todas") {
            filtrada = filtrada.filter { it.categoria == categoriaFiltro }
        }

        if (searchQuery.isNotEmpty()) {
            filtrada = filtrada.filter {
                it.userName.contains(searchQuery, ignoreCase = true) ||
                        it.titulo.contains(searchQuery, ignoreCase = true) ||
                        it.descricao.contains(searchQuery, ignoreCase = true)
            }
        }

        return filtrada
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Gerenciar Ouvidorias", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { carregarOuvidorias() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
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
                }
            }
        },
        containerColor = Color(0xFFF0F7FF)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    var showCategoriaMenu by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = showCategoriaMenu,
                        onExpandedChange = { showCategoriaMenu = it }
                    ) {
                        OutlinedTextField(
                            value = categoriaFiltro,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filtrar por categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoriaMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                                        categoriaFiltro = cat
                                        showCategoriaMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = azulPrincipal)
                }
            } else {
                val lista = if (selectedTab == 0) ouvidoriasPendentes else ouvidoriasResolvidas
                val listaFiltrada = filtrarOuvidorias(lista)

                if (listaFiltrada.isEmpty()) {
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
                        items(listaFiltrada) { ouvidoria ->
                            CardOuvidoriaAdmin(
                                ouvidoria = ouvidoria,
                                onClick = { ouvidoriaSelecionada = it },
                                corStatus = if (selectedTab == 0) laranjaPendente else verdeResolvido
                            )
                        }
                    }
                }
            }
        }
    }

    ouvidoriaSelecionada?.let { ouvidoria ->
        DialogResponderOuvidoria(
            ouvidoria = ouvidoria,
            onDismiss = { ouvidoriaSelecionada = null },
            onSuccess = {
                ouvidoriaSelecionada = null
                carregarOuvidorias()
            },
            firestore = firestore,
            adminName = auth.currentUser?.displayName ?: "Admin"
        )
    }
}

@Composable
fun CardOuvidoriaAdmin(
    ouvidoria: OuvidoriaAdmin,
    onClick: (OuvidoriaAdmin) -> Unit,
    corStatus: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(ouvidoria) },
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
                "Usuário: ${ouvidoria.userName}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(Modifier.height(4.dp))

            Text(
                ouvidoria.descricao.take(80) + if (ouvidoria.descricao.length > 80) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogResponderOuvidoria(
    ouvidoria: OuvidoriaAdmin,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    firestore: FirebaseFirestore,
    adminName: String
) {
    val azulPrincipal = Color(0xFF0066FF)
    val verdeResolvido = Color(0xFF00C853)
    var resposta by rememberSaveable { mutableStateOf(ouvidoria.resposta) }
    var isLoading by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }

    fun marcarComoResolvida() {
        if (resposta.isEmpty()) {
            mensagem = "Por favor, escreva uma resposta antes de marcar como resolvida."
            return
        }

        isLoading = true

        val updates = hashMapOf<String, Any>(
            "resolvido" to true,
            "resposta" to resposta,
            "nomeAdministrador" to adminName,
            "dataResposta" to com.google.firebase.Timestamp.now(),
            "status" to "Resolvido"
        )

        firestore.collection("ouvidoria")
            .document(ouvidoria.id)
            .update(updates)
            .addOnSuccessListener {
                isLoading = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading = false
                mensagem = "Erro ao atualizar: ${e.message}"
            }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Responder Ouvidoria", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    Text("Categoria", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(ouvidoria.categoria)
                    Spacer(Modifier.height(12.dp))

                    Text("Título", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text(ouvidoria.titulo)
                    Spacer(Modifier.height(12.dp))

                    Text("Usuário", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Text("${ouvidoria.userName} (${ouvidoria.userEmail})")
                    Spacer(Modifier.height(12.dp))

                    Text("Descrição", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF0F7FF)
                        )
                    ) {
                        Text(
                            ouvidoria.descricao,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text("Sua Resposta", fontWeight = FontWeight.Bold, color = azulPrincipal)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resposta,
                        onValueChange = { resposta = it },
                        label = { Text("Digite sua resposta") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        maxLines = 10,
                        enabled = !isLoading && !ouvidoria.resolvido,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = azulPrincipal,
                            focusedLabelColor = azulPrincipal
                        )
                    )

                    if (mensagem.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            mensagem,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!ouvidoria.resolvido) {
                Button(
                    onClick = { marcarComoResolvida() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = verdeResolvido)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Marcar como Resolvida")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Fechar", color = Color.Gray)
            }
        }
    )
}