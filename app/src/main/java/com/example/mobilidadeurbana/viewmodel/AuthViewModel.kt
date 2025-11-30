package com.example.mobilidadeurbana.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
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
     * Login com verificação de nível de acesso 2 (motorista) ou 3 (admin)
     */
    fun loginUsuario(email: String, senha: String, onSuccess: (Boolean) -> Unit) {
        if (email.isBlank() || senha.isBlank()) {
            mostrarMensagem("Preencha todos os campos.")
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        // Verifica o nível de acesso
                        firestore.collection("usuarios")
                            .document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                val acesso = document.getLong("acesso")?.toInt()

                                when (acesso) {
                                    2 -> {
                                        mostrarMensagem("Login realizado com sucesso!")
                                        onSuccess(false) // Motorista
                                    }
                                    3 -> {
                                        mostrarMensagem("Login como Administrador")
                                        onSuccess(true) // Administrador
                                    }
                                    else -> {
                                        mostrarMensagem("Acesso negado. Apenas motoristas e administradores podem acessar.")
                                        auth.signOut()
                                    }
                                }
                            }
                            .addOnFailureListener {
                                mostrarMensagem("Erro ao verificar permissões. Tente novamente.")
                                auth.signOut()
                            }
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