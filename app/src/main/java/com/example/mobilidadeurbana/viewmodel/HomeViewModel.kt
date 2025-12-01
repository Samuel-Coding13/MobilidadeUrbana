package com.example.mobilidadeurbana.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Rota(
    val codigo: String = "",
    val nome: String = "",
    val cor: String = "#0066FF",
    val pontos: List<PontoRota> = emptyList(),
    val paradas: List<Parada> = emptyList()
)

data class PontoRota(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Parada(
    val id: String = "",
    val nome: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Veiculo(
    val uid: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista de todas as rotas disponíveis
    var rotas = mutableStateOf<List<Rota>>(emptyList())
        private set

    // Rota selecionada completa
    var rotaSelecionada = mutableStateOf<Rota?>(null)
        private set

    // Veículos da rota selecionada
    var veiculos = mutableStateOf<List<Veiculo>>(emptyList())
        private set

    // Status do ônibus
    var statusOnibus = mutableStateOf("Parado")
        private set

    // Estado do rastreamento
    var isTracking = mutableStateOf(false)
        private set

    // Job para controlar o loop de atualização
    private var trackingJob: Job? = null

    // Listener para veículos em tempo real
    private var veiculosListener: ListenerRegistration? = null

    /**
     * Carrega todas as rotas do Firebase
     */
    fun carregarRotas() {
        firestore.collection("rotas")
            .get()
            .addOnSuccessListener { snapshot ->
                val listaRotas = snapshot.documents.mapNotNull { doc ->
                    try {
                        val codigo = doc.id
                        val nome = doc.getString("nome") ?: codigo
                        val cor = doc.getString("cor") ?: "#0066FF"

                        // Carrega pontos da rota
                        val pontosMap = doc.get("pontos") as? List<Map<String, Any>> ?: emptyList()
                        val pontos = pontosMap.mapNotNull { ponto ->
                            try {
                                PontoRota(
                                    id = ponto["id"] as? String ?: "",
                                    lat = (ponto["lat"] as? Number)?.toDouble() ?: 0.0,
                                    lng = (ponto["lng"] as? Number)?.toDouble() ?: 0.0
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        // Carrega paradas
                        val paradasMap = doc.get("paradas") as? List<Map<String, Any>> ?: emptyList()
                        val paradas = paradasMap.mapNotNull { parada ->
                            try {
                                Parada(
                                    id = parada["id"] as? String ?: "",
                                    nome = parada["nome"] as? String ?: "",
                                    lat = (parada["lat"] as? Number)?.toDouble() ?: 0.0,
                                    lng = (parada["lng"] as? Number)?.toDouble() ?: 0.0
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Rota(
                            codigo = codigo,
                            nome = nome,
                            cor = cor,
                            pontos = pontos,
                            paradas = paradas
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                rotas.value = listaRotas
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    /**
     * Seleciona uma rota e inicia o monitoramento de veículos
     */
    fun selecionarRota(rota: Rota) {
        rotaSelecionada.value = rota
        iniciarMonitoramentoVeiculos(rota.codigo)
    }

    /**
     * Monitora veículos em tempo real
     */
    private fun iniciarMonitoramentoVeiculos(codigoRota: String) {
        // Cancela listener anterior se existir
        veiculosListener?.remove()

        veiculosListener = firestore.collection("onibus")
            .document(codigoRota)
            .collection("veiculos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listaVeiculos = snapshot.documents.mapNotNull { doc ->
                        try {
                            Veiculo(
                                uid = doc.getString("uid") ?: "",
                                lat = doc.getDouble("lat") ?: 0.0,
                                lng = doc.getDouble("lng") ?: 0.0,
                                status = doc.getString("status") ?: "Desconhecido",
                                timestamp = doc.getTimestamp("timestamp")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    veiculos.value = listaVeiculos
                }
            }
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
        val rota = rotaSelecionada.value?.codigo ?: return

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

        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value?.codigo ?: return

        firestore.collection("onibus")
            .document(rota)
            .collection("veiculos")
            .document(user.uid)
            .delete()
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
        veiculosListener?.remove()
    }
}