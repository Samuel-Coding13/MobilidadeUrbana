package com.example.mobilidadeurbana.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Motorista(
    val uid: String = "",
    val nome: String = "",
    val email: String = ""
)

class AdminViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var mensagem = mutableStateOf("")
        private set

    var motoristas = mutableStateOf<List<Motorista>>(emptyList())
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
     * Carrega lista de motoristas do Firestore
     */
    suspend fun carregarMotoristas() {
        isLoading.value = true
        try {
            val snapshot = firestore.collection("usuarios")
                .whereEqualTo("acesso", 2)
                .get()
                .await()

            motoristas.value = snapshot.documents.mapNotNull { doc ->
                Motorista(
                    uid = doc.id,
                    nome = doc.getString("nome") ?: "",
                    email = doc.getString("email") ?: ""
                )
            }
        } catch (e: Exception) {
            mostrarMensagem("Erro ao carregar motoristas: ${e.message}")
        } finally {
            isLoading.value = false
        }
    }

    /**
     * Cria novo motorista (nível 2)
     */
    fun cadastrarMotorista(
        nome: String,
        email: String,
        senha: String,
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

        // Salva o usuário atual (admin) para reautenticar depois
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

                    // Atualiza displayName
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(nome)
                        .build()

                    newUser.updateProfile(profile).addOnCompleteListener {
                        val userData = hashMapOf(
                            "id" to userId,
                            "nome" to nome,
                            "email" to email,
                            "acesso" to 2, // Motorista é nível 2
                            "cordx" to null,
                            "cordy" to null,
                            "mapaAtualId" to null
                        )

                        firestore.collection("usuarios")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                // Envia email de verificação
                                newUser.sendEmailVerification()
                                    .addOnCompleteListener {
                                        // Desloga o motorista recém criado
                                        auth.signOut()

                                        // Reautentica o admin se ainda estiver disponível
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
     * Exclui um motorista (opcional - se quiser implementar)
     */
    fun excluirMotorista(uid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        isLoading.value = true

        firestore.collection("usuarios")
            .document(uid)
            .delete()
            .addOnSuccessListener {
                isLoading.value = false
                mostrarMensagem("Motorista excluído com sucesso")
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                onError("Erro ao excluir: ${e.message}")
            }
    }

    /**
     * Atualiza dados de um motorista (opcional)
     */
    fun atualizarMotorista(
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
                mostrarMensagem("Motorista atualizado com sucesso")
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                onError("Erro ao atualizar: ${e.message}")
            }
    }
}