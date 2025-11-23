package com.example.mobilidadeurbana.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.mutableStateOf

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var mensagem = mutableStateOf("")
        private set

    fun mostrarMensagem(texto: String) {
        mensagem.value = texto
    }

    fun limparMensagem() {
        mensagem.value = ""
    }

    /**
     * Cria usuário sem qualquer dado de imagem.
     * Salva apenas nome e email no Firestore.
     */
    fun cadastrarUsuario(
        nome: String,
        email: String,
        senha: String,
        onSuccess: () -> Unit
    ) {
        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            mostrarMensagem("Preencha todos os campos.")
            return
        }

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { createTask ->
                if (createTask.isSuccessful) {
                    val user = auth.currentUser ?: run {
                        mostrarMensagem("Erro interno ao criar conta.")
                        return@addOnCompleteListener
                    }

                    val userId = user.uid

                    // Atualiza apenas displayName (sem photoUrl)
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(nome)
                        .build()

                    user.updateProfile(profile).addOnCompleteListener {
                        val userData = hashMapOf(
                            "id" to userId,
                            "name" to nome,
                            "email" to email,
                            "cordx" to null,
                            "cordy" to null,
                            "mapaAtualId" to null
                        )

                        firestore.collection("usuarios")
                            .document(userId)
                            .set(userData)
                            .addOnCompleteListener {
                                user.sendEmailVerification()
                                    .addOnCompleteListener {
                                        mostrarMensagem("Conta criada com sucesso! Verifique seu e-mail.")
                                        onSuccess()
                                    }
                            }
                    }

                } else {
                    mostrarMensagem("Erro: ${createTask.exception?.message}")
                }
            }
    }

    fun loginUsuario(email: String, senha: String, onSuccess: () -> Unit) {
        if (email.isBlank() || senha.isBlank()) {
            mostrarMensagem("Preencha todos os campos.")
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        mostrarMensagem("Login realizado com sucesso!")
                        onSuccess()
                    } else {
                        user?.sendEmailVerification()
                        mostrarMensagem("Verifique seu e-mail antes de continuar.")
                        auth.signOut()
                    }
                } else {
                    mostrarMensagem("Erro: ${task.exception?.message}")
                }
            }
    }

    fun enviarResetSenha(email: String) {
        if (email.isBlank()) {
            mostrarMensagem("Informe o e-mail para resetar.")
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mostrarMensagem("E-mail de recuperação enviado.")
                } else {
                    mostrarMensagem("Erro: ${task.exception?.message}")
                }
            }
    }
}
