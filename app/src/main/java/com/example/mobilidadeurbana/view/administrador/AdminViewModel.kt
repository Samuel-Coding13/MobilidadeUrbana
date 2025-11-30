package com.example.mobilidadeurbana.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Usuario(
    val uid: String = "",
    val nome: String = "",
    val email: String = "",
    val acesso: Int = 0,
    val ativo: Boolean = true
)

class AdminViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var mensagem = mutableStateOf("")
        private set

    var motoristas = mutableStateOf<List<Usuario>>(emptyList())
        private set

    var administradores = mutableStateOf<List<Usuario>>(emptyList())
        private set

    // NOVO: Listas filtradas para pesquisa
    var motoristasFiltrados = mutableStateOf<List<Usuario>>(emptyList())
        private set

    var administradoresFiltrados = mutableStateOf<List<Usuario>>(emptyList())
        private set

    // NOVO: Texto de pesquisa
    var searchQuery = mutableStateOf("")
        private set

    var isLoading = mutableStateOf(false)
        private set

    fun mostrarMensagem(texto: String) {
        mensagem.value = texto
    }

    fun limparMensagem() {
        mensagem.value = ""
    }

    /**
     * NOVO: Atualiza o texto de pesquisa e filtra os usuários
     *
     * @param query Texto digitado na barra de pesquisa
     * @param tipo "motorista" ou "administrador"
     */
    fun updateSearchQuery(query: String, tipo: String) {
        searchQuery.value = query

        when (tipo) {
            "motorista" -> {
                motoristasFiltrados.value = if (query.isBlank()) {
                    motoristas.value
                } else {
                    motoristas.value.filter { usuario ->
                        usuario.nome.contains(query, ignoreCase = true) ||
                                usuario.email.contains(query, ignoreCase = true)
                    }
                }
            }
            "administrador" -> {
                administradoresFiltrados.value = if (query.isBlank()) {
                    administradores.value
                } else {
                    administradores.value.filter { usuario ->
                        usuario.nome.contains(query, ignoreCase = true) ||
                                usuario.email.contains(query, ignoreCase = true)
                    }
                }
            }
        }
    }

    /**
     * NOVO: Limpa a pesquisa
     */
    fun limparPesquisa(tipo: String) {
        searchQuery.value = ""
        when (tipo) {
            "motorista" -> motoristasFiltrados.value = motoristas.value
            "administrador" -> administradoresFiltrados.value = administradores.value
        }
    }

    /**
     * Carrega lista de motoristas (acesso = 2)
     * MODIFICADO: Também atualiza a lista filtrada
     */
    suspend fun carregarMotoristas() {
        isLoading.value = true
        try {
            val snapshot = firestore.collection("usuarios")
                .whereEqualTo("acesso", 2)
                .get()
                .await()

            val listaMotoristas = snapshot.documents.mapNotNull { doc ->
                Usuario(
                    uid = doc.id,
                    nome = doc.getString("nome") ?: "",
                    email = doc.getString("email") ?: "",
                    acesso = doc.getLong("acesso")?.toInt() ?: 0,
                    ativo = doc.getBoolean("ativo") ?: true
                )
            }

            motoristas.value = listaMotoristas
            motoristasFiltrados.value = listaMotoristas // Inicializa a lista filtrada
        } catch (e: Exception) {
            mostrarMensagem("Erro ao carregar motoristas: ${e.message}")
        } finally {
            isLoading.value = false
        }
    }

    /**
     * Carrega lista de administradores (acesso = 3)
     * MODIFICADO: Também atualiza a lista filtrada
     */
    suspend fun carregarAdministradores() {
        isLoading.value = true
        try {
            val snapshot = firestore.collection("usuarios")
                .whereEqualTo("acesso", 3)
                .get()
                .await()

            val listaAdmins = snapshot.documents.mapNotNull { doc ->
                Usuario(
                    uid = doc.id,
                    nome = doc.getString("nome") ?: "",
                    email = doc.getString("email") ?: "",
                    acesso = doc.getLong("acesso")?.toInt() ?: 0,
                    ativo = doc.getBoolean("ativo") ?: true
                )
            }

            administradores.value = listaAdmins
            administradoresFiltrados.value = listaAdmins // Inicializa a lista filtrada
        } catch (e: Exception) {
            mostrarMensagem("Erro ao carregar administradores: ${e.message}")
        } finally {
            isLoading.value = false
        }
    }

    /**
     * Cria novo motorista (acesso = 2)
     */
    fun cadastrarMotorista(
        nome: String,
        email: String,
        senha: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        cadastrarUsuario(nome, email, senha, 2, onSuccess, onError)
    }

    /**
     * Cria novo administrador (acesso = 3)
     */
    fun cadastrarAdministrador(
        nome: String,
        email: String,
        senha: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        cadastrarUsuario(nome, email, senha, 3, onSuccess, onError)
    }

    /**
     * Função genérica para cadastrar usuário
     */
    private fun cadastrarUsuario(
        nome: String,
        email: String,
        senha: String,
        nivelAcesso: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            onError("Preencha todos os campos.")
            return
        }

        if (senha.length < 6) {
            onError("A senha deve ter pelo menos 6 caracteres.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onError("Formato de e-mail inválido.")
            return
        }

        isLoading.value = true

        val adminUser = auth.currentUser

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { createTask ->
                if (createTask.isSuccessful) {
                    val newUser = auth.currentUser ?: run {
                        isLoading.value = false
                        onError("Erro interno ao criar conta.")
                        return@addOnCompleteListener
                    }

                    val userId = newUser.uid

                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(nome)
                        .build()

                    newUser.updateProfile(profile).addOnCompleteListener {
                        val userData = hashMapOf(
                            "id" to userId,
                            "nome" to nome,
                            "email" to email,
                            "acesso" to nivelAcesso,
                            "ativo" to true
                        )

                        firestore.collection("usuarios")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                newUser.sendEmailVerification()
                                    .addOnCompleteListener {
                                        auth.signOut()

                                        if (adminUser != null) {
                                            adminUser.reload()
                                        }

                                        isLoading.value = false
                                        onSuccess()
                                    }
                            }
                            .addOnFailureListener { e ->
                                isLoading.value = false
                                onError("Erro ao salvar dados: ${e.message}")
                            }
                    }

                } else {
                    isLoading.value = false
                    val errorMsg = when {
                        createTask.exception?.message?.contains("already in use") == true ->
                            "Este e-mail já está cadastrado."
                        createTask.exception?.message?.contains("weak password") == true ->
                            "Senha muito fraca. Use pelo menos 6 caracteres."
                        else -> "Erro: ${createTask.exception?.message}"
                    }
                    onError(errorMsg)
                }
            }
    }

    /**
     * Atualiza dados de um usuário
     */
    fun atualizarUsuario(
        uid: String,
        nome: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (nome.isBlank() || email.isBlank()) {
            onError("Preencha todos os campos.")
            return
        }

        isLoading.value = true

        val updates = hashMapOf<String, Any>(
            "nome" to nome,
            "email" to email
        )

        firestore.collection("usuarios")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                isLoading.value = false
                mostrarMensagem("Usuário atualizado com sucesso")
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                onError("Erro ao atualizar: ${e.message}")
            }
    }

    /**
     * Ativa ou desativa um usuário
     */
    fun toggleUsuarioAtivo(
        uid: String,
        ativoAtual: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        isLoading.value = true

        firestore.collection("usuarios")
            .document(uid)
            .update("ativo", !ativoAtual)
            .addOnSuccessListener {
                isLoading.value = false
                mostrarMensagem(if (!ativoAtual) "Usuário ativado" else "Usuário desativado")
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                onError("Erro ao alterar status: ${e.message}")
            }
    }

    /**
     * Exclui um usuário
     */
    fun excluirUsuario(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        isLoading.value = true

        firestore.collection("usuarios")
            .document(uid)
            .delete()
            .addOnSuccessListener {
                isLoading.value = false
                mostrarMensagem("Usuário excluído com sucesso")
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                onError("Erro ao excluir: ${e.message}")
            }
    }
}