package com.example.mobilidadeurbana.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.views.overlay.Polyline

// Data classes
data class Parada(
    val id: String = "",
    val nome: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class PontoRota(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Rota(
    val codigo: String = "",
    val nome: String = "",
    val cor: String = "#FF0000",
    val pontos: List<PontoRota> = emptyList(),
    val paradas: List<Parada> = emptyList()
)

data class Veiculo(
    val uid: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "",
    val rotaCodigo: String = ""
)

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Estados principais
    var statusOnibus = mutableStateOf("Em operação")
        private set

    var rotaSelecionada = mutableStateOf<Rota?>(null)
        private set

    var isTracking = mutableStateOf(false)
        private set

    var rotas = mutableStateOf<List<Rota>>(emptyList())
        private set

    var veiculos = mutableStateOf<List<Veiculo>>(emptyList())
        private set

    // Polylines para o mapa
    private val _polylines = MutableStateFlow<List<Polyline>>(emptyList())
    val polylines: StateFlow<List<Polyline>> = _polylines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var veiculosListener: ListenerRegistration? = null

    init {
        iniciarMonitoramentoVeiculos()
    }

    /**
     * Define o status do ônibus
     */
    fun setStatus(novoStatus: String) {
        statusOnibus.value = novoStatus
    }

    /**
     * Seleciona uma rota e carrega seus dados
     */
    fun selecionarRota(rota: Rota) {
        rotaSelecionada.value = rota
        Log.d("HOME_VM", "Rota selecionada: ${rota.nome} (${rota.codigo})")
    }

    /**
     * Carrega todas as rotas do Firestore e gera as Polylines
     */
    fun carregarRotas() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rotasCarregadas = pegarRotas()
                rotas.value = rotasCarregadas

                // Gera as Polylines para exibição no mapa
                val novasPolylines = gerarPolylines(rotasCarregadas)
                _polylines.value = novasPolylines

                Log.d("HOME_VM", "Rotas carregadas: ${rotasCarregadas.size}")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao carregar rotas", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Pega todas as rotas do Firestore
     * Estrutura: /rotas/{docId}
     *   - codigo: List<Map> com {lat, lng} (pontos da linha)
     *   - cor: String
     *   - nome: String
     *   - paradas: List<Map> com {id, nome, lat, lng}
     */
    private suspend fun pegarRotas(): List<Rota> {
        return try {
            val snapshot = firestore
                .collection("rotas")
                .orderBy("nome", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val codigo = doc.id
                    val nome = doc.getString("nome") ?: "Rota $codigo"
                    val cor = doc.getString("cor") ?: "#FF0000"

                    // Pega os pontos que formam a LINHA da rota (campo "codigo")
                    val pontosArray = doc.get("codigo") as? List<Map<String, Any>>
                    val pontos = pontosArray?.mapNotNull { ponto ->
                        val lng = (ponto["lng"] as? Number)?.toDouble()
                        val lat = (ponto["lat"] as? Number)?.toDouble()
                        if (lat != null && lng != null) {
                            PontoRota(lat, lng)
                        } else null
                    }?.takeIf { it.size >= 2 } ?: emptyList()

                    // Pega as PARADAS (campo "paradas")
                    val paradasArray = doc.get("paradas") as? List<Map<String, Any>>
                    val paradas = paradasArray?.mapNotNull { parada ->
                        try {
                            val id = (parada["id"] as? Number)?.toString() ?: ""
                            val nomeParada = parada["nome"] as? String ?: "Parada $id"
                            val lat = (parada["lat"] as? Number)?.toDouble() ?: 0.0
                            val lng = (parada["lng"] as? Number)?.toDouble() ?: 0.0

                            if (lat != 0.0 && lng != 0.0) {
                                Parada(id, nomeParada, lat, lng)
                            } else null
                        } catch (e: Exception) {
                            Log.e("HOME_VM", "Erro ao processar parada", e)
                            null
                        }
                    } ?: emptyList()

                    if (pontos.isEmpty()) {
                        Log.w("HOME_VM", "Rota $codigo sem pontos suficientes")
                        null
                    } else {
                        Rota(
                            codigo = codigo,
                            nome = nome,
                            cor = cor,
                            pontos = pontos,
                            paradas = paradas
                        )
                    }
                } catch (e: Exception) {
                    Log.e("HOME_VM", "Erro ao processar rota ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("HOME_VM", "Erro ao pegar rotas", e)
            emptyList()
        }
    }

    /**
     * Gera as Polylines para exibição no mapa
     * IMPORTANTE: Não cria marcadores para os pontos da linha
     */
    private fun gerarPolylines(rotas: List<Rota>): List<Polyline> {
        val novasLinhas = mutableListOf<Polyline>()

        for (rota in rotas) {
            if (rota.pontos.size < 2) continue

            val polyline = Polyline().apply {
                title = rota.nome

                // Converte cor HEX para int
                color = try {
                    android.graphics.Color.parseColor(rota.cor.uppercase())
                } catch (e: Exception) {
                    android.graphics.Color.parseColor("#FF0000")
                }

                width = 8f
                isGeodesic = true
            }

            // Adiciona os pontos à polyline
            var pontosValidos = 0
            for (ponto in rota.pontos) {
                polyline.addPoint(org.osmdroid.util.GeoPoint(ponto.lat, ponto.lng))
                pontosValidos++
            }

            if (pontosValidos >= 2) {
                novasLinhas.add(polyline)
                Log.d("HOME_VM", "Polyline criada para ${rota.nome}: $pontosValidos pontos")
            }
        }

        return novasLinhas
    }

    /**
     * Inicia o monitoramento de veículos em tempo real
     * Estrutura: /onibus/{uid}
     *   - lat, lng: localização
     *   - status: status do veículo
     *   - rotaCodigo: código da rota
     */
    private fun iniciarMonitoramentoVeiculos() {
        veiculosListener?.remove()

        veiculosListener = firestore
            .collection("onibus")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HOME_VM", "Erro ao monitorar veículos", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listaVeiculos = snapshot.documents.mapNotNull { doc ->
                        try {
                            val uid = doc.getString("uid") ?: doc.id

                            // Tenta pegar de diferentes formas
                            val geoPoint = doc.getGeoPoint("localizacao")
                            val lat = geoPoint?.latitude ?: doc.getDouble("lat") ?: 0.0
                            val lng = geoPoint?.longitude ?: doc.getDouble("lng") ?: 0.0

                            val status = doc.getString("status") ?: "Desconhecido"
                            val rotaCodigo = doc.getString("rotaCodigo") ?: ""

                            if (lat != 0.0 && lng != 0.0) {
                                Veiculo(uid, lat, lng, status, rotaCodigo)
                            } else null
                        } catch (e: Exception) {
                            Log.e("HOME_VM", "Erro ao processar veículo", e)
                            null
                        }
                    }

                    veiculos.value = listaVeiculos
                    Log.d("HOME_VM", "Veículos atualizados: ${listaVeiculos.size}")
                }
            }
    }

    /**
     * Inicia o rastreamento do veículo
     * Salva em: /onibus/{uid}
     */
    fun startTracking(lat: Double, lng: Double) {
        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value ?: return

        viewModelScope.launch {
            try {
                val dados = hashMapOf(
                    "uid" to user.uid,
                    "localizacao" to GeoPoint(lat, lng),
                    "lat" to lat,
                    "lng" to lng,
                    "status" to statusOnibus.value,
                    "rotaCodigo" to rota.codigo,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore
                    .collection("onibus")
                    .document(user.uid)
                    .set(dados)
                    .await()

                isTracking.value = true
                Log.d("HOME_VM", "Rastreamento iniciado na rota ${rota.codigo}")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao iniciar rastreamento", e)
            }
        }
    }

    /**
     * Para o rastreamento do veículo
     */
    fun stopTracking() {
        val user = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                firestore
                    .collection("onibus")
                    .document(user.uid)
                    .delete()
                    .await()

                isTracking.value = false
                Log.d("HOME_VM", "Rastreamento parado")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao parar rastreamento", e)
            }
        }
    }

    /**
     * Atualiza a localização no Firebase durante o rastreamento
     */
    fun updateLocationInFirebase(lat: Double, lng: Double) {
        if (!isTracking.value) return

        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value ?: return

        viewModelScope.launch {
            try {
                val dados = hashMapOf(
                    "uid" to user.uid,
                    "localizacao" to GeoPoint(lat, lng),
                    "lat" to lat,
                    "lng" to lng,
                    "status" to statusOnibus.value,
                    "rotaCodigo" to rota.codigo,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore
                    .collection("onibus")
                    .document(user.uid)
                    .set(dados)
                    .await()

            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao atualizar localização", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        veiculosListener?.remove()
        _polylines.value = emptyList()
    }
}