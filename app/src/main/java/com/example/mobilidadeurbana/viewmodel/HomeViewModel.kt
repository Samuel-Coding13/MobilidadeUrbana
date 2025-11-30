package com.example.mobilidadeurbana.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estado da rota selecionada (persistente)
    var rotaSelecionada = mutableStateOf("")
        private set

    // Status do ônibus
    var statusOnibus = mutableStateOf("Parado")
        private set

    // Estado do rastreamento
    var isTracking = mutableStateOf(false)
        private set

    // Job para controlar o loop de atualização
    private var trackingJob: Job? = null

    /**
     * Define a rota selecionada
     */
    fun setRota(rota: String) {
        rotaSelecionada.value = rota
    }

    /**
     * Define o status do ônibus
     */
    fun setStatus(status: String) {
        statusOnibus.value = status
    }

    /**
     * Inicia o rastreamento com atualização a cada 5 segundos
     */
    fun startTracking(latitude: Double, longitude: Double) {
        if (isTracking.value) return

        isTracking.value = true

        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isTracking.value) {
                updateLocationInFirebase(latitude, longitude)
                delay(5000) // Atualiza a cada 5 segundos
            }
        }
    }

    /**
     * Atualiza a localização no Firebase em tempo real
     */
    fun updateLocationInFirebase(latitude: Double, longitude: Double) {
        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value

        if (rota.isEmpty()) return

        val locationData = hashMapOf(
            "uid" to user.uid,
            "lat" to latitude,
            "lng" to longitude,
            "status" to statusOnibus.value,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("onibus")
            .document(rota)
            .collection("veiculos")
            .document(user.uid)
            .set(locationData)
            .addOnFailureListener { e ->
                // Log de erro silencioso
                e.printStackTrace()
            }
    }

    /**
     * Para o rastreamento e remove do Firebase
     */
    fun stopTracking() {
        isTracking.value = false
        trackingJob?.cancel()
        trackingJob = null

        // Remove do Firebase
        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value

        if (rota.isNotEmpty()) {
            firestore.collection("onibus")
                .document(rota)
                .collection("veiculos")
                .document(user.uid)
                .delete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}