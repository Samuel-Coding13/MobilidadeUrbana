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

/**
 * Representa uma rota com suas características
 * MODIFICADO: Removido campo 'codigo' da cor, mantido apenas 'cor'
 */
data class Rota(
    val codigo: String = "",
    val nome: String = "",
    val cor: String = "#0066FF", // Apenas o valor da cor em formato hexadecimal
    val pontos: List<PontoRota> = emptyList(),
    val paradas: List<Parada> = emptyList()
)

/**
 * Representa um ponto da rota (coordenada)
 */
data class PontoRota(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * Representa uma parada de ônibus
 */
data class Parada(
    val id: String = "",
    val nome: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/**
 * Representa um veículo em rastreamento
 */
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

    // NOVO: Veículo atual sendo rastreado (apenas o do usuário logado)
    var veiculoAtual = mutableStateOf<Veiculo?>(null)
        private set

    // Status do ônibus
    var statusOnibus = mutableStateOf("Parado")
        private set

    // Estado do rastreamento
    var isTracking = mutableStateOf(false)
        private set

    // NOVO: Flag para controle automático de câmera
    var shouldFollowVehicle = mutableStateOf(false)
        private set

    // Job para controlar o loop de atualização de localização
    private var trackingJob: Job? = null

    // Listener para monitorar veículo em tempo real
    private var veiculoListener: ListenerRegistration? = null

    /**
     * Carrega todas as rotas disponíveis do Firebase
     * Estrutura esperada no Firestore:
     * rotas/
     *   ├── {codigo_rota}/
     *   │   ├── nome: String
     *   │   ├── cor: String (formato: "#RRGGBB")
     *   │   ├── pontos: Array<Map>
     *   │   │   ├── id: String
     *   │   │   ├── lat: Number
     *   │   │   └── lng: Number
     *   │   └── paradas: Array<Map>
     *   │       ├── id: String
     *   │       ├── nome: String
     *   │       ├── lat: Number
     *   │       └── lng: Number
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

                        // Carrega pontos da rota (coordenadas que formam a linha)
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

                        // Carrega paradas (pontos onde o ônibus para)
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
     * Seleciona uma rota específica e inicia o monitoramento do veículo
     * MODIFICADO: Agora monitora apenas o veículo do usuário atual
     */
    fun selecionarRota(rota: Rota) {
        rotaSelecionada.value = rota
        iniciarMonitoramentoVeiculo(rota.codigo)
    }

    /**
     * NOVO: Monitora apenas o veículo do usuário atual em tempo real
     * Estrutura esperada no Firestore:
     * onibus/
     *   └── {codigo_rota}/
     *       └── veiculos/
     *           └── {uid_usuario}/
     *               ├── uid: String
     *               ├── lat: Number
     *               ├── lng: Number
     *               ├── status: String
     *               └── timestamp: Timestamp
     */
    private fun iniciarMonitoramentoVeiculo(codigoRota: String) {
        val user = auth.currentUser ?: return

        // Cancela listener anterior se existir
        veiculoListener?.remove()

        // Monitora apenas o documento do usuário atual (não todos os veículos)
        veiculoListener = firestore.collection("onibus")
            .document(codigoRota)
            .collection("veiculos")
            .document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val veiculo = Veiculo(
                            uid = snapshot.getString("uid") ?: "",
                            lat = snapshot.getDouble("lat") ?: 0.0,
                            lng = snapshot.getDouble("lng") ?: 0.0,
                            status = snapshot.getString("status") ?: "Desconhecido",
                            timestamp = snapshot.getTimestamp("timestamp")
                        )
                        veiculoAtual.value = veiculo
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Veículo não existe mais no Firebase (rastreamento parado)
                    veiculoAtual.value = null
                }
            }
    }

    /**
     * Define o status atual do ônibus
     * Opções: "Em operação", "Parado", "Manutenção", "Fora de serviço"
     */
    fun setStatus(status: String) {
        statusOnibus.value = status
    }

    /**
     * Inicia o rastreamento do veículo
     * MODIFICADO: Ativa automaticamente o follow da câmera
     *
     * @param latitude Latitude inicial do veículo
     * @param longitude Longitude inicial do veículo
     */
    fun startTracking(latitude: Double, longitude: Double) {
        if (isTracking.value) return

        isTracking.value = true
        shouldFollowVehicle.value = true // Ativa o acompanhamento automático da câmera

        // Inicia loop de atualização a cada 5 segundos
        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isTracking.value) {
                updateLocationInFirebase(latitude, longitude)
                delay(5000) // Atualiza a cada 5 segundos
            }
        }
    }

    /**
     * Atualiza a localização do veículo no Firebase em tempo real
     * Salva na coleção: onibus/{codigo_rota}/veiculos/{uid_usuario}
     *
     * @param latitude Latitude atual do veículo
     * @param longitude Longitude atual do veículo
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
     * Para o rastreamento do veículo
     * MODIFICADO: Desativa o follow da câmera e remove o veículo do Firebase
     */
    fun stopTracking() {
        isTracking.value = false
        shouldFollowVehicle.value = false // Desativa o acompanhamento da câmera
        trackingJob?.cancel()
        trackingJob = null

        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value?.codigo ?: return

        // Remove o veículo do Firebase ao parar o rastreamento
        firestore.collection("onibus")
            .document(rota)
            .collection("veiculos")
            .document(user.uid)
            .delete()
    }

    /**
     * NOVO: Desativa manualmente o acompanhamento automático da câmera
     * Útil quando o usuário deseja mover o mapa manualmente
     */
    fun disableFollowVehicle() {
        shouldFollowVehicle.value = false
    }

    /**
     * NOVO: Reativa o acompanhamento automático da câmera
     */
    fun enableFollowVehicle() {
        if (isTracking.value) {
            shouldFollowVehicle.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
        veiculoListener?.remove()
    }
}