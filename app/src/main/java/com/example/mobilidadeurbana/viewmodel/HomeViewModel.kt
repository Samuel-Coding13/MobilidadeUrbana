package com.example.mobilidadeurbana.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Job para controlar o rastreamento em background
    private var trackingJob: Job? = null

    // Estados principais
    var statusOnibus = mutableStateOf("Em operação")
        private set

    var rotaSelecionada = mutableStateOf<Rota?>(null)
        private set

    var isTracking = mutableStateOf(false)
        private set

    var rotas = mutableStateOf<List<Rota>>(emptyList())
        private set

    // Última localização conhecida
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastSpeed = 0f

    // Polylines para o mapa
    private val _polylines = MutableStateFlow<List<Polyline>>(emptyList())
    val polylines: StateFlow<List<Polyline>> = _polylines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Define o status do ônibus
     */
    fun setStatus(novoStatus: String) {
        statusOnibus.value = novoStatus
        // Se estiver rastreando, atualiza imediatamente no Firestore
        if (isTracking.value) {
            updateLocationInFirebase(lastLat, lastLng, lastSpeed)
        }
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

                    // Pega os pontos que formam a LINHA da rota
                    val pontosArray = doc.get("codigo") as? List<Map<String, Any>>
                    val pontos = pontosArray?.mapNotNull { ponto ->
                        val lng = (ponto["lng"] as? Number)?.toDouble()
                        val lat = (ponto["lat"] as? Number)?.toDouble()
                        if (lat != null && lng != null) {
                            PontoRota(lat, lng)
                        } else null
                    }?.takeIf { it.size >= 2 } ?: emptyList()

                    // Pega as PARADAS
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
     */
    private fun gerarPolylines(rotas: List<Rota>): List<Polyline> {
        val novasLinhas = mutableListOf<Polyline>()

        for (rota in rotas) {
            if (rota.pontos.size < 2) continue

            val polyline = Polyline().apply {
                title = rota.nome

                color = try {
                    android.graphics.Color.parseColor(rota.cor.uppercase())
                } catch (e: Exception) {
                    android.graphics.Color.parseColor("#FF0000")
                }

                width = 8f
                isGeodesic = true
            }

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
     * Inicia o rastreamento do veículo com atualização contínua em background
     */
    fun startTracking(lat: Double, lng: Double, velocidade: Float) {
        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value ?: return

        // Armazena a localização inicial
        lastLat = lat
        lastLng = lng
        lastSpeed = velocidade

        // Cancela job anterior se existir
        trackingJob?.cancel()

        // Inicia o rastreamento
        isTracking.value = true

        // Envia a localização inicial imediatamente
        viewModelScope.launch {
            try {
                val dados = hashMapOf(
                    "uid" to user.uid,
                    "localizacao" to GeoPoint(lat, lng),
                    "lat" to lat,
                    "lng" to lng,
                    "status" to statusOnibus.value,
                    "rotaCodigo" to rota.codigo,
                    "velocidade" to velocidade,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore
                    .collection("onibus")
                    .document(user.uid)
                    .set(dados)
                    .await()

                Log.d("HOME_VM", "Rastreamento iniciado na rota ${rota.codigo}")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao iniciar rastreamento", e)
            }
        }

        // Inicia o loop contínuo de atualização em background
        trackingJob = viewModelScope.launch {
            while (isActive && isTracking.value) {
                try {
                    // Aguarda 2 segundos entre atualizações
                    delay(2000)

                    // Atualiza no Firestore
                    if (isTracking.value) {
                        updateLocationInFirebase(lastLat, lastLng, lastSpeed)
                    }
                } catch (e: Exception) {
                    Log.e("HOME_VM", "Erro no loop de rastreamento", e)
                    // Continua o loop mesmo com erro
                }
            }
        }
    }

    /**
     * Para o rastreamento do veículo
     */
    fun stopTracking() {
        val user = auth.currentUser ?: return

        // Cancela o job de rastreamento
        trackingJob?.cancel()
        trackingJob = null

        isTracking.value = false

        viewModelScope.launch {
            try {
                firestore
                    .collection("onibus")
                    .document(user.uid)
                    .delete()
                    .await()

                Log.d("HOME_VM", "Rastreamento parado")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao parar rastreamento", e)
            }
        }
    }

    /**
     * Atualiza a localização no Firebase durante o rastreamento
     */
    fun updateLocationInFirebase(lat: Double, lng: Double, velocidade: Float) {
        if (!isTracking.value) return

        val user = auth.currentUser ?: return
        val rota = rotaSelecionada.value ?: return

        // Armazena a última localização
        lastLat = lat
        lastLng = lng
        lastSpeed = velocidade

        viewModelScope.launch {
            try {
                val dados = hashMapOf(
                    "uid" to user.uid,
                    "localizacao" to GeoPoint(lat, lng),
                    "lat" to lat,
                    "lng" to lng,
                    "status" to statusOnibus.value,
                    "rotaCodigo" to rota.codigo,
                    "velocidade" to velocidade,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore
                    .collection("onibus")
                    .document(user.uid)
                    .set(dados)
                    .await()

                Log.d("HOME_VM", "Localização atualizada: $lat, $lng")
            } catch (e: Exception) {
                Log.e("HOME_VM", "Erro ao atualizar localização", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancela o rastreamento quando o ViewModel é destruído
        trackingJob?.cancel()
        _polylines.value = emptyList()
    }
}